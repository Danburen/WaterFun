package org.waterwood.waterfunservicecore.services.user;

import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.waterwood.waterfunservicecore.entity.user.Role;
import org.waterwood.waterfunservicecore.entity.user.UserRole;
import org.waterwood.waterfunservicecore.infrastructure.persistence.RoleRepo;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserRoleRepo;

import java.util.Set;

@Service
@AllArgsConstructor
public class UserRoleCoreServiceImpl implements UserRoleCoreService{
    private final UserRoleRepo userRoleRepo;
    private final RoleRepo roleRepo;

    @PostConstruct
    private void init (){
        roleRepo.findByName(getAdminRoleCode()).orElseGet(()->{
            Role r = new Role();
            r.setName(getAdminRoleCode());
            r.setCode(getAdminRoleCode());
            r.setOrderWeight(0);
            r.setDescription("""
                    System basic admin role
                    """);
            r.setIsSystem(true);
            return roleRepo.save(r);
        });
    }

    @Override
    public Set<UserRole> getUserRoles(long uid) {
        return userRoleRepo.findByUserUid(uid);
    }

    @Override
    public String getAdminRoleCode() {
        return "ADMIN";
    }
}
