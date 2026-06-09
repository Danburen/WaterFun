package org.waterwood.waterfunservicecore.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.waterwood.waterfunservicecore.entity.IpBan;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface IpBanRepository extends JpaRepository<IpBan, Long>, JpaSpecificationExecutor<IpBan> {
  @Query("SELECT b FROM IpBan b WHERE b.expiresAt IS NULL OR b.expiresAt > :now")
  List<IpBan> findActiveList(Instant now);

  @Query("""
        SELECT COUNT(b) > 0 FROM IpBan b
        WHERE b.ip = :ip AND (b.expiresAt IS NULL OR b.expiresAt > :now)
        """)
  Optional<IpBan> findActiveByIp(@Param("ip") String ip, @Param("now") Instant now);

  @Modifying
  @Query("UPDATE IpBan b SET b.expiresAt = NOW()")
  int unbanByIp(@Param("ip") String ip);
}