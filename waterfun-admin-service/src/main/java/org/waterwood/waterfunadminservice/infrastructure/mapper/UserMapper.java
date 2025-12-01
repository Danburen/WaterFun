package org.waterwood.waterfunadminservice.infrastructure.mapper;

import org.mapstruct.*;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import org.waterwood.waterfunadminservice.dto.request.user.UpdateUserProfileRequest;
import org.waterwood.waterfunadminservice.dto.response.user.UserInfoResponse;
import org.waterwood.waterfunservicecore.entity.user.User;
import org.waterwood.waterfunservicecore.entity.user.UserProfile;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = "spring")
public interface UserMapper {
    UserProfile updateRequestToEntity(UpdateUserProfileRequest request);

    UserInfoResponse toUserInfoResponse(User user);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    User partialUpdate(UserInfoResponse userInfoResponse, @MappingTarget User user);
}
