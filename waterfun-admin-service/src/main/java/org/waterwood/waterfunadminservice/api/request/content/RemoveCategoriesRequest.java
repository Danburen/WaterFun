package org.waterwood.waterfunadminservice.api.request.content;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RemoveCategoriesRequest {
    private List<Integer> categoryIds;
}
