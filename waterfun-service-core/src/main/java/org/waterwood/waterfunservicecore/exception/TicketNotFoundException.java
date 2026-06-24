package org.waterwood.waterfunservicecore.exception;

import org.waterwood.api.BaseResponseCode;

public class TicketNotFoundException extends BizException {
    public TicketNotFoundException() {
        super(BaseResponseCode.TicketNotFoundException);

    }
}
