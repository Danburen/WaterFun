package org.waterwood.waterfunadminservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.waterwood.waterfunadminservice.api.response.SiteStatisticResponse;
import org.waterwood.waterfunservicecore.entity.SiteStatistic;
import org.waterwood.waterfunservicecore.exception.notfound.NotFoundException;
import org.waterwood.waterfunservicecore.infrastructure.persistence.SiteStatisticRepository;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class StatisticServiceImpl implements StatisticService {
    private final SiteStatisticRepository siteStatisticRepository;

    @Override
    public Page<SiteStatisticResponse> listStatistics(LocalDate startDate, LocalDate endDate, Pageable pageable) {
        if (startDate != null && endDate != null) {
            return siteStatisticRepository.findAll(
                    (root, query, cb) -> cb.between(root.get("id"), startDate, endDate),
                    pageable
            ).map(this::toResponse);
        }
        return siteStatisticRepository.findAll(pageable).map(this::toResponse);
    }

    @Override
    public SiteStatisticResponse getStatistic(LocalDate date) {
        return toResponse(siteStatisticRepository.findById(date)
                .orElseThrow(() -> new NotFoundException("SiteStatistic date: " + date)));
    }

    @Override
    public SiteStatisticResponse getLatestStatistic() {
        return toResponse(siteStatisticRepository.findTopByOrderByUpdatedAtDesc()
                .orElseThrow(() -> new NotFoundException("No statistics found")));
    }

    private SiteStatisticResponse toResponse(SiteStatistic stat) {
        SiteStatisticResponse resp = new SiteStatisticResponse();
        resp.setStatDate(stat.getId());
        resp.setDailyVisits(stat.getDailyVisits());
        resp.setDailyUv(stat.getDailyUv());
        resp.setNewUsers(stat.getNewUsers());
        resp.setActiveUsers(stat.getActiveUsers());
        resp.setPeakOnline(stat.getPeakOnline());
        resp.setUpdatedAt(stat.getUpdatedAt());
        return resp;
    }
}
