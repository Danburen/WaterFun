package org.waterwood.waterfunservice.api.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;

/**
 * A Dto used for transport mini file content
 * Use {@link Base64#getMimeEncoder()} to encode content
 * usually file size less than 20KB
 */
@Data
public class MiniFileResData {
    private final String fileName;
    private final long fileSize;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private final LocalDateTime lastModified; // ISO 8601
    private final String fileType;
    private final String content;
    public MiniFileResData(Path filePath, String fileType) throws IOException {
        this.lastModified = LocalDateTime.ofInstant(
                Files.getLastModifiedTime(filePath).toInstant(),
                ZoneId.systemDefault()
        );
        this.fileType = fileType;
        this.content =  Base64.getMimeEncoder().encodeToString(Files.readAllBytes(filePath));
        this.fileSize = Files.size(filePath);
        this.fileName = filePath.getFileName().toString();
    }
}
