package org.waterwood.waterfunadminservice.infrastructure.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import org.waterwood.waterfunadminservice.api.response.content.audit.ModerationResourceRes;
import org.waterwood.waterfunservicecore.entity.audit.resource.AuditTaskResource;

@Mapper(
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface AuditTaskResourceMapper {
    @Mapping(source = "auditor.uid", target = "auditorId")
    @Mapping(source = "task.id", target = "taskId")
    ModerationResourceRes toModerationResourceRes(AuditTaskResource auditTaskResource);
}
