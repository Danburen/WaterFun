package org.waterwood.common.io;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
public enum FileExtension {
    // IMAGE
    JPG("jpg", "image/jpeg", ResourceType.IMAGE),
    JPEG("jpeg", "image/jpeg", ResourceType.IMAGE),
    PNG("png", "image/png", ResourceType.IMAGE),
    GIF("gif", "image/gif", ResourceType.IMAGE),
    WEBP("webp", "image/webp", ResourceType.IMAGE),
    BMP("bmp", "image/bmp", ResourceType.IMAGE),
    ICO("ico", "image/x-icon", ResourceType.IMAGE),
    TIFF("tiff", "image/tiff", ResourceType.IMAGE),
    TIF("tif", "image/tiff", ResourceType.IMAGE),
    HEIC("heic", "image/heic", ResourceType.IMAGE),
    HEIF("heif", "image/heif", ResourceType.IMAGE),
    RAW("raw", "image/x-raw", ResourceType.IMAGE),

    // VIDEO
    MP4("mp4", "video/mp4", ResourceType.VIDEO),
    AVI("avi", "video/x-msvideo", ResourceType.VIDEO),
    MKV("mkv", "video/x-matroska", ResourceType.VIDEO),
    MOV("mov", "video/quicktime", ResourceType.VIDEO),
    FLV("flv", "video/x-flv", ResourceType.VIDEO),
    WMV("wmv", "video/x-ms-wmv", ResourceType.VIDEO),
    WEBM("webm", "video/webm", ResourceType.VIDEO),
    M4V("m4v", "video/x-m4v", ResourceType.VIDEO),
    MPG("mpg", "video/mpeg", ResourceType.VIDEO),
    MPEG("mpeg", "video/mpeg", ResourceType.VIDEO),
    TS("ts", "video/mp2t", ResourceType.VIDEO),
    M3U8("m3u8", "application/vnd.apple.mpegurl", ResourceType.VIDEO),

    // AUDIO
    MP3("mp3", "audio/mpeg", ResourceType.AUDIO),
    WAV("wav", "audio/wav", ResourceType.AUDIO),
    FLAC("flac", "audio/flac", ResourceType.AUDIO),
    AAC("aac", "audio/aac", ResourceType.AUDIO),
    OGG("ogg", "audio/ogg", ResourceType.AUDIO),
    M4A("m4a", "audio/mp4", ResourceType.AUDIO),
    WMA("wma", "audio/x-ms-wma", ResourceType.AUDIO),
    OPUS("opus", "audio/opus", ResourceType.AUDIO),

    // TEXT
    TXT("txt", "text/plain", ResourceType.TEXT),
    MD("md", "text/markdown", ResourceType.TEXT),
    LOG("log", "text/plain", ResourceType.TEXT),
    JSON("json", "application/json", ResourceType.TEXT),
    XML("xml", "application/xml", ResourceType.TEXT),
    YAML("yaml", "application/x-yaml", ResourceType.TEXT),
    YML("yml", "application/x-yaml", ResourceType.TEXT),
    CSV("csv", "text/csv", ResourceType.TEXT),
    INI("ini", "text/plain", ResourceType.TEXT),
    CONF("conf", "text/plain", ResourceType.TEXT),
    PROPERTIES("properties", "text/plain", ResourceType.TEXT),
    SQL("sql", "text/plain", ResourceType.TEXT),
    CSS("css", "text/css", ResourceType.TEXT),

    // DOCUMENT
    PDF("pdf", "application/pdf", ResourceType.DOCUMENT),
    DOC("doc", "application/msword", ResourceType.DOCUMENT),
    DOCX("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", ResourceType.DOCUMENT),
    XLS("xls", "application/vnd.ms-excel", ResourceType.DOCUMENT),
    XLSX("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", ResourceType.DOCUMENT),
    PPT("ppt", "application/vnd.ms-powerpoint", ResourceType.DOCUMENT),
    PPTX("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation", ResourceType.DOCUMENT),
    ODT("odt", "application/vnd.oasis.opendocument.text", ResourceType.DOCUMENT),
    ODS("ods", "application/vnd.oasis.opendocument.spreadsheet", ResourceType.DOCUMENT),
    ODP("odp", "application/vnd.oasis.opendocument.presentation", ResourceType.DOCUMENT),
    RTF("rtf", "application/rtf", ResourceType.DOCUMENT),
    EPUB("epub", "application/epub+zip", ResourceType.DOCUMENT),

    // ARCHIVE
    ZIP("zip", "application/zip", ResourceType.ARCHIVE),
    RAR("rar", "application/vnd.rar", ResourceType.ARCHIVE),
    TAR("tar", "application/x-tar", ResourceType.ARCHIVE),
    GZ("gz", "application/gzip", ResourceType.ARCHIVE),
    TGZ("tgz", "application/gzip", ResourceType.ARCHIVE),
    BZ2("bz2", "application/x-bzip2", ResourceType.ARCHIVE),
    XZ("xz", "application/x-xz", ResourceType.ARCHIVE),
    SEVEN_Z("7z", "application/x-7z-compressed", ResourceType.ARCHIVE),

    UNKNOWN("unknown", "application/octet-stream", ResourceType.OTHER);

    private final String ext;
    private final String mimeType;
    private final ResourceType resourceType;

    public static FileExtension fromExt(String ext) {
        if (ext == null || ext.isBlank()) {
            return UNKNOWN;
        }
        String clean = ext.toLowerCase(Locale.ROOT).replaceFirst("^\\.", "");
        for (FileExtension fe : values()) {
            if (fe.ext.equals(clean)) {
                return fe;
            }
        }
        return UNKNOWN;
    }

    public static Set<String> extsOf(ResourceType type) {
        return Arrays.stream(values())
                .filter(fe -> fe.resourceType == type)
                .map(FileExtension::getExt)
                .collect(Collectors.toSet());
    }

    public static boolean isAllowed(String ext, ResourceType... allowedTypes) {
        FileExtension fe = fromExt(ext);
        if (fe == UNKNOWN) {
            return false;
        }
        Set<ResourceType> allowed = Set.of(allowedTypes);
        return allowed.contains(fe.getResourceType());
    }

    public static FileExtension fromMimeType(String mimeType) {
        if (mimeType == null || mimeType.isBlank()) return UNKNOWN;
        String normalized = mimeType.trim().toLowerCase(Locale.ROOT);

        for (FileExtension fe : values()) {
            if (fe.mimeType.equals(normalized)) return fe;
        }

        if (normalized.startsWith("image/")) return JPG;
        if (normalized.startsWith("video/")) return MP4;
        if (normalized.startsWith("audio/")) return MP3;
        if (normalized.startsWith("text/")) return TXT;

        return UNKNOWN;
    }

    public static ResourceType typeOfMime(String mimeType) {
        return fromMimeType(mimeType).getResourceType();
    }

    public static List<String> getAllowExtensionString(ResourceType resourceType) {
        return Arrays.stream(values())
                .filter(fe -> fe.getResourceType() == resourceType)
                .map(FileExtension::getExt)
                .toList();
    }

    public static List<FileExtension> getAllowExtensions(ResourceType resourceType) {
        return Arrays.stream(values())
                .filter(fe -> fe.getResourceType() == resourceType)
                .toList();
    }

    public static List<String> getAllowMimeTypes(ResourceType resourceType) {
        return Arrays.stream(values())
                .filter(fe -> fe.getResourceType() == resourceType)
                .map(FileExtension::getMimeType)
                .toList();
    }
}