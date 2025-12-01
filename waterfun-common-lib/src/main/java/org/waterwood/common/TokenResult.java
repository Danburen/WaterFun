package org.waterwood.common;

/**
 * TokenResult is a record that holds the result of a tokenValue generation operation.
 * @param tokenValue generated tokenValue
 * @param expire expire time in <b>Seconds</b> (TTL)
 */
public record TokenResult(String tokenValue, Long expire) {
}
