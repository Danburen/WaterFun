package org.waterwood.waterfunservicecore.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.waterwood.waterfunservicecore.entity.post.Comment;
import org.waterwood.waterfunservicecore.entity.post.CommentDO;
import org.waterwood.waterfunservicecore.entity.post.CommentStatus;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    @Query("""
        SELECT c FROM Comment c
        WHERE c.post.id = :postId
        AND c.parent IS NULL
        AND c.status = :status
        ORDER BY c.likeCount DESC, c.createdAt DESC
    """)
    Page<Comment> findByPostIdAndParentIsNullAndStatus(@Param("postId") Long postId,@Param("status") CommentStatus status, Pageable pageable);
    @Query("""
        SELECT c FROM Comment c
        WHERE c.post.id = :postId
        AND c.parent.id = :parentId
        AND c.status = :status
        ORDER BY c.likeCount DESC, c.createdAt DESC
      """)
    Page<Comment> findByPostIdAndParentIdAndStatus(@Param("postId") Long postId,@Param("parentId") Long parentId, @Param("status") CommentStatus status,  Pageable pageable);
    @Query("""
        SELECT c FROM Comment c
        WHERE c.post.id = :postId
        AND c.parent.id = :parentId
        AND c.status = :status
        ORDER BY c.likeCount DESC, c.createdAt DESC
      """)
    Optional<Comment> findByPostIdAndParentIdAndStatus(@Param("postId") Long postId,@Param("parentId") Long parentId, @Param("status") CommentStatus status);

    @Query("""
        SELECT c FROM Comment c
        WHERE c.post.id = :postId
        AND c.id = :commentId
        AND c.status = :status
        ORDER BY c.likeCount DESC, c.createdAt DESC
      """)
    Optional<Comment> findByPostIdAndIdAndStatus(@Param("postId") Long postId,@Param("commentId") Long commentId, @Param("status") CommentStatus status);

    @EntityGraph(attributePaths = {"post.author"})
    Optional<Comment> findByIdAndStatus(Long id, CommentStatus status);

    @Modifying
    @Query("UPDATE Comment c SET c.replyCount = c.replyCount + 1 WHERE c.id = :id")
    void increaseReplyCountById(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Comment c SET c.replyCount = GREATEST(c.replyCount - 1, 0) WHERE c.id = :id")
    void decreaseReplyCountById(@Param("id") Long id);

    @Query("UPDATE Comment c SET c.status = :status WHERE c.parent.id = :parentId")
    @Modifying
    int updateStatusByParentId(@Param("status") CommentStatus status,@Param("parentId") Long parentId);

    @Modifying
    @Query("UPDATE Comment c SET c.likeCount = GREATEST (c.likeCount - :count, 0) WHERE c.id = :id")
    void decreaseLikeCountById(@Param("id") Long id, @Param("count") int count);

    @Modifying
    @Query("UPDATE Comment c SET c.likeCount = c.likeCount + :count WHERE c.id = :id")
    void increaseLikeCountById(@Param("id") Long id, @Param("count") int count);

    @Query("""
        SELECT new org.waterwood.waterfunservicecore.entity.post.CommentDO(c.author.uid, c.content, c.post.id) 
            FROM Comment c where c.id = :id AND c.status = :status
    """)
    Optional<CommentDO> findAuthorUidByIdAndStatus(@Param("id") Long id, @Param("status") CommentStatus status);


    @EntityGraph(attributePaths = {"post.author"})
    @Query("""
        SELECT c FROM Comment c
        WHERE c.post.id = :postId
          AND c.root.id IS NULL
          AND c.status = 1
          AND (:cursor = 0L OR
               (c.isPined = :isPinedCursor AND c.likeCount < :likeCursor) OR
               (c.isPined = :isPinedCursor AND c.likeCount = :likeCursor AND c.id < :idCursor))
        ORDER BY c.isPined DESC, c.likeCount DESC, c.id DESC
        """)
    List<Comment> findRootComments(
            @Param("postId") Long postId,
            @Param("cursor") Long cursor,
            @Param("isPinedCursor") Boolean isPinedCursor,
            @Param("likeCursor") Long likeCursor,
            @Param("idCursor") Long idCursor,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"post.author"})
    @Query("""
        SELECT c FROM Comment c
        WHERE c.root.id = :rootId
          AND c.status = 1
          AND (:cursor = 0 OR c.id > :cursor)
        ORDER BY c.createdAt ASC
        """)
    List<Comment> findReplies(
            @Param("rootId") Long rootId,
            @Param("cursor") Long cursor,
            Pageable pageable
    );

}