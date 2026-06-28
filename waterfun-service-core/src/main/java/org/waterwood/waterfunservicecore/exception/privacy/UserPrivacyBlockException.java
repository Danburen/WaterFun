package org.waterwood.waterfunservicecore.exception.privacy;

import org.springframework.http.HttpStatus;
import org.waterwood.api.BaseResponseCode;
import org.waterwood.waterfunservicecore.exception.BizException;

/**
 * Base exception for all privacy-blocked scenarios.
 * Returns HTTP 403 Forbidden.
 */
public class UserPrivacyBlockException extends BizException {
    public UserPrivacyBlockException(BaseResponseCode code) {
        super(code, HttpStatus.FORBIDDEN);
    }
}
