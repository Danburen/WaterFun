package org.waterwood.waterfunservice.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.lionsoul.ip2region.xdb.Searcher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.waterwood.utils.StringUtil;
import org.waterwood.waterfunservicecore.services.location.IpLocationService;

import java.io.InputStream;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class IpLocationServiceImpl implements IpLocationService {

    private final Cache<String, Map<String, String>> locationCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofHours(6))
            .maximumSize(50_000)
            .build();

    private Searcher searcher;

    @Value("${ip2region.db-location:ip2region.xdb}")
    private String dbLocation;

    @PostConstruct
    public void init() {
        try (InputStream in = new ClassPathResource(dbLocation).getInputStream()) {
            byte[] cBuff = in.readAllBytes();
            searcher = Searcher.newWithBuffer(cBuff);
            log.info("Ip2region searcher initialized successfully");
        } catch (Exception e) {
            log.warn("Failed to load ip2region xdb from classpath: {}. IP location lookup disabled.", dbLocation, e);
            searcher = null;
        }
    }

    @Override
    public Map<String, String> lookup(String ip) {
        if (StringUtil.isBlank(ip)) {
            return Map.of("country", "", "province", "", "city", "");
        }

        return locationCache.get(ip, key -> doLookup(key));
    }

    private Map<String, String> doLookup(String ip) {
        if (searcher == null) {
            return Map.of("country", "", "province", "", "city", "");
        }

        try {
            String result = searcher.search(ip);
            if (result == null || result.isBlank()) {
                return Map.of("country", "", "province", "", "city", "");
            }

            String[] parts = result.split("\\|");
            String country = parts.length > 0 ? parts[0] : "";
            String province = parts.length > 2 ? parts[2] : "";
            String city = parts.length > 3 ? parts[3] : "";

            Map<String, String> location = new HashMap<>();
            location.put("country", "0".equals(country) ? "" : country);
            location.put("province", "0".equals(province) ? "" : province);
            location.put("city", "0".equals(city) ? "" : city);
            return location;
        } catch (Exception e) {
            log.debug("Failed to lookup location for IP: {}", ip, e);
            return Map.of("country", "", "province", "", "city", "");
        }
    }
}
