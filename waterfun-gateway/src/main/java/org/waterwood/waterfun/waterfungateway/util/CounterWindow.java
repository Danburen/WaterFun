package org.waterwood.waterfun.waterfungateway.util;

import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.atomic.AtomicInteger;

@Getter
@Setter
public class CounterWindow {
    private final AtomicInteger counter = new AtomicInteger(0);
    private long windowStartEpochSecond;

    public CounterWindow(long windowStartEpochSecond) {
        this.windowStartEpochSecond = windowStartEpochSecond;
    }

}