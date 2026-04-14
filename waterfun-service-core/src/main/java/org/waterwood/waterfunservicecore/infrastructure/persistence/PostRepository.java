package org.waterwood.waterfunservicecore.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.waterwood.waterfunservicecore.entity.post.Post;
import org.waterwood.common.jpa.SlugUniquenessChecker;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long>,
        JpaSpecificationExecutor<Post>,
        SlugUniquenessChecker {
    boolean existsBySlug(String slug);

    int deleteByIdIn(List<Long> attr0);
}
