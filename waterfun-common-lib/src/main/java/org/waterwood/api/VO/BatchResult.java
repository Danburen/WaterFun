package org.waterwood.api.VO;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collection;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BatchResult {
    private int requested;
    private int success;
    private int ignored;
    private int failed;

    public static BatchResult of(int requested, int success) {
        return BatchResult.builder()
                .requested(requested)
                .success(success)
                .ignored(requested - success)
                .build();
    }

    public static BatchResult empty() {
        return new BatchResult(0, 0, 0, 0);
    }

    public static <T> BatchResult ofNullable(Collection<T> collection, int success) {
        if(collection != null && !collection.isEmpty()) {
            if (success < 0 || success > collection.size()) {
                throw new IllegalArgumentException(
                        "success must be between 0 and " + collection.size() + ", got: " + success);
            }
            return new BatchResult(collection.size(), success, collection.size() - success, 0);
        }
        return BatchResult.empty();
    }
}
