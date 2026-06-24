package org.waterwood.common;

public final class RabbitConstants {
    public static final String MODERATION_EXCHANGE = "moderation.direct";
    public static final String ROUTE_MODERATION_RESULT = "moderation.result";
    public static final  String ROUTE_MODERATION_BATCH_RESULT = "moderation.batch.result";
    public static final String ROUTE_TICKET_RESULT = "ticket.result";

    public static final String QUEUE_MODERATION_NOTIFICATION = "notification.moderation";
    public static final String QUEUE_TICKET_NOTIFICATION = "notification.ticket";

}
