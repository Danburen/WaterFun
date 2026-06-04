package org.waterwood.waterfunservicecore.infrastructure.validation;

import org.waterwood.common.io.FileExtension;
import org.waterwood.waterfunservicecore.entity.audit.TargetType;
import org.waterwood.waterfunservicecore.exception.io.FileTypeNotAllowException;
import org.waterwood.waterfunservicecore.exception.io.IllegalUploadCountException;
import org.waterwood.waterfunservicecore.services.sys.upload.UploadPolicy;

public class UploadValidator {

    public static FileExtension validateSingleFileUpload(UploadPolicy policy, TargetType type) {
        if(policy.getExts().size() != 1){
            throw new IllegalUploadCountException(1);
        }

        FileExtension ext = FileExtension.fromExt(policy.getExts().getFirst());
        if(! type.isAllowed(ext)){
            throw new FileTypeNotAllowException(
                    ext.getExt(),
                    type.getAllowedExts().stream()
                            .map(FileExtension::getExt).toList()
            );
        }
        return ext;
    }
}
