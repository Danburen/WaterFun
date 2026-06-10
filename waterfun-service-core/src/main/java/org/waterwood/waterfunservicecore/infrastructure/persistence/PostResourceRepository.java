package org.waterwood.waterfunservicecore.infrastructure.persistence;

import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.waterwood.waterfunservicecore.entity.post.PostResource;
import org.waterwood.waterfunservicecore.entity.post.PostResourceId;
import org.waterwood.waterfunservicecore.entity.resource.Resource;
import org.waterwood.waterfunservicecore.entity.resource.ResourceStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PostResourceRepository extends JpaRepository<PostResource, PostResourceId>, JpaSpecificationExecutor<PostResource> {
    Optional<PostResource> findByPostIdAndResourceUuidUuid(@NotNull Long postId, String resourceUuidUuid);
    List<PostResource> findAllByPostIdAndResourceUuidUuidIn(@NotNull Long postId, Collection<String> resourceUuidUuids);

    List<PostResource> findByPostIdAndResourceUuidStatusNot(@NotNull Long postId, ResourceStatus resourceUuidStatus);

    @Query("SELECT pr.resourceUuid.uuid FROM PostResource pr WHERE pr.post.id = :postId")
    List<String> findUuidsByPostId(@Param("postId") Long postId);

    List<PostResource> findByPostId(@NotNull Long postId);
}