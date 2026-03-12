package org.waterwood.waterfunservicecore.services.sys;

import static org.waterwood.common.KeyConstants.FS;

public class CloudKeyBuilder {
    private static final String CLOUD_KEY_PREFIX = "cloud:";

    public static String fs(){
        return CLOUD_KEY_PREFIX + FS;
    }
}
