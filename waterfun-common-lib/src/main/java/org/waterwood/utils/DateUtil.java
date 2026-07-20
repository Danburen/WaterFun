package org.waterwood.utils;

import java.time.LocalDate;
import java.time.ZoneId;

public final class DateUtil {
    public static long getSecondsUntilMidnight() {
        long now = System.currentTimeMillis() / 1000;
        LocalDate today = LocalDate.now();
        long midnight = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toEpochSecond();
        return midnight - now;
    }
}
