package org.waterwood.common.constratin;

import static org.waterwood.common.KeyConstants.*;

public final class StatsKeyBuilder {
    private static final String STATS_PREFIX = "stats:";

    private StatsKeyBuilder() {
    }

    public static String lastActiveBuffer() {
        return LAST_ACTIVE + ":" + BUFFER;
    }
}
