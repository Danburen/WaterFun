package org.waterwood.waterfunservicecore.infrastructure.persistence;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.waterwood.waterfunservicecore.entity.resource.Resource;
import org.waterwood.waterfunservicecore.entity.resource.ResourceStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ResourceRepository extends JpaRepository<Resource, Long> {
    Optional<Resource> findByUuidAndStatus(@Size(max = 36) @NotNull String uuid, ResourceStatus status);

    List<Resource> findByUuidIn(Collection<String> uuids);

    @Modifying
    @Query("UPDATE Resource r SET r.status = :status WHERE r.uuid IN :uuids")
    void batchUpdateStatus(@Param("status") ResourceStatus status,
                           @Param("uuids") Collection<String> uuids);

    List<Resource> findByUuidInAndUploaderId(Collection<String> uuids, Long uploaderId);

    List<Resource> findByUuidInAndStatus(Collection<String> uuids, ResourceStatus status);

    Resource getReferenceByUuid(@Size(max = 36) @NotNull String uuid);

    Optional<Resource> getByUuid(String uuid);
}