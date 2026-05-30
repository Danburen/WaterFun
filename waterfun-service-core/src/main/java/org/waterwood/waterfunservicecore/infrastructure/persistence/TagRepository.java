package org.waterwood.waterfunservicecore.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.waterwood.waterfunservicecore.entity.post.Tag;
import org.waterwood.common.jpa.SlugUniquenessChecker;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface TagRepository extends JpaRepository<Tag, Long>, JpaSpecificationExecutor<Tag>, SlugUniquenessChecker {
  boolean existsTagBySlug(String slug);

  List<Tag> findAllByCreatorUid(Long currentUserUid);

  List<Long> findTagIdsByTagsIdIn(Collection<Long> tagsIds);

  int removeByIdIn(Collection<Long> ids);

  @Query("SELECT t FROM Tag t ORDER BY t.usageCount DESC LIMIT :limit")
  List<Tag> findTopByOrderByUsageCountDesc(@Param("limit") Integer limit);


  @Query("""
    SELECT p.id, new org.waterwood.api.VO.OptionVO(t.id, t.slug, t.name, false)
    FROM Post p JOIN p.tags t
    WHERE p.id IN :postIds
""")
  List<Object[]> findTagsByPostIds(List<Long> postIds);

  List<Tag> findAllByNameIn(Set<String> newTagNames);

  int countByCreatorUid(Long userUid);
}