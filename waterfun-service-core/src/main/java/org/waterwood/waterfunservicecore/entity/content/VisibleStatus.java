package org.waterwood.waterfunservicecore.entity.content;

import lombok.Getter;

@Getter
public enum VisibleStatus {
    SHOW(1),
    HIDE(2),;

    private final byte code;
    private VisibleStatus(int code) {
        this.code = (byte) code;
    }

    public static VisibleStatus fromCode(Byte code) {
      for (VisibleStatus visibleStatus : values()) {
            if (visibleStatus.code == code) {
                return visibleStatus;
            }
      }
      throw new IllegalArgumentException("Unknown visible status code: " + code);
    }
}
