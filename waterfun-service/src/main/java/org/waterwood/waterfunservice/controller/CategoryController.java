package org.waterwood.waterfunservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.waterwood.api.VO.OptionVO;
import org.waterwood.api.ApiResponse;
import org.waterwood.waterfunservice.service.post.CategoryService;

import java.util.List;

@RestController
@RequestMapping("/api/post/category")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService categoryService;

    @GetMapping("/options")
    public ApiResponse<List<OptionVO<Long>>> getCategoryOptions(){
        return ApiResponse.success(categoryService.getCategoryOptions());
    }
}
