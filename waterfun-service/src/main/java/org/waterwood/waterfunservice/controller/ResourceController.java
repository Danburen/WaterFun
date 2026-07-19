package org.waterwood.waterfunservice.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.waterwood.api.ApiResponse;
import org.waterwood.waterfunservice.api.response.MiniFileResData;
import org.waterwood.waterfunservice.service.resource.ResourceService;

@RestController
@RequestMapping("/api/resource")
public class ResourceController {
    @Autowired
    private ResourceService resourceService;

    @GetMapping("{fileName}")
    public ApiResponse<MiniFileResData> getResource(@PathVariable String fileName) {
        return ApiResponse.success(resourceService.getResourceFile(fileName));
    }
}
