package org.waterwood.waterfunservicecore.infrastructure.persistence;

import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.waterwood.waterfunservicecore.entity.post.PostAuthorUidTitleDO;
import org.waterwood.waterfunservicecore.entity.post.PostStatus;
import org.waterwood.waterfunservicecore.entity.post.PostVisibility;
import org.waterwood.waterfunservicecore.entity.post.Post;
import org.waterwood.common.jpa.SlugUniquenessChecker;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long>,
        JpaSpecificationExecutor<Post>,
        SlugUniquenessChecker {
    boolean existsBySlug(String slug);
    @EntityGraph(attributePaths = { "author"})
    @Query("SELECT p FROM Post p WHERE p.id IN :ids ORDER BY p.createdAt DESC")
    List<Post> findAllByIdInAndOrderBYCreatedAtDesc(@Param("ids") Collection<Long> ids);

    int deleteByIdIn(List<Long> attr0);

    Optional<Post> findByIdAndVisibilityAndIsDeleted(Long id, PostVisibility visibility, Boolean isDeleted);
    default Page<Long> findAllIds(Specification<Post> spec, Pageable pageable) {
        Specification<Post> merged = Specification.where(spec)
                .and((root, query, cb) -> cb.equal(root.get("isDeleted"), false));
        return findAll(merged, pageable).map(Post::getId);
    }

    @Query("SELECT p FROM Post p WHERE p.id = :id AND p.author.uid = :authorUid AND p.isDeleted = :isDeleted")
    Optional<Post> findByIdAndAuthorUidAndIsDeleted(@Param("id") Long id, @Param("authorUid") Long authorUid,@Param("isDeleted")  boolean isDeleted);

    Optional<Post> findByIdAndAuthorUidAndIsDeletedAndStatus(Long id, Long authorUid, Boolean isDeleted, PostStatus status);

    @EntityGraph(attributePaths = { "author", "category", "tags"})
    Optional<Post> findByIdAndIsDeleted(@NotNull Long id, Boolean isDeleted);

    @EntityGraph(attributePaths = { "author", "category", "tags"})
    Optional<Post> findByIdAndIsDeletedAndStatus(@NotNull Long id, Boolean isDeleted, PostStatus status);

    List<Post> findAllByIdInAndIsDeletedAndStatus(List<Long> ids, Boolean isDeleted, PostStatus status);
    @Modifying
    @Query("UPDATE Post p SET p.commentCount = p.commentCount + :count WHERE p.id = :postId")
    void increaseCommentCountById(@Param("postId") Long id, @Param("count") int count);

    @Modifying
    @Query("UPDATE Post p SET p.commentCount = GREATEST(p.commentCount - :count, 0) WHERE p.id = :postId")
    void decreaseCommentCountById(@Param("postId") Long id, @Param("count") int count);

    @Modifying
    @Query("UPDATE Post p SET p.likeCount = p.likeCount + :count WHERE p.id = :postId")
    void increaseLikeCount(@Param("postId") Long postId, @Param("count") int count);

    @Modifying
    @Query("UPDATE Post p SET p.likeCount = GREATEST(p.likeCount - :count, 0) WHERE p.id = :postId")
    void decreaseLikeCount(@Param("postId") Long postId, @Param("count") int count);

    @Modifying
    @Query("UPDATE Post p SET p.collectCount = p.collectCount + :count WHERE p.id = :postId")
    void increaseCollectCount(@Param("postId") Long postId, @Param("count") int count);

    @Modifying
    @Query("UPDATE Post p SET p.collectCount = GREATEST(p.collectCount - :count, 0) WHERE p.id = :postId")
    void decreaseCollectCount(@Param("postId") Long postId, @Param("count") int count);
    @Modifying
    @Query("UPDATE Post p SET p.viewCount = p.viewCount + :count WHERE p.id = :postId")
    void increaseViewCount(@Param("postId") Long postId,@Param("count") int count);

    Long findAuthorUidById(Long id);

    @Query("SELECT new org.waterwood.waterfunservicecore.entity.post.PostAuthorUidTitleDO(p.author.uid, p.title, p.coverageResource.uuid) " +
            "FROM Post p WHERE p.id = :id")
    Optional<PostAuthorUidTitleDO> findPostAuthorIdTitleDOById(@Param("id") Long id);

    @Query("SELECT COALESCE(SUM(p.likeCount), 0) FROM Post p WHERE p.author.uid = :authorUid AND p.isDeleted = false")
    Long sumLikeCountByAuthorUid(@Param("authorUid") Long authorUid);

    long countByAuthorUidAndStatusAndIsDeleted(Long authorUid, PostStatus status, Boolean isDeleted);

    long countByAuthorUidAndIsDeleted(Long authorUid, Boolean isDeleted);

    List<Post> findAllByIdInAndAuthorUidAndIsDeleted(List<Long> ids, Long authorUid, Boolean isDeleted);

    @Query(value = """
        SELECT p.id FROM post p
        WHERE p.is_deleted = false
        AND p.status = 2
        AND p.visibility = 0
        AND (
            MATCH(p.title, p.summary, p.content) AGAINST(:keyword IN BOOLEAN MODE)
            OR EXISTS (
                SELECT 1 FROM post_tag pt
                INNER JOIN tag t ON pt.tag_id = t.id
                WHERE pt.post_id = p.id AND t.name = :exactKeyword
            )
        )
        ORDER BY MATCH(p.title, p.summary, p.content) AGAINST(:keyword IN BOOLEAN MODE) DESC
        """,
        countQuery = """
        SELECT COUNT(*) FROM (
            SELECT p.id FROM post p
            WHERE p.is_deleted = false
            AND p.status = 2
            AND p.visibility = 0
            AND (
                MATCH(p.title, p.summary, p.content) AGAINST(:keyword IN BOOLEAN MODE)
                OR EXISTS (
                    SELECT 1 FROM post_tag pt
                    INNER JOIN tag t ON pt.tag_id = t.id
                    WHERE pt.post_id = p.id AND t.name = :exactKeyword
                )
            )
        ) AS cnt
        """,
        nativeQuery = true)
    Page<Long> searchByFulltext(@Param("keyword") String keyword, @Param("exactKeyword") String exactKeyword, Pageable pageable);

    @Query(value = """
        SELECT p.id FROM post p
        WHERE p.is_deleted = false
        AND p.status = 2
        AND p.visibility = 0
        AND p.type = 0
        ORDER BY (p.view_count * 1 + p.comment_count * 3 + p.like_count * 2) DESC
        """,
        countQuery = """
        SELECT COUNT(p.id) FROM post p
        WHERE p.is_deleted = false
        AND p.status = 2
        AND p.visibility = 0
        AND p.type = 0
        """,
        nativeQuery = true)
    Page<Long> findHotPostIds(Pageable pageable);

    @Query("SELECT p.coverageResource.uuid FROM Post p WHERE p.id = :id")
    String findCoverageResourceUuidById(@Param("id")Long id);
}
