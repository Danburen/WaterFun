package org.waterwood.waterfunservicecore.entity.ticket;

import lombok.Getter;

@Getter
public enum TicketType {
    CONTENT_REPORT(1),
    ACCOUNT_APPEAL(2),
    FEATURE_FEEDBACK(3),
    SUGGESTION(4),;

    private final short value;

    TicketType(final int value) {
        this.value = (short) value;
    }

    public static TicketType fromValue(final int value) {
        for (final TicketType ticketType : TicketType.values()) {
            if (ticketType.value == value) {
                return ticketType;
            }
        }
        throw new IllegalArgumentException("Unknown ticket type: " + value);
    }
}
