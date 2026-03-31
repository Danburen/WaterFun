package org.waterwood.waterfunservicecore.infrastructure.persistence.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.waterwood.waterfunservicecore.entity.user.UserRole;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface UserRoleRepo extends JpaRepository<UserRole, Long>, JpaSpecificationExecutor<UserRole> {
    Set<UserRole> findByUserUid(Long userUid);
    Optional<UserRole> findByUserUidAndRoleId(Long userUid, Integer roleId);
    List<UserRole> findByRoleIdAndUserUidIn(Integer roleId, List<Long> userUids);
    List<UserRole> findByUserUidAndRoleIdIn(Long userUid, Set<Integer> roleIds);
    boolean existsByUserUidAndRoleId(Long userUid, Integer roleId);
    int deleteByUserUidAndRoleIdIn(Long userUid, Set<Integer> roleIds);

    void deleteByUserUid(Long userUid);

    Page<UserRole> findByRoleId(int id, Pageable pageable);

    @Modifying
    @Query("DELETE FROM UserRole ur WHERE ur.role.id = :id AND ur.user.uid IN :userUids")
    int deleteByRoleIdAndUserUids(@Param("id") int id,@Param("userUids") List<Long> userUids);

    int deleteByRoleId(Integer roleId);
}
