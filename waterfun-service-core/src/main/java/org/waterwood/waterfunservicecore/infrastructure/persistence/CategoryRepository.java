package org.waterwood.waterfunservicecore.infrastructure.persistence;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.waterwood.api.VO.OptionVO;
import org.waterwood.waterfunservicecore.entity.post.Category;
import org.waterwood.common.jpa.SlugUniquenessChecker;
import org.waterwood.waterfunservicecore.entity.post.IdOptionVOPackagedDO;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long>, JpaSpecificationExecutor<Category>,SlugUniquenessChecker {
    boolean existsTagBySlug(String slug);
    List<Category> findAllByCreatorUid(Long creatorId);

    Optional<Category> findByName(String name);

    void removeCategoryById(Long id);

    int deleteByIdIn(Collection<Long> ids);

    @Query("""
        SELECT new org.waterwood.api.VO.OptionVO(c.id, c.slug, c.name, false)
        FROM Post p JOIN p.category c
        WHERE p.id IN :postIds
    """)
    List<OptionVO<Long>> findCategoryOptionVOByPostIdIn(@Param("postIds") List<Long> postIds);

    @Query("""
        SELECT new org.waterwood.waterfunservicecore.entity.post.IdOptionVOPackagedDO
            (p.id ,c.id, c.slug, c.name, false)
        FROM Post p JOIN p.category c
        WHERE p.id IN :postIds
    """)
    List<IdOptionVOPackagedDO<Long, Long>> findCategoryDOByPostIdIn(@Param("postIds") List<Long> postIds);

    List<Category> findAllByIsDeleted(Boolean isDeleted);

    @EntityGraph("withParent")
    @Query("""
        SELECT c FROM Category c
        WHERE c.isDeleted = false
        ORDER BY c.usageCount DESC
       """)
    List<Category> findAllByIsDeletedWithParentOrderByUsageCountDesc();

    @Modifying
    @Query("""
       UPDATE Category c
        SET c.usageCount = c.usageCount + :count
        WHERE c.id = :id
    """)
    void increaseUsageCountById(@Param("id") Long id, @Param("count") int count);

    @Modifying
    @Query("""
         UPDATE Category c
          SET c.usageCount = GREATEST ( c.usageCount - :count, 0)
          WHERE c.id = :id
""")
    void decreaseUsageCountById(@Param("id") Long id, @Param("count") int count);
}
