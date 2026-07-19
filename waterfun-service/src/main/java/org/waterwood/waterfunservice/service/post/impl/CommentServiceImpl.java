package org.waterwood.waterfunservice.service.post.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.waterwood.utils.StringUtil;
import org.waterwood.waterfunservice.api.request.CreateCommentReq;
import org.waterwood.waterfunservice.api.response.CommentResponse;
import org.waterwood.waterfunservice.infrastructure.dto.RootCommentCursor;
import org.waterwood.waterfunservice.service.NotificationService;
import org.waterwood.waterfunservice.service.post.CommentService;
import org.waterwood.waterfunservicecore.api.CursorPage;
import org.waterwood.waterfunservicecore.api.resp.user.UserBrief;
import org.waterwood.waterfunservicecore.entity.post.*;
import org.waterwood.waterfunservicecore.entity.user.ContentPermission;
import org.waterwood.waterfunservicecore.exception.CommentAlreadyDeletedOrNotFoundException;
import org.waterwood.waterfunservicecore.exception.notfound.CommentNotFoundException;
import org.waterwood.waterfunservicecore.exception.reference.PostReferenceInvalidException;
import org.waterwood.waterfunservicecore.infrastructure.persistence.CommentLikeRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.CommentRepository;
import org.waterwood.api.BaseResponseCode;
import org.waterwood.waterfunservicecore.exception.BizException;
import org.waterwood.waterfunservicecore.exception.privacy.CommentNotAllowedException;
import org.waterwood.waterfunservicecore.infrastructure.persistence.PostRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserFollowerRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserSettingRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserCounterRepository;
import org.waterwood.waterfunservicecore.entity.user.UserFollowerId;
import org.waterwood.waterfunservicecore.entity.audit.UserActionType;
import org.waterwood.waterfunservicecore.entity.notification.BusinessType;
import org.waterwood.waterfunservicecore.infrastructure.utils.IdGenerator;
import org.waterwood.waterfunservicecore.infrastructure.utils.context.UserCtxHolder;
import org.waterwood.waterfunservicecore.exception.InappropriateContentException;
import org.waterwood.waterfunservicecore.services.audit.UserActivityLogService;
import org.waterwood.waterfunservicecore.services.content.TextFilterService;
import org.waterwood.waterfunservicecore.services.sys.storage.CloudFileService;
import org.waterwood.waterfunservicecore.services.user.UserBriefService;

