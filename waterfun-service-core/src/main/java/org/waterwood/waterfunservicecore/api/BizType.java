package org.waterwood.waterfunservicecore.api;

public interface BizType {
    String getCode();

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
