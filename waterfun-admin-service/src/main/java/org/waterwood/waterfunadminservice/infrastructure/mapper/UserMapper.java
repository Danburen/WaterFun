package org.waterwood.waterfunadminservice.infrastructure.mapper;

import org.mapstruct.*;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import org.waterwood.waterfunadminservice.api.request.user.UserProfileUpdateAReq;
import org.waterwood.waterfunservicecore.entity.user.User;
import org.waterwood.waterfunadminservice.api.response.user.UserInfoARes;
import org.waterwood.waterfunservicecore.entity.user.UserProfile;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = "spring")
public interface UserMapper {
    UserProfile updateRequestToEntity(UserProfileUpdateAReq request);

    User toEntity(UserInfoARes userInfoARes);

    UserInfoARes toDto(User user);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    User partialUpdate(UserInfoARes userInfoARes, @MappingTarget User user);
}
