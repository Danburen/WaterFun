package org.waterwood.waterfunservicecore.utils;

import org.waterwood.utils.codec.Snowflake;

public final class ContentIdGenerator {
    private static final long DETACENTER_CONTENT = 1L;

    private static final long WORKER_POST = 0L;
    private static final Snowflake postGen = Snowflake.of(WORKER_POST, DETACENTER_CONTENT);

    public static long nextPostId(){
        return postGen.nextId();
    };
}
