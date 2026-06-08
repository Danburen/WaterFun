package org.waterwood.waterfunservice.infrastructure.dto;

import org.waterwood.api.Mappable;

public interface InboxPayload extends Mappable<Object> {
    String getNativeUrl();
}
