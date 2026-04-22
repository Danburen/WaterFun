package org.waterwood.waterfunservicecore.services.sys.storage.tencent;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.exception.CosServiceException;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.GetObjectRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.waterwood.api.BaseResponseCode;
import org.waterwood.common.exceptions.BizException;
import org.waterwood.common.exceptions.ServiceException;
import org.waterwood.common.io.FileMeta;
import org.waterwood.waterfunservicecore.services.sys.storage.CloudFileTypeDetector;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TencentFileTypeDetectService implements CloudFileTypeDetector {
    private final COSClient cosClient;
    @Value("${cloud.tencent.cos.bucket-name}")
    private String bucketName;

    private static final Map<String, List<byte[]>> MAGIC_NUMBERS = new HashMap<>();

    static {
        // JPEG: FF D8 FF
        MAGIC_NUMBERS.put("image/jpeg", List.of(
                new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}
        ));
        // PNG: 89 50 4E 47 0D 0A 1A 0A
        MAGIC_NUMBERS.put("image/png", List.of(
                new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}
        ));
        // GIF: GIF87a / GIF89a
        MAGIC_NUMBERS.put("image/gif", List.of(
                new byte[]{0x47, 0x49, 0x46, 0x38, 0x37, 0x61},  // GIF87a
                new byte[]{0x47, 0x49, 0x46, 0x38, 0x39, 0x61}   // GIF89a
        ));
        // WebP: RIFF....WEBP
        MAGIC_NUMBERS.put("image/webp", List.of(
                new byte[]{0x52, 0x49, 0x46, 0x46}  // 前4字节 RIFF，后4字节长度，8-11字节 WEBP
        ));
        // PDF: %PDF
        MAGIC_NUMBERS.put("application/pdf", List.of(
                new byte[]{0x25, 0x50, 0x44, 0x46}
        ));
        // ZIP/DOCX/XLSX: PK 0x03 0x04
        MAGIC_NUMBERS.put("application/zip", List.of(
                new byte[]{0x50, 0x4B, 0x03, 0x04}
        ));
        // EXE/DLL: MZ
        MAGIC_NUMBERS.put("application/x-msdownload", List.of(
                new byte[]{0x4D, 0x5A}
        ));
    }


    @Override
    public FileMeta detectByMagicNumber(String fullCloudFilePathKey) {
        GetObjectRequest rangeRequest = new GetObjectRequest(bucketName, fullCloudFilePathKey);
        rangeRequest.setRange(0, 15);  // bytes=0-15
        try (COSObject cosObject = cosClient.getObject(rangeRequest);
             InputStream is = cosObject.getObjectContent()) {
            long totalSize = cosObject.getObjectMetadata().getInstanceLength();
            byte[] header = is.readNBytes(16);
            return new FileMeta(
                    cosObject.getObjectMetadata().getETag(),
                    totalSize,
                    matchMagicNumber(header)
            );

        } catch (CosServiceException e) {
            if (e.getStatusCode() == 404) {
                throw new BizException(BaseResponseCode.CLOUD_FILE_NOT_FOUND, fullCloudFilePathKey);
            }
            throw new ServiceException("Failed to read COS object header: " + e.getMessage());
        } catch (IOException e) {
            throw new ServiceException("IO error reading COS object: " + e.getMessage());
        }
    }

    private String matchMagicNumber(byte[] header) {
        if (header.length < 2) {
            return "application/octet-stream";
        }

        for (Map.Entry<String, List<byte[]>> entry : MAGIC_NUMBERS.entrySet()) {
            for (byte[] magic : entry.getValue()) {
                if (isStartsWithMagicNumber(header, magic)) {
                    // WebP Specification：check byte ofPending 8-11 whether is "WEBP"
                    if ("image/webp".equals(entry.getKey())) {
                        if (header.length >= 12 &&
                                header[8] == 0x57 && header[9] == 0x45 &&
                                header[10] == 0x42 && header[11] == 0x50) {
                            return "image/webp";
                        }
                        continue;  // continue if is not WebP
                    }
                    return entry.getKey();
                }
            }
        }
        // Text file detection (simple heuristic)
        if (isTextFile(header)) {
            return "text/plain";
        }

        return "application/octet-stream";
    }

    private boolean isStartsWithMagicNumber(byte[] header, byte[] magic) {
        if (header.length < magic.length) return false;
        for (int i = 0; i < magic.length; i++) {
            if (header[i] != magic[i]) return false;
        }
        return true;
    }

    private boolean isTextFile(byte[] header) {
        int printable = 0;
        for (byte b : header) {
            if ((b >= 0x20 && b < 0x7F) || b == 0x0A || b == 0x0D || b == 0x09) {
                printable++;
            }
        }
        return printable > header.length * 0.8;
    }
}
