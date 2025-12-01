package org.waterwood.waterfunadminservice.service.perm;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.waterwood.waterfunservicecore.entity.Permission;

public interface PermissionService {
    /**
     * Get permission by permission ID
     * @param permId permission ID
     * @return  permission
     */
    Permission getPermission(int permId);

    /**
     * List all the permissions
     *
     * @param spec     specification
     * @param pageable pageable
     * @return permissions
     */
    Page<Permission> listPermissions(Specification<Permission> spec, Pageable pageable);

    /**
     * Add permission
     * @param perm  permission
     */
    void addPermission(Permission  perm);

    /**
     * Update permission
     * @param perm  permission
     */
    void update(Permission perm);

    /**
     * Delete permission
     * @param id permission ID
     */
    void deleteUser(int id);
}
