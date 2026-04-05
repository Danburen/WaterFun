package org.waterwood.utils;

import org.waterwood.api.VO.OptionVO;

import java.lang.reflect.Method;
import java.io.Serializable;
import java.util.Arrays;

public final class OptionConverter {
    public static <T, ID extends Serializable> OptionVO<ID> toOptionVO(T entity, Class<ID> idType) {
        if(entity == null) return null;
        OptionVO<ID> vo = new OptionVO<>();
        try {
            Class<?> clazz = entity.getClass();
            vo.setId(getFieldValue(entity, clazz, "id", idType));
            vo.setCode(getStringField(entity, clazz, "code"));
            vo.setName(getStringField(entity, clazz, "name"));
        } catch (Exception e) {
            return null;
        }
        return vo;
    }

    private static <ID extends Serializable> ID getFieldValue(Object entity, Class<?> clazz, String fieldName, Class<ID> type)
            throws Exception {
        Method method = findMethod(clazz, "get" + capitalize(fieldName));
        if (method == null) {
            method = findMethodBySuffix(clazz, "Id");
        }
        Object value = method != null ? method.invoke(entity) : null;
        return value == null ? null : type.cast(value);
    }

    private static String getStringField(Object entity, Class<?> clazz, String fieldName)
            throws Exception {
        Method method = findMethod(clazz, "get" + capitalize(fieldName));
        if (method == null) {
            method = findMethodBySuffix(clazz, capitalize(fieldName));
        }
        return method != null ? (String) method.invoke(entity) : null;
    }

    private static Method findMethodBySuffix(Class<?> clazz, String suffix) {
        return Arrays.stream(clazz.getMethods())
                .filter(m -> m.getName().startsWith("get") && m.getName().endsWith(suffix))
                .filter(m -> m.getParameterCount() == 0)
                .findFirst()
                .orElse(null);
    }

    private static String capitalize(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private static Method findMethod(Class<?> clazz, String methodName) {
        try {
            return clazz.getMethod(methodName);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }
}
