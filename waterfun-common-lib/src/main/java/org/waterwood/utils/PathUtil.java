package org.waterwood.utils;

import cn.hutool.core.date.DateUtil;

import java.util.UUID;

public final class PathUtil {
    public static String getUniquePathFile(String fileSuffix) {
        String uuid = UUID.randomUUID().toString();
        return DateUtil.today().replace("-", "/") + "/" + uuid + "." + fileSuffix;
    }
}
