package org.waterwood.common.io;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
public enum FileExtension {
    // ========== IMAGE ==========
    JPG("jpg", ResourceType.IMAGE),
    JPEG("jpeg", ResourceType.IMAGE),
    PNG("png", ResourceType.IMAGE),
    GIF("gif", ResourceType.IMAGE),
    WEBP("webp", ResourceType.IMAGE),
    BMP("bmp", ResourceType.IMAGE),
    ICO("ico", ResourceType.IMAGE),
    TIFF("tiff", ResourceType.IMAGE),
    TIF("tif", ResourceType.IMAGE),
    HEIC("heic", ResourceType.IMAGE),
    HEIF("heif", ResourceType.IMAGE),
    RAW("raw", ResourceType.IMAGE),

    // ========== VIDEO ==========
    MP4("mp4", ResourceType.VIDEO),
    AVI("avi", ResourceType.VIDEO),
    MKV("mkv", ResourceType.VIDEO),
    MOV("mov", ResourceType.VIDEO),
    FLV("flv", ResourceType.VIDEO),
    WMV("wmv", ResourceType.VIDEO),
    WEBM("webm", ResourceType.VIDEO),
    M4V("m4v", ResourceType.VIDEO),
    MPG("mpg", ResourceType.VIDEO),
    MPEG("mpeg", ResourceType.VIDEO),
    TS("ts", ResourceType.VIDEO),
    M3U8("m3u8", ResourceType.VIDEO),

    // ========== AUDIO ==========
    MP3("mp3", ResourceType.AUDIO),
    WAV("wav", ResourceType.AUDIO),
    FLAC("flac", ResourceType.AUDIO),
    AAC("aac", ResourceType.AUDIO),
    OGG("ogg", ResourceType.AUDIO),
    M4A("m4a", ResourceType.AUDIO),
    WMA("wma", ResourceType.AUDIO),
    OPUS("opus", ResourceType.AUDIO),

    // ========== TEXT ==========
    TXT("txt", ResourceType.TEXT),
    MD("md", ResourceType.TEXT),
    LOG("log", ResourceType.TEXT),
    JSON("json", ResourceType.TEXT),
    XML("xml", ResourceType.TEXT),
    YAML("yaml", ResourceType.TEXT),
    YML("yml", ResourceType.TEXT),
    CSV("csv", ResourceType.TEXT),
    INI("ini", ResourceType.TEXT),
    CONF("conf", ResourceType.TEXT),
    PROPERTIES("properties", ResourceType.TEXT),
    SQL("sql", ResourceType.TEXT),
    CSS("css", ResourceType.TEXT),

    // ========== DOCUMENT ==========
    PDF("pdf", ResourceType.DOCUMENT),
    DOC("doc", ResourceType.DOCUMENT),
    DOCX("docx", ResourceType.DOCUMENT),
    XLS("xls", ResourceType.DOCUMENT),
    XLSX("xlsx", ResourceType.DOCUMENT),
    PPT("ppt", ResourceType.DOCUMENT),
    PPTX("pptx", ResourceType.DOCUMENT),
    ODT("odt", ResourceType.DOCUMENT),
    ODS("ods", ResourceType.DOCUMENT),
    ODP("odp", ResourceType.DOCUMENT),
    RTF("rtf", ResourceType.DOCUMENT),
    EPUB("epub", ResourceType.DOCUMENT),

    // ========== ARCHIVE ==========
    ZIP("zip", ResourceType.ARCHIVE),
    RAR("rar", ResourceType.ARCHIVE),
    TAR("tar", ResourceType.ARCHIVE),
    GZ("gz", ResourceType.ARCHIVE),
    TGZ("tgz", ResourceType.ARCHIVE),
    BZ2("bz2", ResourceType.ARCHIVE),
    XZ("xz", ResourceType.ARCHIVE),
    SEVEN_Z("7z", ResourceType.ARCHIVE),

    // ========== OTHER ==========
    UNKNOWN("unknown", ResourceType.OTHER);

    private final String ext;
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

}