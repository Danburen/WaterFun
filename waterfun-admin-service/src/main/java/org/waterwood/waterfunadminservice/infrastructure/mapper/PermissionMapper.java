package org.waterwood.waterfunadminservice.infrastructure.mapper;


import org.mapstruct.*;
import org.waterwood.waterfunadminservice.api.request.perm.CreatePermRequest;
import org.waterwood.waterfunadminservice.api.request.perm.UpdatePermRequest;
import org.waterwood.waterfunadminservice.api.response.perm.PermissionResp;
import org.waterwood.waterfunservicecore.entity.Permission;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE,
        componentModel = MappingConstants.ComponentModel.SPRING)
public interface PermissionMapper {
    @Mapping(source = "parentId", target = "parent.id")
    Permission toEntity(PermissionResp permissionResp);

    @Mapping(source = "parent.id", target = "parentId")
    PermissionResp toPermissionResp(Permission permission);

    Permission toEntity(CreatePermRequest body);

    @Mapping(target = "parent", ignore = true)
    @Mapping(target = "code", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Permission update(UpdatePermRequest updatePermRequest, @MappingTarget Permission entity);

    @Mapping(source = "parent.id", target = "parentId")
    UpdatePermRequest toPermUpdateRequest(Permission permission);

}