package org.waterwood.waterfunservice.infrastructure.mapper;

import org.mapstruct.*;
import org.waterwood.waterfunservice.api.response.SystemNotificationRes;
import org.waterwood.waterfunservicecore.entity.notification.InboxSystem;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface InboxSystemMapper {
    InboxSystem toEntity(SystemNotificationRes systemNotificationRes);

    SystemNotificationRes toDto(InboxSystem inboxSystem);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    InboxSystem partialUpdate(SystemNotificationRes systemNotificationRes, @MappingTarget InboxSystem inboxSystem);
}