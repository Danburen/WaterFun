package org.waterwood.waterfunservicecore.infrastructure.utils;

import org.waterwood.utils.codec.Snowflake;

public final class IdGenerator {
    private static final long DETACENTER_CONTENT = 1L;

    private static final long WORKER_POST = 0L;
    private static final long WORKER_COMMENT = 1L;
    private static final long WORKER_AUDIT_TASK = 2L;
    private static final long WORKER_REPORT = 3L;
    private static final long WORKER_TICKET = 4L;

    private static final Snowflake postGen = Snowflake.of(WORKER_POST, DETACENTER_CONTENT);
    private static final Snowflake commentIdGen = Snowflake.of(WORKER_COMMENT, DETACENTER_CONTENT);
    private static final Snowflake auditTaskIdGen = Snowflake.of(WORKER_AUDIT_TASK, DETACENTER_CONTENT);
    private static final Snowflake ticketIdGen = Snowflake.of(WORKER_TICKET, DETACENTER_CONTENT);

    public static long nextPostId(){
        return postGen.nextId();
    };

    public static long nextCommentId() {
        return commentIdGen.nextId();
    }

    public static long generateAuditTaskId() {
        return auditTaskIdGen.nextId();
    }

    public static long generateTicketId() {
        return ticketIdGen.nextId();
    }
}
