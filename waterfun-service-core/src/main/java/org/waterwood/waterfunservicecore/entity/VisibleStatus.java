package org.waterwood.waterfunservicecore.entity;

import lombok.Getter;

@Getter
public enum VisibleStatus {
    SHOW(1),
    HIDE(2),;

    private final short code;
    private VisibleStatus(int code) {
        this.code = (short) code;
    }

    public static VisibleStatus fromCode(short code) {
       return switch (code) {
           case 1 -> SHOW;
           case 2 -> HIDE;
           default -> throw new IllegalArgumentException("Unknown visible status code: " + code);
       };
    }
}
