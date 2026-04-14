package org.waterwood.waterfunservicecore.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.waterwood.waterfunservicecore.entity.post.Tag;
import org.waterwood.common.jpa.SlugUniquenessChecker;

import java.util.Collection;
import java.util.List;

public interface TagRepository extends JpaRepository<Tag, Integer>, JpaSpecificationExecutor<Tag>, SlugUniquenessChecker {
  boolean existsTagBySlug(String slug);

  List<Tag> findAllByCreatorUid(Long currentUserUid);

  List<Integer> findTagIdsByTagsIdIn(Collection<Integer> tagsIds);

    int removeByIdIn(Collection<Integer> ids);
}