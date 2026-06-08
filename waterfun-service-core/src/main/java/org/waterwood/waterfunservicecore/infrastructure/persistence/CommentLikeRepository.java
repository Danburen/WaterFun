package org.waterwood.waterfunservicecore.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.waterwood.waterfunservicecore.entity.post.CommentLike;
import org.waterwood.waterfunservicecore.entity.post.CommentLikeId;

public interface CommentLikeRepository extends JpaRepository<CommentLike, CommentLikeId> {
}