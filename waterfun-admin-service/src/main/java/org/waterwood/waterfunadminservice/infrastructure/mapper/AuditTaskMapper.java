package org.waterwood.waterfunadminservice.infrastructure.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.waterwood.waterfunadminservice.api.response.AuditTaskRes;
import org.waterwood.waterfunservicecore.api.message.ModerationConsumerMessage;
import org.waterwood.waterfunservicecore.entity.audit.AuditTask;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE,
        componentModel = "spring",
        nullValuePropertyMappingStrategy = org.mapstruct.NullValuePropertyMappingStrategy.IGNORE)
public interface AuditTaskMapper {

    @Mapping(source = "submitter.uid", target = "submitterUid")
    @Mapping(source = "submitter.uid", target = "submitterId")
    @Mapping(source = "id", target = "taskId")
    AuditTaskRes toModerateTaskResponse(AuditTask auditTask);

    @Mapping(source = "submitter.uid", target = "submitterId")
    ModerationConsumerMessage toModerationConsumerMessage(AuditTask task);
}
