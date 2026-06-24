package org.waterwood.waterfunservicecore.entity.ticket;

import lombok.Getter;

@Getter
public enum TicketRejectType {
    NONE(0, "ticket.reject.none", false),
    INSUFFICIENT_EVIDENCE(1, "ticket.reject.insufficient_evidence", true),
    NO_VIOLATION(2, "ticket.reject.no_violation", true),
    DUPLICATE_REPORT(3, "ticket.reject.duplicate_report", true),
    MALICIOUS_REPORT(4, "ticket.reject.malicious_report", true),
    BEYOND_SCOPE(5, "ticket.reject.beyond_scope", true),
    FALSE_POSITIVE(6, "ticket.reject.false_positive", true),
    APPEAL_ACCEPTED(7, "ticket.reject.appeal_accepted", true),
    OTHER(99, "ticket.reject.other", true);

    private final short value;
    private final String messageKey;
    private final boolean terminal;
    private TicketRejectType(final int value, final String messageKey, final boolean terminal) {
        this.value = (short) value;
        this.messageKey = messageKey;
        this.terminal = terminal;
    }

    public static TicketRejectType fromValue(final int value) {
        for (final TicketRejectType type : TicketRejectType.values()) {
            if (type.value == value) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown TicketRejectType: " + value);
    }
}
