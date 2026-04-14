package org.waterwood.waterfunadminservice.infrastructure.mapper;

import org.mapstruct.*;
import org.waterwood.waterfunadminservice.api.request.content.CreateCategoryRequest;
import org.waterwood.waterfunadminservice.api.request.content.UpdateCategoryRequest;
import org.waterwood.waterfunadminservice.api.response.content.CategoryResponse;
import org.waterwood.waterfunservicecore.entity.post.Category;

import java.util.List;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE,
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface CategoryMapper {
    @Mapping(source = "creator.uid", target = "creatorId")
    @Mapping(source = "parent.id", target = "parentId")
    CategoryResponse toResponse(Category category);
    List<CategoryResponse> toResponseList(List<Category> categoryList);

    Category toEntity(CreateCategoryRequest body);

    Category toEntity(UpdateCategoryRequest body);


    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Category partialUpdate(UpdateCategoryRequest req,@MappingTarget Category category);
}
