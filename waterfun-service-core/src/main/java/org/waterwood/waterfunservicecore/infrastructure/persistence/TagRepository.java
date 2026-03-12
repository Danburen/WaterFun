package org.waterwood.waterfunservicecore.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.waterwood.waterfunservicecore.entity.post.Tag;
import org.waterwood.common.jpa.SlugUniquenessChecker;

import java.util.List;

public interface TagRepository extends JpaRepository<Tag, Integer>, SlugUniquenessChecker {
  boolean existsTagBySlug(String slug);

  List<Tag> findAllByCreatorUid(Long currentUserUid);
}