package org.waterwood.waterfunservicecore.infrastructure.mapper;

import org.mapstruct.*;
import org.waterwood.waterfunservicecore.api.req.user.UpdateUserProfileRequest;
import org.waterwood.waterfunservicecore.entity.user.User;
import org.waterwood.waterfunservicecore.api.resp.user.UserInfoResponse;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = "spring")
public interface UserCoreMapper {
    UserInfoResponse toUserInfoResponse(User user);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    User partialUpdate(UserInfoResponse userInfoResponse, @MappingTarget User user);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void toEntity(UpdateUserProfileRequest dto, @MappingTarget User u);
}
