package org.waterwood.waterfunservice.infrastructure.mapper;

import org.mapstruct.*;
import org.waterwood.waterfunservice.api.request.content.CreateTagRequest;
import org.waterwood.waterfunservice.api.request.content.UpdateTagRequest;
import org.waterwood.waterfunservice.api.response.post.TagResponse;
import org.waterwood.waterfunservicecore.entity.post.Tag;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface TagMapper {
    Tag toEntity(TagResponse tagResponse);

    TagResponse toResponseDto(Tag tag);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Tag partialUpdate(TagResponse tagResponse, @MappingTarget Tag tag);

    Tag toEntity(CreateTagRequest request);

    Tag toEntity(UpdateTagRequest request);
}