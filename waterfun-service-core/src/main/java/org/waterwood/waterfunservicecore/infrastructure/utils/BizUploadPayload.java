package org.waterwood.waterfunservicecore.infrastructure.utils;

import lombok.*;
import org.waterwood.waterfunservicecore.api.BizType;
import org.waterwood.waterfunservicecore.services.sys.upload.UploadContext;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BizUploadPayload {
    private String resourceUuid;
    private String bizType;
    private String bizId;
    private String cosKey;

    public static BizUploadPayload of(Serializable bizId, String bizType, UUID uuid) {
        return new BizUploadPayload(
                uuid.toString().replace("-", ""),
                bizType,
                bizId.toString().replace("-", ""),
                null
        );
    }

    public Map<String, String> toMap() {
        Map<String, String> map = new HashMap<>();
        if(resourceUuid != null) {
            map.put("resourceUuid", resourceUuid);
        }
        if (bizType != null) {
            map.put("bizType", bizType);
        }
        if (bizId != null) {
            map.put("bizId", bizId);
        }
        if (cosKey != null) {
            map.put("cosKey", cosKey);
        }
        return map;
    }

    public static BizUploadPayload fromMap(Map<String, String> map) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        BizUploadPayload payload = new BizUploadPayload();
        payload.setBizType(map.get("bizType"));
        payload.setBizId(map.get("bizId"));
        payload.setCosKey(map.get("cosKey"));
        payload.setResourceUuid(map.get("resourceUuid"));
        return payload;
    }

    /**
     * Converts this {@link BizUploadPayload} into a strongly-typed {@link UploadContext}
     * using the provided enum type, bizId type, and context supplier.
     *
     * <p>This method bridges the raw string-based payload (typically deserialized from
     * cloud storage callbacks or queue messages) back to a type-safe domain context
     * that carries both the business identifier and the typed business type enum.
     *
     * <p>Usage example:
     * <pre>{@code
     * UserUploadContext<Long> ctx = payload.toContext(
     *     UserBizType.class,
     *     Long.class,
     *     UserUploadContext::new
     * );
     * }</pre>
     *
     * @param <T>             the type of the business identifier (e.g., {@code Long}, {@code Integer}, {@code String})
     * @param <B>             the concrete enum type implementing {@link BizType}, used to resolve the business category
     * @param <C>             the concrete {@link UploadContext} subtype to instantiate
     * @param bizTypeClass    the {@code Class} object of the target enum type (e.g., {@code UserBizType.class})
     * @param IdType      the {@code Class} object of the target bizId type (e.g., {@code Long.class})
     * @param contextSupplier a supplier that provides a new empty instance of the target context type
     * @return a fully populated {@code C} instance with all fields mapped from this payload
     * @throws IllegalArgumentException if {@code bizType} code cannot be resolved in the given enum,
     *                                  or if {@code bizId} cannot be parsed into the requested {@code targetType}
     */
    public <T extends Serializable, B extends Enum<B> & BizType, C extends UploadContext<T, B>> C toContext(
            Class<B> bizTypeClass,
            Class<T> IdType,
            Supplier<C> contextSupplier) {

        C context = contextSupplier.get();
        context.setResourceUuid(this.resourceUuid);
        context.setCosKey(this.cosKey);
        context.setBizId(parseBizId(this.bizId, IdType));
        context.setBizType(BizType.fromCode(bizTypeClass, this.bizType));
        return context;
    }

    @SuppressWarnings("unchecked")
    private static <T> T parseBizId(String bizIdStr, Class<T> targetType) {
        if (bizIdStr == null || bizIdStr.isBlank()) return null;
        try {
            if (targetType == Long.class || targetType == long.class) {
                return (T) Long.valueOf(bizIdStr);
            } else if (targetType == Integer.class || targetType == int.class) {
                return (T) Integer.valueOf(bizIdStr);
            } else if (targetType == String.class) {
                return (T) bizIdStr;
            }
            throw new IllegalArgumentException("Unsupported type: " + targetType);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid bizId: " + bizIdStr);
        }
    }
}