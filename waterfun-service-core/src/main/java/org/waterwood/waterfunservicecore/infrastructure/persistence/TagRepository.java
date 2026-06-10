package org.waterwood.waterfunservicecore.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.waterwood.api.VO.OptionVO;
import org.waterwood.waterfunservicecore.entity.post.IdOptionVOPackagedDO;
import org.waterwood.waterfunservicecore.entity.post.Tag;
import org.waterwood.common.jpa.SlugUniquenessChecker;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface TagRepository extends JpaRepository<Tag, Long>, JpaSpecificationExecutor<Tag>, SlugUniquenessChecker {
  boolean existsTagBySlug(String slug);

  List<Tag> findAllByCreatorUid(Long currentUserUid);

  List<Long> findTagIdsByTagsIdIn(Collection<Long> tagsIds);

  int removeByIdIn(Collection<Long> ids);

  @Query("SELECT t FROM Tag t ORDER BY t.usageCount DESC LIMIT :limit")
  List<Tag> findTopByOrderByUsageCountDesc(@Param("limit") Integer limit);


  @Query("""
    SELECT new org.waterwood.api.VO.OptionVO(t.id, t.slug, t.name, false)
    FROM Post p JOIN p.tags t
    WHERE p.id IN :postIds
""")
  List<OptionVO<Long>> findTagOptionVOsByPostIdIn(List<Long> postIds);

  @Query("""
        SELECT new org.waterwood.waterfunservicecore.entity.post.IdOptionVOPackagedDO
            (t.id, t.slug, t.name, false)
        FROM Post p JOIN p.tags t
        WHERE p.id IN :postIds
    """)
  List<IdOptionVOPackagedDO<Long>> findTagDOByPostIdIn(@Param("postIds") List<Long> postIds);

  List<Tag> findAllByNameIn(Set<String> newTagNames);

  int countByCreatorUid(Long userUid);

  Optional<Tag> findByName(String name);

  Page<Tag> findAllByIsDeleted(boolean attr0, Pageable attr1);

  @Query("""
    SELECT t FROM Tag t
    WHERE t.isDeleted = false
    AND (t.name LIKE :keyword% OR t.slug LIKE :keyword%)
    """)
  List<Tag> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

  List<Tag> findAllByIdInAndIsDeleted(Collection<Long> ids, Boolean isDeleted);

  @Modifying
  @Query("""
   UPDATE Tag t
   SET t.usageCount = t.usageCount + :count
   WHERE t.id IN :ids
   """)
    void increaseUsageCountInIds(@Param("ids") List<Long> tagIds, @Param("count") int count);
  @Modifying
  @Query("""
    UPDATE Tag t
    SET t.usageCount = GREATEST (t.usageCount - :count, 0)
    WHERE t.id IN :ids
""")
  void decreaseUsageCountInIds(@Param("ids") List<Long> removedTagIds, @Param("count") int count);
}