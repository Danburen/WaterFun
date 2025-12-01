package org.waterwood.api;

/**
 * A recorder contains the pair of AccessToken and RefreshToken with their expirations
 * @param accessToken Access Token
 * @param accessExp Expiration of Access Token in seconds
 * @param refreshToken Refresh Token
 * @param refreshExp Expiration of Refresh Token
 *
 * @version 1.0
 */
public record TokenPair(String accessToken, long accessExp, String refreshToken,long refreshExp) {
}
