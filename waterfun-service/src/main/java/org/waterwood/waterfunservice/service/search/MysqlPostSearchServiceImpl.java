package org.waterwood.waterfunservice.service.search;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.waterwood.utils.StringUtil;
import org.waterwood.waterfunservicecore.infrastructure.persistence.PostRepository;
import org.waterwood.waterfunservicecore.services.search.PostSearchService;

import java.util.Arrays;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "search.engine", havingValue = "mysql", matchIfMissing = true)
public class MysqlPostSearchServiceImpl implements PostSearchService {

    private final PostRepository postRepository;

    @Override
    public Page<Long> searchPostIds(String keyword, Pageable pageable) {
        if (StringUtil.isBlank(keyword)) {
            return Page.empty();
        }
        String raw = keyword.trim();
        String escaped = escapeFulltext(raw);
        String query = Arrays.stream(escaped.split("\\s+"))
                .filter(w -> !w.isEmpty())
                .map(w -> w + "*")
                .collect(Collectors.joining(" "));
        return postRepository.searchByFulltext(query, raw, pageable);
    }

    private String escapeFulltext(String keyword) {
        return keyword
                .replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\"", "\\\"")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replace("+", "\\+")
                .replace("-", "\\-")
                .replace("~", "\\~")
                .replace("@", "\\@")
                .replace(">", "\\>")
                .replace("<", "\\<");
    }
}
