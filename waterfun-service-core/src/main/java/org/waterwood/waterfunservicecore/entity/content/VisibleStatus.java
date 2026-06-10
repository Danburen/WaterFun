package org.waterwood.waterfunservicecore.entity.content;

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
      for (VisibleStatus visibleStatus : values()) {
            if (visibleStatus.code == code) {
                return visibleStatus;
            }
      }
      throw new IllegalArgumentException("Unknown visible status code: " + code);
    }
}
