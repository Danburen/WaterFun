package org.waterwood.waterfunservicecore.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.waterwood.waterfunservicecore.entity.post.Category;
import org.waterwood.common.jpa.SlugUniquenessChecker;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Integer>, SlugUniquenessChecker {
    boolean existsTagBySlug(String slug);
    List<Category> findAllByCreatorId(Long creatorId);

    Optional<Category> findByName(String name);

    void removeCategoryById(Integer id);
}
