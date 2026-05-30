package org.waterwood.waterfunservicecore.infrastructure.persistence;

import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.waterwood.waterfunservicecore.entity.post.PostStatus;
import org.waterwood.waterfunservicecore.entity.post.PostVisibility;
import org.waterwood.waterfunservicecore.entity.post.Post;
import org.waterwood.common.jpa.SlugUniquenessChecker;

import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long>,
        JpaSpecificationExecutor<Post>,
        SlugUniquenessChecker {
    boolean existsBySlug(String slug);

    int deleteByIdIn(List<Long> attr0);

    Optional<Post> findByIdAndVisibilityAndIsDeleted(Long id, PostVisibility visibility, Boolean isDeleted);

    Page<Long> findAllIds(Specification<Post> spec, Pageable pageable);

    Optional<Post> findByIdAndAuthorUidAndIsDeleted(Long id, Long id1, boolean attr0);

    Optional<Post> findByIdAndAuthorUidAndIsDeletedAndStatus(Long id, Long authorUid, Boolean isDeleted, PostStatus status);

    Optional<Post> findByIdAndIsDeleted(@NotNull Long id, Boolean isDeleted);
}
