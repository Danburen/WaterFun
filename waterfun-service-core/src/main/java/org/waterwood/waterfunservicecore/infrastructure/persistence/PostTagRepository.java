package org.waterwood.waterfunservicecore.infrastructure.persistence;

import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.waterwood.waterfunservicecore.entity.post.PostTag;
import org.waterwood.waterfunservicecore.entity.post.Tag;

import java.util.Collection;
import java.util.List;

public interface PostTagRepository extends JpaRepository<PostTag, Long> {
  List<PostTag> findAllByPostId(@NotNull Long postId);

  List<Integer> findTagIdsByPostId(@NotNull Long postId);

  int deleteByPostId(@NotNull Long postId);

  int deleteByPostIdAndTagIdIn(@NotNull Long postId, Collection<Integer> tagIds);
}