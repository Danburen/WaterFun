package org.waterwood.waterfunadminservice.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.waterwood.waterfunadminservice.api.response.SiteStatisticResponse;

import java.time.LocalDate;

public interface StatisticService {
    Page<SiteStatisticResponse> listStatistics(LocalDate startDate, LocalDate endDate, Pageable pageable);
    SiteStatisticResponse getStatistic(LocalDate date);
    SiteStatisticResponse getLatestStatistic();
}
