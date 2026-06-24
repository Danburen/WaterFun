package org.waterwood.waterfunservicecore.exception;

import org.waterwood.api.BaseResponseCode;

/**
 * Thrown when a banned user attempts an action they are restricted from.
 * Returns a 401 response indicating the user is banned.
 */
public class BanForbiddenException extends BizException {

    public BanForbiddenException() {
        super(BaseResponseCode.BAN_FORBIDDEN.getCode(), "ban.forbidden");
        setHttpStatusCode(403);
    }
}
