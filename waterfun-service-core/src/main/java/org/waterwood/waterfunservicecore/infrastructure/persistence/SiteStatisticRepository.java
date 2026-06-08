package org.waterwood.waterfunservicecore.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.waterwood.waterfunservicecore.entity.SiteStatistic;

import java.time.LocalDate;
import java.util.Optional;

public interface SiteStatisticRepository extends JpaRepository<SiteStatistic, LocalDate>, JpaSpecificationExecutor<SiteStatistic> {
    Optional<SiteStatistic> findTopByOrderByUpdatedAtDesc();
}