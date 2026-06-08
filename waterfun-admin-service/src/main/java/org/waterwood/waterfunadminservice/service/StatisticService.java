package org.waterwood.waterfunadminservice.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.waterwood.waterfunadminservice.api.response.SiteStatisticResponse;

import java.time.LocalDate;

public interface StatisticService {

    /**
     * List site statistics with optional date range filter.
     */
    Page<SiteStatisticResponse> listStatistics(LocalDate startDate, LocalDate endDate, Pageable pageable);

    /**
     * Get statistics for a specific date.
     * @throws org.waterwood.waterfunservicecore.exception.notfound.NotFoundException if not found
     */
    SiteStatisticResponse getStatistic(LocalDate date);

    /**
     * Get the most recent site statistics record.
     * @throws org.waterwood.waterfunservicecore.exception.notfound.NotFoundException if no record exists
     */
    SiteStatisticResponse getLatestStatistic();
}
