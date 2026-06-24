package org.waterwood.waterfunservicecore.api;

import org.waterwood.common.io.FileExtension;

import java.util.UUID;

public record UploadItem(
        int originalIndex,
        String path,
        String uuidPlain,
        UUID uuid,
        FileExtension ext
) {}