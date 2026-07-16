package org.waterwood.api;

import java.util.Map;

public interface Mappable<T> {
    Map<String,T> toMap();
}
