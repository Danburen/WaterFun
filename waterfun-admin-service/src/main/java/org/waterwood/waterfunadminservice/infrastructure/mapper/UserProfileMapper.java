package org.waterwood.waterfunadminservice.infrastructure.mapper;

import org.waterwood.waterfunadminservice.api.request.user.UserProfileUpdateAReq;
import org.waterwood.waterfunadminservice.api.response.user.UserProfileRes;
import org.waterwood.waterfunservicecore.entity.user.UserProfile;
import org.mapstruct.*;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserProfileMapper {
    UserProfile toEntity(UserProfileRes userProfileRes);

    UserProfileRes toResponse(UserProfile userProfile);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    UserProfile partialUpdate(UserProfileRes userProfileRes, @MappingTarget UserProfile userProfile);

    UserProfile toEntity(UserProfileUpdateAReq body);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    UserProfile toEntity(UserProfileUpdateAReq body, @MappingTarget UserProfile p);
}