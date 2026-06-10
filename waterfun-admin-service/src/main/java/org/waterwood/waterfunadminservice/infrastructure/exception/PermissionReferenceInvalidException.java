package org.waterwood.waterfunadminservice.infrastructure.exception;


import org.waterwood.api.BaseResponseCode;
import org.waterwood.waterfunservicecore.exception.reference.ReferenceInvalidException;

public class PermissionReferenceInvalidException extends ReferenceInvalidException {
    public PermissionReferenceInvalidException(String referenceName) {
        super(BaseResponseCode.PERMISSION_NOT_FOUND_ARGS, referenceName);
    }
}
