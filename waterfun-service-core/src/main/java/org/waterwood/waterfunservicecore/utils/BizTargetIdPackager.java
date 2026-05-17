package org.waterwood.waterfunservicecore.utils;

import org.jetbrains.annotations.NotNull;
import org.waterwood.common.KeyConstants;

public final class BizTargetIdPackager {
    public static BizPayload ofUser(Long userUid,@NotNull String domain){
        return new BizPayload(KeyConstants.USER, String.valueOf(userUid), domain, null);
    }
}
