package org.waterwood.waterfunservicecore.api;

import org.waterwood.waterfunservicecore.entity.audit.TargetType;

/**
 * Biz type
 * must be same as {@link org.waterwood.waterfunservicecore.entity.audit.TargetType}
 */
public interface BizType {
    String getCode();
//    TargetType getTargetType();
    static <T extends Enum<T> & BizType> T fromCode(Class<T> clazz, String code) {
        String lowerCaseCode = code.toLowerCase();
        for (T type : clazz.getEnumConstants()) {
            if (type.getCode().equals(lowerCaseCode)) {
                return type;
            }
        } // TODO REPLACE FOR BIZEXCEPTION
        throw new IllegalArgumentException("No " + clazz.getSimpleName() + " for code: " + code);
        }
}
