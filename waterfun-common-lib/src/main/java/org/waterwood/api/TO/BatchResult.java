package org.waterwood.api.TO;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

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

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<Integer> ignoredIds;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<Integer> failedIds;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String message;

    public static BatchResult of(int requested, int success) {
        return BatchResult.builder()
                .requested(requested)
                .success(success)
                .ignored(requested - success)
                .build();
    }

    public static BatchResult empty() {
        return new BatchResult(0, 0, 0, 0, null, null, null);
    }
 }
