package org.waterwood.waterfunservicecore.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.waterwood.common.jpa.CodeUniquenessChecker;
import org.waterwood.waterfunservicecore.entity.Permission;

import java.util.List;
import java.util.Optional;

public interface PermissionRepo extends JpaRepository<Permission, Integer>, JpaSpecificationExecutor<Permission>, CodeUniquenessChecker {
    List<Permission> findByName(String name);
    Optional<Permission> findByCode(String code);
}
