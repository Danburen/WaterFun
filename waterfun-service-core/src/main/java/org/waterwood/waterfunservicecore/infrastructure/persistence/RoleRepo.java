package org.waterwood.waterfunservicecore.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.waterwood.common.jpa.CodeUniquenessChecker;
import org.waterwood.waterfunservicecore.entity.Role;

import java.util.Optional;

public interface RoleRepo extends JpaRepository<Role, Integer>, JpaSpecificationExecutor<Role>, CodeUniquenessChecker {
    Optional<Role> findByName(String name);
}
