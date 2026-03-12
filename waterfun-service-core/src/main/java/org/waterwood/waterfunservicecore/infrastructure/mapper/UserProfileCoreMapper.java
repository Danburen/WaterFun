package org.waterwood.waterfunservicecore.infrastructure.mapper;

import org.mapstruct.*;
import org.waterwood.waterfunservicecore.api.req.user.UpdateUserProfileRequest;
import org.waterwood.waterfunservicecore.entity.user.UserProfile;
import org.waterwood.waterfunservicecore.api.resp.user.UserProfileResponse;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserProfileCoreMapper {
    UserProfile toEntity(UserProfileResponse userProfileResponse);

    UserProfileResponse toResponse(UserProfile userProfile);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    UserProfile partialUpdate(UserProfileResponse userProfileResponse, @MappingTarget UserProfile userProfile);
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    UserProfile toEntity(UpdateUserProfileRequest body,  @MappingTarget UserProfile entity);
}