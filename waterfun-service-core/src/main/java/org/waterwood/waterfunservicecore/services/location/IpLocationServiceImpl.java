package org.waterwood.waterfunservicecore.services.location;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.lionsoul.ip2region.xdb.Searcher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.waterwood.utils.StringUtil;

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

    @Value("${ip2region.db-location:classpath:ip2region.xdb}")
    private Resource dbResource;

    @PostConstruct
    public void init() {
        try (InputStream in = dbResource.getInputStream()) {
            byte[] cBuff = in.readAllBytes();
            searcher = Searcher.newWithBuffer(cBuff);
            log.info("Ip2region searcher initialized successfully from {}", dbResource.getDescription());
        } catch (Exception e) {
            log.warn("Failed to load ip2region xdb from {}. IP location lookup disabled.", dbResource != null ? dbResource.getDescription() : "null", e);
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
