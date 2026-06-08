package org.waterwood.waterfunservicecore.infrastructure.persistence;

import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.waterwood.waterfunservicecore.entity.post.PostTag;

import java.util.Collection;
import java.util.List;

public interface PostTagRepository extends JpaRepository<PostTag, Long> {

  List<Long> findTagIdsByPostId(@NotNull Long postId);

  int deleteByPostId(@NotNull Long postId);

  int deleteByPostIdAndTagIdIn(@NotNull Long post_id, Collection<Long> tag_id);
}