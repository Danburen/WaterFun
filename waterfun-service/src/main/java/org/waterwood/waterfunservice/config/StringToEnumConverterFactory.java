package org.waterwood.waterfunservice.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.stereotype.Component;

@Component
public class StringToEnumConverterFactory implements ConverterFactory<String, Enum> {

    @Override
    public <T extends Enum> Converter<String, T> getConverter(Class<T> targetType) {
        return new StringToEnumConverter<>(targetType);
    }

    private static class StringToEnumConverter<T extends Enum> implements Converter<String, T> {
        private final Class<T> enumType;

        public StringToEnumConverter(Class<T> enumType) {
            this.enumType = enumType;
        }

        @Override
        public T convert(String source) {
            if (source == null || source.isEmpty()) {
                return null;
            }
            try {
                @SuppressWarnings("unchecked")
                T result = (T) Enum.valueOf(enumType, source.toUpperCase());
                return result;
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        "无效的枚举值 '" + source + "'，期望的类型: " + enumType.getSimpleName()
                );
            }
        }
    }
}
