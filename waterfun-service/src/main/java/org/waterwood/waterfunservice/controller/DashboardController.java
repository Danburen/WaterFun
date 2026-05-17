package org.waterwood.waterfunservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.waterwood.waterfunservice.api.response.SalesDataDTO;
import org.waterwood.waterfunservice.service.DashboardService;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    private final DashboardService dashboardService;
    @GetMapping("/sales-data")
    public List<SalesDataDTO> getSalesData() {
        return dashboardService.getAllSalesData();
    }

    @PostMapping("/sales-data")
    public void addSalesData(@RequestBody SalesDataDTO salesDataDTO) {
        dashboardService.createSalesData(salesDataDTO);
    }
}
