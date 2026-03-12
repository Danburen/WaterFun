package org.waterwood.waterfunadminservice.infrastructure.mapper;

import org.mapstruct.*;
import org.waterwood.waterfunservicecore.entity.user.UserCounter;
import org.waterwood.waterfunadminservice.api.response.user.UserCounterARes;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserCounterMapper {
    UserCounter toEntity(UserCounterARes userCounterARes);

    UserCounterARes toDto(UserCounter userCounter);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    UserCounter partialUpdate(UserCounterARes userCounterARes, @MappingTarget UserCounter userCounter);
}