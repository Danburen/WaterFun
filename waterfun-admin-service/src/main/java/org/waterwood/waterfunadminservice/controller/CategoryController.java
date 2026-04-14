package org.waterwood.waterfunadminservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;
import org.waterwood.api.ApiResponse;
import org.waterwood.api.VO.BatchResult;
import org.waterwood.api.VO.OptionVO;
import org.waterwood.waterfunadminservice.api.request.content.RemoveCategoriesRequest;
import org.waterwood.waterfunadminservice.api.request.content.UpdateCategoryRequest;
import org.waterwood.waterfunadminservice.api.response.content.CategoryResponse;
import org.waterwood.waterfunadminservice.infrastructure.mapper.CategoryMapper;
import org.waterwood.waterfunadminservice.service.content.CategoryService;
import org.waterwood.waterfunservicecore.entity.post.Category;
import org.waterwood.waterfunservicecore.infrastructure.persistence.utils.CategorySpec;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@RequestMapping("/api/admin/categorys")
public class CategoryController {
    private final CategoryService categoryService;
    private final CategoryMapper categoryMapper;

    @GetMapping("/list")
    public ApiResponse<Page<CategoryResponse>> list(@RequestParam(required = false) String name,
                                                    @RequestParam(required = false) String slug,
                                                    @RequestParam(required = false) Integer parentId,
                                                    @RequestParam(required = false) Long creatorId,
                                                    @RequestParam(required = false) Instant createStart,
                                                    @RequestParam(required = false) Instant createEnd,
                                                    @PageableDefault Pageable pageable) {
        Specification<Category> spec = CategorySpec.of(name, slug, parentId, creatorId, createStart, createEnd);
        Page<Category> categories = categoryService.list(spec, pageable);
        return ApiResponse.success(
                categories.map(categoryMapper::toResponse)
        );
    }

    @DeleteMapping
    public ApiResponse<BatchResult> deleteCategories(@RequestBody @Valid RemoveCategoriesRequest req){
        return ApiResponse.success(
                categoryService.deleteCategories(req)
        );
    }

    @GetMapping("/{id}")
    public ApiResponse<CategoryResponse> get(@PathVariable Integer id) {
        return ApiResponse.success(
                categoryMapper.toResponse(categoryService.getById(id))
        );
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Integer id) {
        categoryService.removeById(id);
        return ApiResponse.success();
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Integer id, @RequestBody UpdateCategoryRequest req) {
        categoryService.update(id, req);
        return ApiResponse.success();
    }

    @GetMapping("/options")
    public ApiResponse<List<OptionVO<Integer>>> getOptions() {
        return ApiResponse.success(
                categoryService.getOptions()
        );
    }
}
