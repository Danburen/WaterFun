package org.waterwood.waterfunservicecore.infrastructure.persistence;

import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.waterwood.waterfunservicecore.entity.post.PostTag;

import java.util.Collection;
import java.util.List;

public interface PostTagRepository extends JpaRepository<PostTag, Long> {

  @Query("SELECT pt.post.id FROM PostTag pt WHERE pt.post.id = :postId " )
  List<Long> findTagIdsByPostId(@Param("postId") Long postId);

  int deleteByPostId(@NotNull Long postId);

  int deleteByPostIdAndTagIdIn(@NotNull Long postId, Collection<Long> tagId);
}