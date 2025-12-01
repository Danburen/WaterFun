package org.waterwood.waterfunadminservice.infrastructure.mapper;

import org.waterwood.waterfunadminservice.dto.request.user.UpdateUserProfileRequest;
import org.waterwood.waterfunadminservice.dto.response.user.UserProfileResponse;
import org.waterwood.waterfunservicecore.entity.user.UserProfile;
import org.mapstruct.*;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserProfileMapper {
    UserProfile toEntity(UserProfileResponse userProfileResponse);

    UserProfileResponse toResponse(UserProfile userProfile);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    UserProfile partialUpdate(UserProfileResponse userProfileResponse, @MappingTarget UserProfile userProfile);

    UserProfile toEntity(UpdateUserProfileRequest body);
}