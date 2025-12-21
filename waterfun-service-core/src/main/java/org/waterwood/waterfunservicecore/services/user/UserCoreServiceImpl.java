package org.waterwood.waterfunservicecore.services.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.waterwood.api.BaseResponseCode;
import org.waterwood.waterfunservicecore.entity.Permission;
import org.waterwood.waterfunservicecore.entity.Role;
import org.waterwood.waterfunservicecore.entity.RolePermission;
import org.waterwood.waterfunservicecore.entity.user.User;
import org.waterwood.waterfunservicecore.entity.user.UserPermission;
import org.waterwood.waterfunservicecore.entity.user.UserRole;
import org.waterwood.waterfunservicecore.infrastructure.persistence.RolePermRepo;
import org.waterwood.waterfunservicecore.infrastructure.persistence.RoleRepo;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserPermRepo;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserRepository;
import org.waterwood.common.exceptions.BusinessException;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserRoleRepo;
import org.waterwood.waterfunservicecore.infrastructure.security.AuthContextHelper;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserCoreServiceImpl implements UserCoreService {
    private final UserRepository userRepository;
    private final UserRoleRepo userRoleRepo;
    private final UserPermRepo userPermRepo;
    private final RoleRepo roleRepo;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final RolePermRepo rolePermRepo;

    @Override
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username).orElseThrow(
                ()-> new BusinessException(BaseResponseCode.USER_NOT_FOUND)
        );
    }

    @Override
    public User getUserByUid(long uid) {
        return  userRepository.findById(uid).orElseThrow(
                ()-> new BusinessException(BaseResponseCode.USER_NOT_FOUND)
        );
    }

    @Override
    public User addUser(User user) {
        return userRepository.save(user);
    }

    @Override
    public User update(User user){
        return addUser(user);
    }

    @Override
    public Set<Permission> getUserPermissions(long userUid) {
        if(userUid != AuthContextHelper.getCurrentUserUid()){
           throw  new BusinessException(BaseResponseCode.HTTP_UNAUTHORIZED);
        }
        List<Role> roles = userRoleRepo.findByUserUid(userUid).stream().map(UserRole::getRole).toList();

        Set<Permission> rolePermission = roles.stream()
                .flatMap(role-> getRolePermissions(role.getId()).stream())
                .collect(Collectors.toSet());

        Set<Permission> userPermission = userPermRepo.findByUserUid(userUid).stream()
                .map(UserPermission::getPermission).collect(Collectors.toSet());
        HashSet<Permission> permissions = new HashSet<>();
        permissions.addAll(rolePermission);
        permissions.addAll(userPermission);
        return permissions;
    }

    @Override
    public Set<Role> getRoles(long userUid) {
        return userRoleRepo.findByUserUid(userUid).stream().map(UserRole::getRole).collect(Collectors.toSet());
    }

    @Override
    public User changePwd(long userUid, String newPwdHashed) {
        User u = getUser(userUid);
        if(u.getPasswordHash() == null) return u;
        if(u.getPasswordHash().equals(newPwdHashed)){
            throw new BusinessException(BaseResponseCode.PASSWORD_TWO_PASSWORD_MUST_DIFFERENT);
        }
        u.setPasswordHash(newPwdHashed);
        return userRepository.save(u);
    }

    @Override
    public User getUser(long userUid){
        return userRepository.findUserById(userUid)
                .orElseThrow(() -> new BusinessException(BaseResponseCode.USER_NOT_FOUND));
    }

    private List<Permission> getRolePermissions(int roleId){
        return roleRepo.findById(roleId).map(role ->
                        rolePermRepo.findByRole(role).stream()
                                .map(RolePermission::getPermission)
                                .toList())
                .orElse(List.of());
    }
}
