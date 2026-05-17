package org.waterwood.waterfunservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.waterwood.waterfunservice.api.response.SalesDataDTO;
import org.waterwood.waterfunservice.entity.SalesData;
import org.waterwood.waterfunservice.infrastructure.persistence.DashboardRepo;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {
    private final DashboardRepo dashboardRepo;

    public List<SalesDataDTO> getAllSalesData() {
        List<SalesData> salesDataList = dashboardRepo.findAll();

        return salesDataList.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    private SalesDataDTO convertToVO(SalesData salesData) {
        SalesDataDTO dto = new SalesDataDTO();
        dto.setProductName(salesData.getProductName());
        dto.setSalesAmount(salesData.getSalesAmount());
        return dto;
    }

    public SalesData createSalesData(SalesDataDTO dto) {
        SalesData salesData = new SalesData();
        salesData.setProductName(dto.getProductName());
        salesData.setSalesAmount(dto.getSalesAmount());
        return dashboardRepo.save(salesData);
    }
}
