package org.waterwood.api;

import java.util.Map;
import java.util.Objects;

public interface Mappable<T> {
    Map<String,T> toMap();
}
