package org.waterwood.waterfunservicecore.infrastructure.persistence;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.waterwood.waterfunservicecore.entity.content.Banner;
import org.waterwood.waterfunservicecore.entity.content.VisibleStatus;

import java.time.Instant;
import java.util.List;

public interface BannerRepository extends JpaRepository<Banner, Long>, JpaSpecificationExecutor<Banner> {
    @EntityGraph(attributePaths = "resource")
    @Query("""
    SELECT b FROM Banner b
    WHERE b.status = :status
      AND b.isDeleted = :isDeleted
      AND (
          b.startAt IS NULL AND b.endAt IS NULL
          OR b.startAt IS NOT NULL AND b.endAt IS NULL AND b.startAt <= :now
          OR b.startAt IS NULL AND b.endAt IS NOT NULL AND b.endAt >= :now
          OR b.startAt IS NOT NULL AND b.endAt IS NOT NULL AND b.startAt <= :now AND b.endAt >= :now
      )
    """)
    List<Banner> findCurrentlyActive(
            @Param("now") Instant now,
            @Param("status") VisibleStatus status,
            @Param("isDeleted") Boolean isDeleted
    );
}