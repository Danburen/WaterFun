package org.waterwood.utils;

import cn.hutool.core.date.DateUtil;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public final class PathUtil {
    public static String getUniquePathFile(String fileSuffix) {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return DateUtil.today().replace("-", "/") + "/" + uuid + "." + fileSuffix;
    }

    public static String buildPath(Serializable... segments){
        if (segments == null || segments.length == 0) {
            return "";
        }

        List<String> flattened = new ArrayList<>();
        for (Serializable seg : segments) {
            if (seg == null) {
                flattened.add("");
            } else if (seg.getClass().isArray()) { // flat map array segments
                int len = Array.getLength(seg);
                for (int i = 0; i < len; i++) {
                    Object item = Array.get(seg, i);
                    flattened.add(item != null ? item.toString() : "");
                }
            } else {
                flattened.add(seg.toString());
            }
        }

        return String.join("/", flattened);
    }
    public static String getFilenameWithNoSuffix(String path){
        return path.substring(path.lastIndexOf("/") + 1, path.lastIndexOf("."));
    }

    public static String getSuffix(String fullKeyPath) {
        return fullKeyPath.substring(fullKeyPath.lastIndexOf(".") + 1);
    }
}
