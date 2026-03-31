package org.waterwood.waterfunadminservice.service.perm;

import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.waterwood.api.TO.BatchResult;
import org.waterwood.waterfunadminservice.api.request.perm.CreatePermRequest;
import org.waterwood.waterfunadminservice.api.request.perm.UpdatePermRequest;
import org.waterwood.waterfunservicecore.entity.Permission;
import org.waterwood.waterfunservicecore.entity.user.User;

import java.time.Instant;
import java.util.List;

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
     *
     * @param req permission
     * @return
     */
    Permission addPermission(CreatePermRequest req);

    /**
     * Update permission
     *
     * @param id the perm id
     * @param req permission
     * @return saved permission entity
     */
    Permission fullUpdate(int id, UpdatePermRequest req);

    /**
     * Delete permission
     * @param id permission ID
     */
    void deletePerm(int id);

    /**
     * Batch assign perm to users
     *
     * @param id       permission id
     * @param items    id exp item
     * @param expireAt
     * @return batch processing result
     */
    BatchResult assignPermToUsers(Integer id, List<Long> items, Instant expireAt);

    /**
     * List a permission's users
     * @param id permission id
     * @param pageable pageable
     * @return page of users
     */
    Page<User> listPermUsers(int id, Pageable pageable);

    /**
     * Batch replace a permission's users, the old user-permission relations will be removed
     * @param id the permission id
     * @param userUids user uids
     * @param expiresAt the permission expiration. will never expire if set null
     * @return batch processing result
     */
    BatchResult replacePermUsers(int id, List<Long> userUids, Instant expiresAt);

    /**
     * Batch remove a permission's users.
     * @param id target permission id
     * @param userUids target userUids
     * @return batch processing result
     */
    BatchResult removePermUsers(int id, @NotNull List<Long> userUids);
}
