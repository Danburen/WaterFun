package org.waterwood.waterfunservice.infrastructure.mapper;

import org.mapstruct.*;
import org.waterwood.waterfunservice.api.response.InboxNotificationRes;
import org.waterwood.waterfunservicecore.entity.notification.Inbox;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface InboxSystemMapper {
    Inbox toEntity(InboxNotificationRes inboxNotificationRes);

    InboxNotificationRes toDto(Inbox inbox);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Inbox partialUpdate(InboxNotificationRes inboxNotificationRes, @MappingTarget Inbox inbox);
}