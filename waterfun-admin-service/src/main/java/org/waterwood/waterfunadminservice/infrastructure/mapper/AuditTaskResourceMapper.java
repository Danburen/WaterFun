package org.waterwood.waterfunadminservice.infrastructure.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import org.waterwood.waterfunadminservice.api.response.content.audit.ModerationResourceRes;
import org.waterwood.waterfunservicecore.entity.resource.AuditResource;

@Mapper(
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface AuditTaskResourceMapper {
    @Mapping(source = "task.id", target = "taskId")
    @Mapping(source = "auditor.uid", target = "auditorId")
    @Mapping(source = "resource.uuid", target = "resourceUuid")
    ModerationResourceRes toModerationResourceRes(AuditResource auditResource);
}
