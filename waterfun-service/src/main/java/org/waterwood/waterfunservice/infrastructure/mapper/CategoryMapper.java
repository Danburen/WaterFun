package org.waterwood.waterfunservice.infrastructure.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import org.waterwood.waterfunservice.api.request.content.CreateCategoryRequest;
import org.waterwood.waterfunservice.api.request.content.UpdateCategoryRequest;
import org.waterwood.waterfunservice.api.response.post.CategoryResponse;
import org.waterwood.waterfunservicecore.entity.post.Category;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE,
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface CategoryMapper {
    @Mapping(target = "parent", ignore = true)
    CategoryResponse toResponse(Category category);

    Category toEntity(CreateCategoryRequest body);

    Category toEntity(UpdateCategoryRequest body);
}
