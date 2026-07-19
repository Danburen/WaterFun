package org.waterwood.waterfunservice.service.resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.waterwood.api.BaseResponseCode;
import org.waterwood.waterfunservice.api.response.MiniFileResData;
import org.waterwood.waterfunservicecore.exception.BizException;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Set;

@Service
public class ResourceService {
    @Autowired
    ResourceLoader resourceLoader;

    private final Set<String> allowedMimeTypes = Set.of("text/plain", "text/html");


    public Resource loadResource(String filename) {
        return new ClassPathResource(filename);
    }

    public String getContent(String filePath) throws IOException {
        return getContent(filePath, StandardCharsets.UTF_8);
    }

    public MiniFileResData getLegalFileContent(String type, String lang, String fileName) throws IOException {
        // 优先尝试精确文件名，不满足则尝试扩展名 fallback（.md ↔ .txt）
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

        // 安全校验：确保解析后的文件仍在 legal/ 目录下（防路径穿越）
        Path resolvedPath = resource.getFile().toPath().toRealPath();
        String resolved = resolvedPath.toString().replace('\\', '/');
        if (!resolved.contains("/legal/") || !resolved.endsWith("/" + fileName)) {
            throw new BizException(BaseResponseCode.INVALID_PATH);
        }

        String contentType = detectContentType(resolved);
        return new MiniFileResData(resolvedPath, contentType);
    }

    public boolean isPathValid(String type, String lang) {
        return LegalResourceConstants.VALID_TYPES.contains(type)
                && LegalResourceConstants.VALID_LANGS.contains(lang);
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
