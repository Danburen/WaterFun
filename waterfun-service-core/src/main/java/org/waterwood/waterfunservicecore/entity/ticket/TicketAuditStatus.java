package org.waterwood.waterfunservicecore.entity.ticket;

import lombok.Getter;

@Getter
public enum TicketAuditStatus {
    PENDING(1),
    RESOLVED(2),
    REJECTED(3),
    CANCELLED(4);

    private final short value;
    TicketAuditStatus(final int value) {
        this.value = (short) value;
    }

    public static TicketAuditStatus fromValue(final int value) {
        return switch (value) {
            case 1 -> PENDING;
            case 2 -> RESOLVED;
            case 3 -> REJECTED;
            case 4 -> CANCELLED;
            default -> throw new IllegalArgumentException("Unknown TicketAuditStatus value: " + value);
        };
    }

    public boolean isTerminal() {
        return this == RESOLVED || this == REJECTED || this == CANCELLED;
    }
}
