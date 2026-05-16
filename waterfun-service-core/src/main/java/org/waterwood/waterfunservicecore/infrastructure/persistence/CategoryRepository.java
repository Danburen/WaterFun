package org.waterwood.waterfunservicecore.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.waterwood.waterfunservicecore.entity.post.Category;
import org.waterwood.common.jpa.SlugUniquenessChecker;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Integer>, JpaSpecificationExecutor<Category>,SlugUniquenessChecker {
    boolean existsTagBySlug(String slug);
    List<Category> findAllByCreatorUid(Long creatorId);

    Optional<Category> findByName(String name);

    void removeCategoryById(Integer id);

    int deleteByIdIn(Collection<Integer> ids);

    @Query("""
        SELECT p.id, new org.waterwood.api.VO.OptionVO(c.id, c.slug, c.name, false)
        FROM Post p JOIN p.category c
        WHERE p.id IN :postIds
    """)
    List<Object[]> findCategoryByPostIds(List<Long> postIds);
}
