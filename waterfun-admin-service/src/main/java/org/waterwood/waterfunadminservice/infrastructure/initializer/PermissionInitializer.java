package org.waterwood.waterfunadminservice.infrastructure.initializer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.waterwood.waterfunservicecore.entity.BanPermission;
import org.waterwood.waterfunservicecore.entity.perm.Permission;
import org.waterwood.waterfunservicecore.entity.perm.PermissionType;
import org.waterwood.waterfunservicecore.infrastructure.persistence.PermissionRepo;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(1)
public class PermissionInitializer implements CommandLineRunner {

    private final PermissionRepo permissionRepo;

    @Override
    public void run(String... args) {

        List<String> expectPermissionCodes = Arrays.stream(BanPermission.values())
                .map(BanPermission::getCode)
                .toList();
        List<String> existsPermissionCodes = permissionRepo.findByCodeIn(expectPermissionCodes)
                .stream().map(Permission::getCode).toList();
        List<Permission> toSave = Arrays.stream(BanPermission.values())
                .filter(bp -> !existsPermissionCodes.contains(bp.getCode()))
                .map(bp -> {
                    Permission p = new Permission();
                    p.setCode(bp.getCode());
                    p.setName(bp.getName());
                    p.setDescription(bp.getDescription());
                    p.setType(PermissionType.BAM);
                    p.setResource(null);
                    p.setIsSystem(true);
                    return p;
                }).toList();
        if (!toSave.isEmpty()) {
            permissionRepo.saveAll(toSave);
            log.info("Initialized {} ban permissions: {}", toSave.size(),
                    toSave.stream().map(Permission::getCode).toList());
        }
    }
}
