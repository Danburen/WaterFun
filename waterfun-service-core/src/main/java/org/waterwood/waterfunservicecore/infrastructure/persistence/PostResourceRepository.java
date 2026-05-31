package org.waterwood.waterfunservicecore.infrastructure.persistence;

import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.waterwood.waterfunservicecore.entity.post.PostResource;
import org.waterwood.waterfunservicecore.entity.post.PostResourceId;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PostResourceRepository extends JpaRepository<PostResource, PostResourceId>, JpaSpecificationExecutor<PostResource> {
    Optional<PostResource> findByPostIdAndResourceUuidUuid(@NotNull Long postId, String resourceUuidUuid);
    List<PostResource> findAllByPostIdAndResourceUuidUuidIn(@NotNull Long postId, Collection<String> resourceUuidUuids);
}