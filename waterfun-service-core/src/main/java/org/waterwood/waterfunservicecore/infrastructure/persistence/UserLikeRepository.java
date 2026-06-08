package org.waterwood.waterfunservicecore.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.waterwood.waterfunservicecore.entity.user.UserLike;
import org.waterwood.waterfunservicecore.entity.user.UserLikeId;

public interface UserLikeRepository extends JpaRepository<UserLike, UserLikeId> {
}