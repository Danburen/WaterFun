package org.waterwood.waterfunservicecore.entity;

import lombok.Getter;

@Getter
public enum BannerPosition {
    HOME(1),
    SIDE(2),;

    private final short code;
    private BannerPosition(int code) {
        this.code = (short) code;
    }

    public static BannerPosition fromCode(short code) {
      for (BannerPosition position : BannerPosition.values()) {
          if (position.code == code) {
              return position;
          }
      }
      throw new IllegalArgumentException("Invalid BannerPosition: " + code);
    }
}
