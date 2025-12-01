package org.waterwood.waterfunadminservice.infrastructure.mapper;

import org.waterwood.waterfunadminservice.dto.request.post.CreateTagRequest;
import org.waterwood.waterfunadminservice.dto.request.post.UpdateTagRequest;
import org.waterwood.waterfunadminservice.dto.response.post.TagResponse;
import org.waterwood.waterfunservicecore.entity.post.Tag;
import org.mapstruct.*;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface TagMapper {
    Tag toEntity(TagResponse tagResponse);

    TagResponse toResponseDto(Tag tag);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Tag partialUpdate(TagResponse tagResponse, @MappingTarget Tag tag);

    Tag toEntity(CreateTagRequest request);

    Tag toEntity(UpdateTagRequest request);
}