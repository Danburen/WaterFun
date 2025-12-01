package org.waterwood.waterfunadminservice.service.user.impl;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.waterwood.api.BaseResponseCode;
import org.waterwood.waterfunservicecore.entity.Permission;
import org.waterwood.waterfunservicecore.entity.user.User;
import org.waterwood.waterfunservicecore.entity.user.UserPermission;
import org.waterwood.common.exceptions.BusinessException;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserPermRepo;
import org.waterwood.waterfunadminservice.service.perm.PermissionServiceImpl;
import org.waterwood.waterfunadminservice.service.user.UserService;

import java.util.List;

@Service
public class UserPermissionService {
    @Autowired
    private UserPermRepo userPermRepo;
    @Autowired
    private PermissionServiceImpl permService;
    @Autowired
    private UserService userService;

    public List<Permission> getUserPermissions(long userId){
        return userPermRepo.findByUserId(userId).stream().map(
                UserPermission::getPermission
        ).toList();
    }

    public List<User> getPermissionUsers(int permId){
        return userPermRepo.findByPermissionId(permId).stream().map(
                UserPermission::getUser
        ).toList();
    }

    @Transactional
    public void addUserPermission(long userId,int permId){
        User user = userService.getUserById(userId);
        Permission perm = permService.getPermission(permId);
        UserPermission userPermission = new UserPermission();
        userPermission.setUser(user);
        userPermission.setPermission(perm);
        userPermRepo.save(userPermission);
    }

    @Transactional
    public void removeUserPermission(long userId, int permId){
        User user = userService.getUserById(userId);
        if(!userPermRepo.existsByUserIdAndPermissionId(userId,permId)){
            throw  new BusinessException(BaseResponseCode.PERMISSION_NOT_FOUND,
                    "User ID "+userId +"doesn't have permission ID" + permId);
        }
        userPermRepo.deleteByUserIdAndPermissionId(userId,permId);
    }
}
