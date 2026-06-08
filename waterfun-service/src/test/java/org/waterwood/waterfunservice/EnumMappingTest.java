package org.waterwood.waterfunservice;

import org.junit.jupiter.api.Test;
import org.waterwood.waterfunservice.service.moderation.ModerationTargetType;
import org.waterwood.waterfunservicecore.entity.audit.TargetType;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class EnumMappingTest {
    @Test
    public void testAllTargetTypeHaveModerationMapping() {
        for (TargetType type : TargetType.values()) {
            if (type == TargetType.UNKNOWN) continue;
            ModerationTargetType mt = ModerationTargetType.fromTargetType(type);
            assertNotNull(mt, "TargetType." + type.name() + " missing ModerationTargetType mapping");
        }
    }

}