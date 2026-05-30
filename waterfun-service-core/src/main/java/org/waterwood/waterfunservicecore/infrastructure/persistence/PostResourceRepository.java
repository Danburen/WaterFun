package org.waterwood.waterfunservicecore.infrastructure.persistence;

import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.waterwood.waterfunservicecore.entity.PostResource;
import org.waterwood.waterfunservicecore.entity.PostResourceId;

import java.util.Collection;
import java.util.List;

public interface PostResourceRepository extends JpaRepository<PostResource, PostResourceId>, JpaSpecificationExecutor<PostResource> {
    List<PostResource> findAllByPostIdAndResourceUuidUuidIn(Long postId, Collection<String> resourceUuidUuids);
}