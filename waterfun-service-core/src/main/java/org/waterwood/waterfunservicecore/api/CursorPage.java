package org.waterwood.waterfunservicecore.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.function.Function;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CursorPage<T, ID> {
    private List<T> list;
    private ID nextCursor;
    private Boolean hasNext;

    public static <T, ID> CursorPage<T, ID> of(List<T> list, int limit, Function<T, ID> mapper) {
        boolean hasNext = list.size() > limit;
        if (hasNext) {
            list = list.subList(0, limit);
        }

        ID nextCursor = hasNext ? mapper.apply(list.getLast()) : null;

        return new CursorPage<>(list, nextCursor, hasNext);
    }

    public <R> CursorPage<R, ID> map(Function<T, R> mapper) {
        return new CursorPage<>(
                list.stream().map(mapper).toList(),
                nextCursor,
                hasNext
        );
    }
}
