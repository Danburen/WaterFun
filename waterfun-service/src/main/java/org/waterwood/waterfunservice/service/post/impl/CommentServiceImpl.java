package org.waterwood.waterfunservice.service.post.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.waterwood.utils.StringUtil;
import org.waterwood.waterfunservice.api.request.CreateCommentReq;
import org.waterwood.waterfunservice.api.response.CommentResponse;
import org.waterwood.waterfunservice.infrastructure.dto.RootCommentCursor;
import org.waterwood.waterfunservice.service.NotificationService;
import org.waterwood.waterfunservice.service.post.CommentService;
import org.waterwood.waterfunservicecore.api.CursorPage;
import org.waterwood.waterfunservicecore.api.resp.user.UserBrief;
import org.waterwood.waterfunservicecore.entity.post.*;
import org.waterwood.waterfunservicecore.exception.CommentAlreadyDeletedOrNotFoundException;
import org.waterwood.waterfunservicecore.exception.notfound.CommentNotFoundException;
import org.waterwood.waterfunservicecore.infrastructure.persistence.CommentLikeRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.CommentRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.PostRepository;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserRepository;
import org.waterwood.waterfunservicecore.infrastructure.utils.context.UserCtxHolder;
import org.waterwood.waterfunservicecore.services.sys.storage.CloudFileService;
import org.waterwood.waterfunservicecore.services.user.UserBriefService;

import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class CommentServiceImpl implements CommentService {
    private final CommentRepository commentRepository;
    private final UserBriefService userBriefService;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final NotificationService notificationService;

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 20;
    private final CloudFileService cloudFileService;


    @Transactional
    @Override
    public void create(CreateCommentReq req) {
        Comment parent = null;
        Comment comment = new Comment();
        comment.setPost(postRepository.getReferenceById(req.getPostId()));

        if(req.getParentId() != null){
            parent = commentRepository.findByPostIdAndParentIdAndStatus(
                    req.getPostId(), req.getParentId(), CommentStatus.NORMAL
            ).orElseThrow(CommentAlreadyDeletedOrNotFoundException::new);

            commentRepository.increaseReplyCountById(req.getParentId());
            if(parent.getRoot() != null){
                comment.setRoot(commentRepository.getReferenceById(parent.getRoot().getId()));
            }
        } else {
            postRepository.increaseCommentCountById(req.getPostId(), 1);
        }

        comment.setParent(parent);
        comment.setAuthor(userRepository.getReferenceById(UserCtxHolder.getUserUid()));
        comment.setContent(req.getContent());
        commentRepository.save(comment);
    }

    @Transactional
    @Override
    public void delete(Long id) {
        Comment comment = commentRepository.findByIdAndStatus(id, CommentStatus.NORMAL)
                .orElseThrow(CommentNotFoundException::new);
        if(comment.isDeleted()) throw new CommentAlreadyDeletedOrNotFoundException();

        int deletedReplies = 0;
        if (comment.getReplyCount() > 0) {
            deletedReplies = commentRepository.updateStatusByParentId(CommentStatus.DELETED, id);
        }

        comment.setStatus(CommentStatus.DELETED);
        commentRepository.save(comment);
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
                },
                () -> {
                    CommentLikeId commentLikeId = new CommentLikeId(id, userUid);
                    CommentLike commentLike = new CommentLike();
                    commentLike.setId(commentLikeId);
                    commentLikeRepository.save(commentLike);
                    commentRepository.increaseLikeCountById(id, 1);
                    if (commentDO != null) {
                        notificationService.onCommentLike(
                                commentDO.getAuthorUid(),
                                userUid,
                                id,
                                commentDO.getContent(),
                                commentDO.getPostId()
                        );
                    }
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
                c.getIsTop(),
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
                new RootCommentCursor(comment.getIsTop(), comment.getLikeCount(), comment.getId()).encode()
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
                null // first level comment won't have its parent replier
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
        list.forEach(c -> {
            allUserIds.add(c.getAuthor().getUid());
            if (c.getParentId() != null && !c.getParentId().equals(rootId)) {
                commentRepository.findById(c.getParentId())
                        .ifPresent(p -> allUserIds.add(p.getAuthor().getUid()));
            }
        });

        Map<Long, UserBrief> userBriefMap = userBriefService.listUseBriefs(new ArrayList<>(allUserIds)).stream().collect(
                Collectors.toMap(UserBrief::getUid, ub -> ub)
        );

        Map<Long, String> parentAuthorNameMap = new HashMap<>();
        for (Comment c : list) {
            Long parentId = c.getParentId();
            if (parentId != null && !parentId.equals(rootId)) {
                parentAuthorNameMap.put(parentId,
                        commentRepository.findById(parentId)
                                .map(p -> userBriefMap.get(p.getAuthor().getUid()))
                                .map(UserBrief::getDisplayName)
                                .orElse(null)
                );
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
                    isDirectReplyToRoot ? null : parentAuthorNameMap.get(parentId)
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
                            null // single comment won't show replacer display name
                    );
                })
                .orElseThrow(CommentNotFoundException::new);
    }
}
