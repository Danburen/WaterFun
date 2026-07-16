package org.waterwood.waterfunservicecore.entity.resource;

import lombok.Getter;

public enum SourceType {
    SYSTEM(0),
    CONTENT_ATTACHED(1),
    USER_UPLOADED(2),;

    @Getter
    private final byte value;

    SourceType(final int value) {
        this.value = (byte) value;
    }

    public static SourceType fromCode(final byte code) {
      for (final SourceType sourceType : SourceType.values()) {
          if (sourceType.value == code) {
              return sourceType;
          }
      }
      throw new IllegalArgumentException("No SourceType found for code " + code);
    }
}
