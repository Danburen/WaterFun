package org.waterwood.common;

/**
 * TokenResult is a record that holds the result of a value generation operation.
 * @param value generated value
 * @param expiresIn expiresIn time in <b>Seconds</b> (TTL)
 */
public record TokenResult(String value, Long expiresIn) {
    public TokenResult(){
        this(null, 0L);
    }

    public static TokenResult empty(){
        return new TokenResult();
    }
}
