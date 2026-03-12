package org.waterwood.waterfunadminservice.infrastructure.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import org.waterwood.waterfunadminservice.api.request.post.CreateCategoryRequest;
import org.waterwood.waterfunadminservice.api.request.post.UpdateCategoryRequest;
import org.waterwood.waterfunadminservice.api.response.post.CategoryResponse;
import org.waterwood.waterfunservicecore.entity.post.Category;

import java.util.List;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE,
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface CategoryMapper {
    CategoryResponse toResponse(Category category);
    List<CategoryResponse> toResponseList(List<Category> categoryList);

    Category toEntity(CreateCategoryRequest body);

    Category toEntity(UpdateCategoryRequest body);
}
