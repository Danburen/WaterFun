package org.waterwood.waterfunservicecore.infrastructure.mapper;

import org.mapstruct.*;
import org.waterwood.waterfunservicecore.api.req.user.UpdateUserProfileRequest;
import org.waterwood.waterfunservicecore.entity.user.User;
import org.waterwood.waterfunservicecore.api.resp.user.UserInfoResponse;
import org.waterwood.waterfunservicecore.entity.user.UserProfile;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = "spring")
public interface UserMapper {
    UserInfoResponse toUserInfoResponse(User user);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    User partialUpdate(UserInfoResponse userInfoResponse, @MappingTarget User user);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void toEntity(UpdateUserProfileRequest dto, @MappingTarget User u);
}
