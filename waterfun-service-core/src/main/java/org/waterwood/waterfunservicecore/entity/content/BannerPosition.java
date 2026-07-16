package org.waterwood.waterfunservicecore.entity.content;

import lombok.Getter;

@Getter
public enum BannerPosition {
    HOME(1),
    SIDE(2),;

    private final byte code;
    private BannerPosition(int code) {
        this.code = (byte) code;
    }

    public static BannerPosition fromCode(Byte code) {
      for (BannerPosition position : BannerPosition.values()) {
          if (position.code == code) {
              return position;
          }
      }
      throw new IllegalArgumentException("Invalid BannerPosition: " + code);
    }
}
