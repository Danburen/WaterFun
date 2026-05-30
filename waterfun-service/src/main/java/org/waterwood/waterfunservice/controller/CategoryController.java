package org.waterwood.waterfunservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.waterwood.waterfunservice.api.request.content.CreateCategoryRequest;
import org.waterwood.api.ApiResponse;
import org.waterwood.waterfunservice.api.request.content.UpdateCategoryRequest;
import org.waterwood.waterfunservice.api.response.post.CategoryResponse;
import org.waterwood.waterfunservice.infrastructure.mapper.CategoryMapper;
import org.waterwood.waterfunservice.service.post.CategoryService;

import java.util.List;

@RestController
@RequestMapping("/api/post/category")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService categoryService;
    private final CategoryMapper categoryMapper;

    // Create a new category
    @PostMapping
    public ApiResponse<Void> addCategory(@RequestBody @Valid CreateCategoryRequest body){
        categoryService.createCategory(categoryMapper.toEntity(body));
        return ApiResponse.success();
    }

    @GetMapping
    public ApiResponse<List<CategoryResponse>> getCategories(){
        return ApiResponse.success(categoryMapper.toResponseList(
                categoryService.getCategories()
        ));
    }

    @GetMapping("/{id}")
    public ApiResponse<CategoryResponse> getCategory(@PathVariable Long id){
        return ApiResponse.success(categoryMapper.toResponse(
                categoryService.getCategory(id)
        ));
    }

    @PutMapping
    public ApiResponse<Void> updateCategory(@RequestBody @Valid UpdateCategoryRequest body){
        categoryService.updateCategory(categoryMapper.toEntity(body));
        return ApiResponse.success();
    }

    @DeleteMapping
    public ApiResponse<Void> deleteCategory(@RequestParam Long id){
        categoryService.deleteCategory(id);
        return ApiResponse.success();
    }
}
