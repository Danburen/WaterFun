package org.waterwood.waterfunadminservice.service.perm;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.waterwood.api.BaseResponseCode;
import org.waterwood.waterfunservicecore.entity.Permission;
import org.waterwood.common.exceptions.BusinessException;
import org.waterwood.waterfunservicecore.infrastructure.persistence.PermissionRepo;

@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {
    private final PermissionRepo permissionRepo;
    @Transactional(readOnly = true)
    @Override
    public Permission getPermission(int PermId){
        return permissionRepo.findById(PermId)
                .orElseThrow(() -> new BusinessException(BaseResponseCode.PERMISSION_NOT_FOUND));
    }

    @Override
    public Page<Permission> listPermissions(Specification<Permission> spec, Pageable pageable) {
        // TODO: check permission
        return permissionRepo.findAll(spec, pageable);
    }

    @Override
    public void addPermission(Permission perm) {
        // TODO: check permission
        permissionRepo.findByCode(perm.getCode()).ifPresent(_ -> {
            throw new BusinessException(BaseResponseCode.PERMISSION_ALREADY_EXISTS);
        });
        permissionRepo.save(perm);
    }

    @Override
    public void update(Permission perm) {
        // TODO: check permission
        if (perm.getParent() != null && perm.getParent().getId() != null) {
            perm.setParent(permissionRepo.getReferenceById(perm.getParent().getId()));
        }
        permissionRepo.save(perm);
    }

    @Override
    public void deleteUser(int id) {
        // TODO: check permission
        permissionRepo.deleteById(id);
    }
}
