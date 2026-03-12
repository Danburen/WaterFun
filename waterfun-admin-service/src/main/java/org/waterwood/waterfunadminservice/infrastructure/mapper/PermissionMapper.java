package org.waterwood.waterfunadminservice.infrastructure.mapper;


import org.mapstruct.*;
import org.waterwood.waterfunadminservice.api.request.perm.CreatePermRequest;
import org.waterwood.waterfunadminservice.api.request.perm.PatchPermRequest;
import org.waterwood.waterfunadminservice.api.request.perm.UpdatePermRequest;
import org.waterwood.waterfunadminservice.api.response.PermissionResp;
import org.waterwood.waterfunservicecore.entity.Permission;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE,
        componentModel = MappingConstants.ComponentModel.SPRING)
public interface PermissionMapper {
    @Mapping(source = "parentId", target = "parent.id")
    Permission toEntity(PermissionResp permissionResp);

    @Mapping(source = "parent.id", target = "parentId")
    PermissionResp toPermissionResp(Permission permission);

    Permission toEntity(CreatePermRequest body);

    @Mapping(source = "parentId", target = "parent.id")
    Permission toEntity(PatchPermRequest patchPermRequest);

    @Mapping(source = "parent.id", target = "parentId")
    PatchPermRequest toPermPatchRequest(Permission permission);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Permission partialUpdate(PatchPermRequest patchPermRequest, @MappingTarget Permission permission);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
    Permission fullUpdate(UpdatePermRequest updatePermRequest, @MappingTarget Permission entity);

    @Mapping(source = "parent.id", target = "parentId")
    UpdatePermRequest toPermUpdateRequest(Permission permission);

}