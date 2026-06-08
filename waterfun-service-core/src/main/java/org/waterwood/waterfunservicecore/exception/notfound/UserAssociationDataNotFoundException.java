package org.waterwood.waterfunservicecore.exception.notfound;

import org.waterwood.api.BaseResponseCode;

import java.io.Serializable;

public class UserAssociationDataNotFoundException extends NotFoundException {
    public UserAssociationDataNotFoundException() {
        super(BaseResponseCode.USER_ASSOCIATION_DATA_NOT_FOUND);
    }

    public UserAssociationDataNotFoundException(Serializable id, String field) {
        super(BaseResponseCode.USER_ASSOCIATION_DATA_NOT_FOUND_ARGS, new Object[] {id, field});
    }
}
