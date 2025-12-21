package org.waterwood.waterfunservicecore.infrastructure.security;

public record RefreshTokenPayload(long userUid,String deviceId) {
}
