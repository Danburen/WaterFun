package org.waterwood.waterfunadminservice.infrastructure.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import org.waterwood.waterfunadminservice.api.response.AuditTaskRes;
import org.waterwood.waterfunservicecore.api.message.ModerationConsumerMessage;
import org.waterwood.waterfunservicecore.entity.audit.AuditTask;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE,
        componentModel = "spring",
        nullValuePropertyMappingStrategy = org.mapstruct.NullValuePropertyMappingStrategy.IGNORE)
public interface AuditTaskMapper {

    @Mapping(target = "auditor", ignore = true)
    @Mapping(target = "submitter", ignore = true)
    @Mapping(source = "id", target = "taskId")
    void toModerateTaskResponse(AuditTask auditTask, @MappingTarget AuditTaskRes target);

    @Mapping(source = "submitter.uid", target = "submitterId")
    ModerationConsumerMessage toModerationConsumerMessage(AuditTask task);
}
