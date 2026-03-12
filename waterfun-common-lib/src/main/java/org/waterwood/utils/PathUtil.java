package org.waterwood.utils;

import cn.hutool.core.date.DateUtil;

import java.io.Serializable;
import java.util.Arrays;
import java.util.UUID;

public final class PathUtil {
    public static String getUniquePathFile(String fileSuffix) {
        String uuid = UUID.randomUUID().toString();
        return DateUtil.today().replace("-", "/") + "/" + uuid + "." + fileSuffix;
    }

    public static String buildPath(Serializable... segments){
        return String.join("/", Arrays.toString(segments));
    }
    public static String getFilenameWithNoSuffix(String path){
        return path.substring(path.lastIndexOf("/") + 1, path.lastIndexOf("."));
    }
}
