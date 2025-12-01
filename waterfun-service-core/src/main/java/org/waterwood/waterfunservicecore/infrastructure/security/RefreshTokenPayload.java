package org.waterwood.waterfunservicecore.infrastructure.security;

public record RefreshTokenPayload(long userId,String deviceId) {
}
