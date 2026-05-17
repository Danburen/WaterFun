package org.waterwood.waterfunservice.service.resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.waterwood.api.BaseResponseCode;
import org.waterwood.waterfunservice.api.response.MiniFileResData;
import org.waterwood.common.exceptions.BizException;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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
        String relativePath = "legal/" + type + "/" + lang + "/" + fileName;
        Resource resource = resourceLoader.getResource("classpath:" + relativePath);
        if (!resource.exists()) throw new BizException(BaseResponseCode.NOT_FOUND);

        String contentType = detectContentType(fileName);
        return new MiniFileResData(resource.getFile().toPath(), contentType);
    }

    public boolean isPathValid(String type, String lang) {
        return LegalResourceConstants.VALID_TYPES.contains(type)
                && LegalResourceConstants.VALID_LANGS.contains(lang);
    }

    public String getContent(String filePath, Charset charset) throws IOException {
        charset = charset == null ? StandardCharsets.UTF_8 : charset;
        return Files.readString(Paths.get(filePath).normalize(), charset);
    }

    public boolean exists(String filePath) {
        return Files.exists(Paths.get(filePath).normalize());
    }

    private String detectContentType(String fileName) throws IOException {
        return Optional.ofNullable(Files.probeContentType(Paths.get(fileName)))
                .orElse("text/plain");
    }

    public boolean isAllowedContentType(String fileName) throws IOException {
        String contentType = detectContentType(fileName);
        return allowedMimeTypes.contains(contentType);
    }
}
