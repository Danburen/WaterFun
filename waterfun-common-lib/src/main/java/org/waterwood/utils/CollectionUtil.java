package org.waterwood.utils;

import java.util.Collection;

public final class CollectionUtil {
    public static <T>  boolean isEmpty(Collection<T> collection){
        return collection == null || collection.isEmpty();
    }

    public static <T> boolean isNotEmpty(Collection<T> collection){
        return !isEmpty(collection);
    }
}
