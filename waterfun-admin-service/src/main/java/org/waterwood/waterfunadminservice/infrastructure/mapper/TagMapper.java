package org.waterwood.waterfunadminservice.infrastructure.mapper;

import org.waterwood.waterfunadminservice.api.request.content.CreateTagRequest;
import org.waterwood.waterfunadminservice.api.request.content.UpdateTagReq;
import org.waterwood.waterfunadminservice.api.request.content.UpdateTagRequest;
import org.waterwood.waterfunadminservice.api.response.content.TagResponse;
import org.waterwood.waterfunservicecore.entity.post.Tag;
import org.mapstruct.*;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE,
        componentModel = MappingConstants.ComponentModel.SPRING,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface TagMapper {
    Tag toEntity(TagResponse tagResponse);

    @Mapping(source = "creator.uid", target = "creatorId")
    TagResponse toResponseDto(Tag tag);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Tag partialUpdate(TagResponse tagResponse, @MappingTarget Tag tag);

    Tag toEntity(CreateTagRequest request);

    Tag toEntity(UpdateTagRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void partialUpdate(UpdateTagReq req,@MappingTarget Tag tag);
}