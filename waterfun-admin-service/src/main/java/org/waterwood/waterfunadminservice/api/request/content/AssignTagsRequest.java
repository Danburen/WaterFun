package org.waterwood.waterfunadminservice.api.request.content;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AssignTagsRequest {
    private List<Integer> tagIds;
}
