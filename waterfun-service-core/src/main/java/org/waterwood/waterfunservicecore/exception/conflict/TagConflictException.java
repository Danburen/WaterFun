package org.waterwood.waterfunservicecore.exception.conflict;

import org.waterwood.api.BaseResponseCode;

public class TagConflictException extends ConflictException{
    public TagConflictException(){
        super(BaseResponseCode.POST_TAG_CONFLICT);
    }
}
