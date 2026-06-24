package org.waterwood.waterfunservicecore.infrastructure.persistence;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.waterwood.waterfunservicecore.entity.resource.Resource;
import org.waterwood.waterfunservicecore.entity.resource.ResourceStatus;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ResourceRepository extends JpaRepository<Resource, Long> {
    Optional<Resource> findByUuidAndStatus(String uuid, ResourceStatus status);

    List<Resource> findByUuidIn(Collection<String> uuids);

    @Modifying
    @Query("UPDATE Resource r SET r.status = :to WHERE r.uuid IN :uuids AND r.status = :from")
    void batchUpdateStatusFromTo(@Param("from") ResourceStatus From,
                                 @Param("to") ResourceStatus status,
                                 @Param("uuids") Collection<String> uuids);
    @Modifying
    @Query("UPDATE Resource r SET r.status = :status WHERE r.uuid IN :uuids")
    void batchUpdateStatusFromTo(@Param("status") ResourceStatus status, @Param("uuids") Collection<String> uuids);

    @Modifying
    @Query("UPDATE Resource r SET r.status = :status WHERE r.uuid = :uuid")
    void updateStatusTo(@Param("status") ResourceStatus status,@Param("uuid") String uuid);

    List<Resource> findByUuidInAndUploaderIdAndStatus(Collection<String> uuids, Long uploaderId, ResourceStatus status);

    List<Resource> findByUuidInAndStatus(Collection<String> uuids, ResourceStatus status);

    Resource getReferenceByUuid(String uuid);

    Optional<Resource> getByUuid(String uuid);

    Optional<Resource> findByUuidAndStatusNot(String uuid, ResourceStatus status);

    @Query("SELECT r.resourceKey FROM Resource r WHERE r.uuid in :uuids")
    List<String> findResourceResourceKeyByUuidIn(Collection<String> uuids);

    List<Resource> findByUuidIn(List<String> attr0);

    List<Resource> findByUploaderIdAndUuidInAndStatusNot(Long uploaderId, Set<String> attr0, ResourceStatus status);

    Instant findResourceUpdatedAtByUuid(@Size(max = 36) @NotNull String uuid);
}