package org.waterwood.waterfunservicecore.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CursorPage<T, ID> {
    private List<T> list;
    private ID nextCursor;
    private Boolean hasNext;
}
