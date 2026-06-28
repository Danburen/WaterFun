package org.waterwood.waterfunservicecore.services.search;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PostSearchService {
    Page<Long> searchPostIds(String keyword, Pageable pageable);
}
