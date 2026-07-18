package org.waterwood.waterfunservicecore.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.waterwood.waterfunservicecore.entity.security.IpAccessLog;

public interface IpAccessLogRepository extends JpaRepository<IpAccessLog, Long>, JpaSpecificationExecutor<IpAccessLog> {
}
