package org.waterwood.waterfunservice.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.waterwood.waterfunservicecore.entity.IpBan;
import org.waterwood.waterfunservicecore.infrastructure.persistence.IpBanRepository;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class IpBanService {
    private final Cache<String, Long> banCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(5))  // clean for 5 minutes no react
            .maximumSize(100_000)
            .build();
    private final IpBanRepository ipBanRepository;


    @PostConstruct
    public void loadBanList() {
        refreshCache();
    }

    @Scheduled(fixedRate = 60_000)
    public void refreshCache() {
        List<IpBan> activeBans = ipBanRepository.findActiveList(LocalDateTime.now());

        // rebuild cache
        banCache.invalidateAll();
        for (IpBan ban : activeBans) {
            long expireTs = ban.getExpiresAt() == null ? 0 : ban.getExpiresAt().getEpochSecond();
            banCache.put(ban.getIp(), expireTs);
        }
    }

    public boolean isBanned(String ip) {
        Long expireTs = banCache.getIfPresent(ip);
        if (expireTs == null) return false;
        if (expireTs == 0) return true;  // permanent ban

        if (Instant.now().getEpochSecond() > expireTs) {
            banCache.invalidate(ip);
            return false;
        }
        return true;
    }
}
