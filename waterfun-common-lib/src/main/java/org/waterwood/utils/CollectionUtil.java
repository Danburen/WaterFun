package org.waterwood.utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class CollectionUtil {
    public static <T>  boolean isEmpty(Collection<T> collection){
        return collection == null || collection.isEmpty();
    }

    public static <T> boolean isNotEmpty(Collection<T> collection){
        return !isEmpty(collection);
    }

    /**
     * Check if the size of two collections are the same, if either of them is null, return false
     * @param collection1 first given collection
     * @param collection2 second given collection
     * @return boolean
     */
    public static boolean isSameSize(Collection<?> collection1, Collection<?> collection2) {
        if(collection1 == null || collection2 == null){
            return false;
        }

        return collection1.size() == collection2.size();
    }

    public static <T extends Serializable> List<Serializable> serialize(List<T> ids) {
        return new ArrayList<Serializable>(ids);
    }
}
