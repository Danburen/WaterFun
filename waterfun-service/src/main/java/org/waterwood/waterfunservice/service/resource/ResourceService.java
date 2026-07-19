package org.waterwood.waterfunservice.service.resource;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.waterwood.api.BaseResponseCode;
import org.waterwood.waterfunservice.api.response.MiniFileResData;
import org.waterwood.waterfunservicecore.exception.BizException;
import org.waterwood.waterfunservicecore.exception.io.FilePathInvalidException;
import org.waterwood.waterfunservicecore.exception.notfound.ResourceNotFoundException;
import org.waterwood.waterfunservicecore.infrastructure.utils.context.UserCtxHolder;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Set;

@Service
@Slf4j
public class ResourceService {
    @Autowired
    ResourceLoader resourceLoader;

    private final Set<String> allowedMimeTypes = Set.of("text/plain", "text/html");

    @Value("${app.resources.path:./deploy/resource}")
    private String resourceBasePath;

    private Path resolvedBasePath;

    @PostConstruct
    public void init() {
        this.resolvedBasePath = Paths.get(resourceBasePath).toAbsolutePath().normalize();
    }

    // ────────────── New: Flat resource file reader ──────────────

    /**
     * Read a resource file from the external config path (deploy/resource/ mounted volume).
     * <p>
     * Includes: filename validation, protected-file auth check, path traversal prevention.
     * Exceptions are handled by {@link org.waterwood.waterfunservice.infrastructure.GlobalExceptionHandler}.
     *
     * @param fileName e.g. "contact.md", "about_en_US.md"
     * @return MiniFileResData
     */
    public MiniFileResData getResourceFile(String fileName) {
        // Filename safety: only allow [a-zA-Z0-9_-] + '.' + extension
        if (fileName == null || !fileName.matches("^[\\w\\-]+\\.[a-zA-Z]+$")) {
            throw new FilePathInvalidException();
        }

        // Protected files require authentication
        if (LegalResourceConstants.PROTECTED_FILES.contains(fileName)
                && UserCtxHolder.getUserUid() == null) {
            throw new BizException(BaseResponseCode.HTTP_UNAUTHORIZED, HttpStatus.UNAUTHORIZED);
        }

        // Path traversal prevention
        Path fullPath = resolvedBasePath.resolve(fileName).normalize();
        if (!fullPath.startsWith(resolvedBasePath)) {
            throw new FilePathInvalidException();
        }

        // File existence check
        if (!Files.exists(fullPath) || Files.isDirectory(fullPath)) {
            throw new ResourceNotFoundException();
        }

        try {
            String contentType = detectContentType(fullPath.toString());
            return new MiniFileResData(fullPath, contentType);
        } catch (IOException e) {
            log.error("Failed to read resource file: {}", fileName, e);
            throw new BizException(BaseResponseCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ────────────── Old: classpath legal resource reader (deprecated, kept for compat) ──────────────

    public Resource loadResource(String filename) {
        return new ClassPathResource(filename);
    }

    public String getContent(String filePath) throws IOException {
        return getContent(filePath, StandardCharsets.UTF_8);
    }

    public MiniFileResData getLegalFileContent(String type, String lang, String fileName) throws IOException {
        // Try exact fileName first, then fallback (.md ↔ .txt)
        String relativePath = "legal/" + type + "/" + lang + "/" + fileName;
        Resource resource = resourceLoader.getResource("classpath:" + relativePath);
        if (!resource.exists()) {
            String altName = fileName.endsWith(".md")
                    ? fileName.replace(".md", ".txt")
                    : fileName.replace(".txt", ".md");
            String altPath = "legal/" + type + "/" + lang + "/" + altName;
            Resource altResource = resourceLoader.getResource("classpath:" + altPath);
            if (altResource.exists()) {
                resource = altResource;
            } else {
                throw new BizException(BaseResponseCode.NOT_FOUND);
            }
        }

        // Safety: ensure resolved file stays under legal/ (path traversal prevention)
        Path resolvedPath = resource.getFile().toPath().toRealPath();
        String resolved = resolvedPath.toString().replace('\\', '/');
        if (!resolved.contains("/legal/") || !resolved.endsWith("/" + fileName)) {
            throw new BizException(BaseResponseCode.INVALID_PATH);
        }

        String contentType = detectContentType(resolved);
        return new MiniFileResData(resolvedPath, contentType);
    }

    public boolean isPathValid(String type, String lang) {
        return false; // deprecated, old type/lang validation is no longer valid
    }

    public String getContent(String filePath, Charset charset) throws IOException {
        charset = charset == null ? StandardCharsets.UTF_8 : charset;
        Path path = Paths.get(filePath).normalize();
        return Files.readString(path, charset);
    }

    public boolean exists(String filePath) {
        return Files.exists(Paths.get(filePath).normalize());
    }

    private String detectContentType(String filePath) throws IOException {
        return Optional.ofNullable(Files.probeContentType(Paths.get(filePath)))
                .orElse("text/plain");
    }

    public boolean isAllowedContentType(String fileName) throws IOException {
        String contentType = detectContentType(fileName);
        return allowedMimeTypes.contains(contentType);
    }
}