import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {
    private final CommentRepository commentRepository;
    private final UserBriefService userBriefService;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final NotificationService notificationService;
    private final UserActivityLogService userActivityLogService;
    private final UserSettingRepository userSettingRepository;
    private final UserFollowerRepository userFollowerRepository;
    private final UserCounterRepository userCounterRepository;
    private final TextFilterService textFilterService;

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 20;
    private final CloudFileService cloudFileService;


    @Transactional
    @Override
    public void create(CreateCommentReq req) {
        Comment parent = null;
        Comment comment = new Comment();
        comment.setId(IdGenerator.nextCommentId());
        Long postId = req.getPostId();
        Post p = postRepository.findById(postId).orElseThrow(
                () -> new PostReferenceInvalidException(postId)
        );
        // Allow interaction if: publicly visible AND has been published at least once
        if (p.getVisibility() != PostVisibility.PUBLIC || p.getPublishedAt() == null) {
            throw new CommentNotFoundException();
        }
        // Check post author's comment permission
        Long postAuthorUid = p.getAuthor().getUid();
        Long currentUid = UserCtxHolder.getUserUid();
        if (!postAuthorUid.equals(currentUid)) {
            userSettingRepository.findById(postAuthorUid).ifPresent(setting -> {
                if (setting.getCommentPermission() == ContentPermission.NONE) {
                    throw new CommentNotAllowedException();
                }
                if (setting.getCommentPermission() == ContentPermission.FOLLOWERS
                        && !userFollowerRepository.existsById(new UserFollowerId(postAuthorUid, currentUid))) {
                    throw new CommentNotAllowedException();
                }
            });
        }
        if (textFilterService.containsSensitiveWords(req.getContent())) {
            throw new InappropriateContentException();
        }

        comment.setPost(postRepository.getReferenceById(postId));

        if(req.getParentId() != null){
            parent = commentRepository.findByPostIdAndIdAndStatus(
                    postId, req.getParentId(), CommentStatus.NORMAL
            ).orElseThrow(CommentAlreadyDeletedOrNotFoundException::new);
            if(parent.getRoot() != null){
                comment.setRoot(commentRepository.getReferenceById(parent.getRoot().getId()));
            } else {
                comment.setRoot(parent);
            }
            comment.setParent(parent);
            commentRepository.increaseReplyCountById(req.getParentId());
        }
        postRepository.increaseCommentCountById(postId, 1);

        comment.setAuthor(userRepository.getReferenceById(currentUid));
        comment.setContent(req.getContent());
        commentRepository.save(comment);
        userActivityLogService.record(currentUid, UserActionType.CREATE, BusinessType.COMMENT, comment.getId());

        // Send reply notification
        if (parent != null) {
            if (!parent.getAuthor().getUid().equals(currentUid)) {
                notificationService.onReply(
                        parent.getAuthor().getUid(),
                        currentUid,
                        comment.getId(),
                        parent.getContent(),
                        postId,
                        req.getContent()
                );
            }
        } else {
            if (!postAuthorUid.equals(currentUid)) {
                notificationService.onPostReply(
                        postAuthorUid,
                        currentUid,
                        comment.getId(),
                        comment.getPost().getTitle(),
                        postId,
                        req.getContent()
                );
            }
        }
    }

    @Transactional
    @Override
    public void delete(Long id) {
        Comment comment = commentRepository.findByIdAndStatus(id, CommentStatus.NORMAL)
                .orElseThrow(CommentNotFoundException::new);

        Long currentUid = UserCtxHolder.getUserUid();
        if (!comment.getAuthor().getUid().equals(currentUid)) {
            throw new BizException(BaseResponseCode.FORBIDDEN, "You are not the author of this comment");
        }

        int deletedReplies = 0;
        if (comment.getReplyCount() > 0) {
            deletedReplies = commentRepository.updateStatusByParentId(CommentStatus.DELETED, id);
        }

        comment.setStatus(CommentStatus.DELETED);
        commentRepository.save(comment);
        userActivityLogService.record(currentUid, UserActionType.DELETED, BusinessType.COMMENT, id);
        if (comment.isTopLevel()) {
            postRepository.decreaseCommentCountById(comment.getPostId(), deletedReplies + 1);
        } else {
            commentRepository.decreaseReplyCountById(comment.getParentId());
        }
    }

    @Transactional
    @Override
    public void like(Long id) {
        Long userUid = UserCtxHolder.getUserUid();
        CommentDO commentDO = commentRepository
                .findAuthorUidByIdAndStatus(id, CommentStatus.NORMAL)
                .orElse(null);
        commentLikeRepository.findById(new CommentLikeId(id, userUid)).ifPresentOrElse(
                cl -> {
                    commentLikeRepository.delete(cl);
                    commentRepository.decreaseLikeCountById(id, 1);
                    userCounterRepository.decreaseUserLikeCount(userUid, 1);
                    userActivityLogService.record(userUid, UserActionType.DELETED, BusinessType.COMMENT, id);
                },
                () -> {
                    CommentLikeId commentLikeId = new CommentLikeId(id, userUid);
                    CommentLike commentLike = new CommentLike();
                    commentLike.setId(commentLikeId);
                    commentLikeRepository.save(commentLike);
                    commentRepository.increaseLikeCountById(id, 1);
                    userCounterRepository.increaseUserLikeCount(userUid, 1);
                    if (commentDO != null) {
                        notificationService.onCommentLike(
                                commentDO.getAuthorUid(),
                                userUid,
                                id,
                                commentDO.getContent(),
                                commentDO.getPostId()
                        );
                    }
                    userActivityLogService.record(userUid, UserActionType.CREATE, BusinessType.COMMENT, id);
                }
        );
    }

    @Override
    public CursorPage<CommentResponse, String> listRootComments(Long postId, String cursor, Integer limit, Long includeRootId) {
        limit = Math.min(limit == null ? DEFAULT_LIMIT : limit, MAX_LIMIT);
        RootCommentCursor c = StringUtil.isBlank(cursor) ? RootCommentCursor.first() : RootCommentCursor.decode(cursor);
        List<Comment> list = commentRepository.findRootComments(
                postId,
                c.getId(),
                c.getIsPined(),
                c.getLikeCount(),
                c.getId(),
                PageRequest.of(0, limit + 1)
        );

        if (includeRootId != null) {
            boolean exists = list.stream().anyMatch(cmt -> cmt.getId().equals(includeRootId));
            if (!exists) {
                Comment included = commentRepository.findByIdAndStatus(includeRootId, CommentStatus.NORMAL)
                        .orElseThrow(CommentNotFoundException::new);
                list.addFirst(included);
            }
        }

        CursorPage<Comment, String> page = CursorPage.of(list, limit, comment ->
                new RootCommentCursor(comment.getIsPined(), comment.getLikeCount(), comment.getId()).encode()
        );


        List<Long> userIds = page.getList().stream()
                .map(cmt -> cmt.getAuthor().getUid())
                .distinct()
                .toList();
        Map<Long, UserBrief> userBriefMap = userBriefService.listUseBriefs(userIds).stream().collect(
                Collectors.toMap(UserBrief::getUid, ub -> ub)
        );
        return page.map(comment -> new CommentResponse(
                comment.getId(),
                comment.getPost().getId(),
                null,
                null,
                userBriefMap.get(comment.getAuthor().getUid()),
                comment.getContent(),
                comment.getLikeCount(),
                comment.getReplyCount(),
                comment.getCreatedAt(),
                null,
                comment.getPost().getAuthor() != null && comment.getPost().getAuthor().getUid().equals(comment.getAuthor().getUid() ),
                commentLikeRepository.existsById(new CommentLikeId(comment.getId(), UserCtxHolder.getUserUid()))
        ));
    }

    @Override
    public CursorPage<CommentResponse, Long> listReplies(Long rootId, Long cursor, Integer limit, Long includeRootId) {
        limit = Math.min(limit == null ? DEFAULT_LIMIT : limit, MAX_LIMIT);
        Long actualCursor = (cursor == null || cursor == 0) ? 0L : cursor;


        List<Comment> list = commentRepository.findReplies(
                rootId,
                actualCursor,
                PageRequest.of(0, limit + 1)
        );

        if (includeRootId != null) {
            boolean exists = list.stream().anyMatch(c -> c.getId().equals(includeRootId));
            if (!exists) {
                Comment highlighted = commentRepository.findByIdAndStatus(includeRootId, CommentStatus.NORMAL)
                        .orElseThrow(CommentNotFoundException::new);
                list.add(highlighted);
            }
        }

        Set<Long> allUserIds = new HashSet<>();
        Set<Long> parentIds = new HashSet<>();
        for (Comment c : list) {
            allUserIds.add(c.getAuthor().getUid());
            if (c.getParentId() != null && !c.getParentId().equals(rootId)) {
                parentIds.add(c.getParentId());
            }
        }
        Map<Long, Comment> parentCommentMap = parentIds.isEmpty()
                ? Collections.emptyMap()
                : commentRepository.findAllById(parentIds).stream()
                        .collect(Collectors.toMap(Comment::getId, p -> p));
        parentCommentMap.values().forEach(p -> allUserIds.add(p.getAuthor().getUid()));

        Map<Long, UserBrief> userBriefMap = userBriefService.listUseBriefs(new ArrayList<>(allUserIds)).stream().collect(
                Collectors.toMap(UserBrief::getUid, ub -> ub)
        );

        Map<Long, String> parentAuthorNameMap = new HashMap<>();
        for (Comment c : list) {
            Long parentId = c.getParentId();
            if (parentId != null && !parentId.equals(rootId)) {
                Comment parent = parentCommentMap.get(parentId);
                if (parent != null) {
                    UserBrief parentAuthor = userBriefMap.get(parent.getAuthor().getUid());
                    if (parentAuthor != null) {
                        parentAuthorNameMap.put(parentId, parentAuthor.getDisplayName());
                    }
                }
            }
        }

        CursorPage<Comment, Long> page = CursorPage.of(list, limit, Comment::getId);

        return page.map(comment -> {
            Long parentId = comment.getParentId();
            boolean isDirectReplyToRoot = parentId == null || parentId.equals(rootId);
            return new CommentResponse(
                    comment.getId(),
                    comment.getPost().getId(),
                    comment.getParent() == null ? null : comment.getParent().getId(),
                    comment.getRoot() == null ? null : comment.getRoot().getId(),
                    userBriefMap.get(comment.getAuthor().getUid()),
                    comment.getContent(),
                    comment.getLikeCount(),
                    comment.getReplyCount(),
                    comment.getCreatedAt(),
                    isDirectReplyToRoot ? null : parentAuthorNameMap.get(parentId),
                    comment.getPost().getAuthor() != null && comment.getPost().getAuthor().getUid().equals(comment.getAuthor().getUid()),
                    commentLikeRepository.existsById(new CommentLikeId(comment.getId(), UserCtxHolder.getUserUid()))
            );
        });
    }

    @Override
    public CommentResponse getComment(Long commentId) {
        return commentRepository.findByIdAndStatus(commentId, CommentStatus.NORMAL)
                .map(comment -> {
                    UserBrief authorBrief = userBriefService.getUserBrief(comment.getAuthor().getUid());
                    return new CommentResponse(
                            comment.getId(),
                            comment.getPost().getId(),
                            comment.getParent() == null ? null : comment.getParent().getId(),
                            comment.getRoot() == null ? null : comment.getRoot().getId(),
                            authorBrief,
                            comment.getContent(),
                            comment.getLikeCount(),
                            comment.getReplyCount(),
                            comment.getCreatedAt(),
                            null,
                            comment.getPost().getAuthor() != null && comment.getPost().getAuthor().getUid().equals(comment.getAuthor().getUid()),
                            commentLikeRepository.existsById(new CommentLikeId(comment.getId(), UserCtxHolder.getUserUid()))
                    );
                })
                .orElseThrow(CommentNotFoundException::new);
    }
}
