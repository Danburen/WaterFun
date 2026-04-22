package org.waterwood.waterfunadminservice.infrastructure.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.waterwood.waterfunadminservice.api.response.ModerateTaskResponse;
import org.waterwood.waterfunservicecore.api.message.ModerationConsumerMessage;
import org.waterwood.waterfunservicecore.entity.audit.task.AuditTask;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE,
        componentModel = "spring",
        nullValuePropertyMappingStrategy = org.mapstruct.NullValuePropertyMappingStrategy.IGNORE)
public interface AuditTaskMapper {

    @Mapping(source = "submitter.uid", target = "submitterId")
    @Mapping(target = "content", ignore = true)
    ModerateTaskResponse toModerateTaskResponse(AuditTask auditTask);

    @Mapping(source = "submitter.uid", target = "submitterId")
    ModerationConsumerMessage toModerationConsumerMessage(AuditTask task);
}
