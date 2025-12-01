package org.waterwood.utils.codec;

import java.math.BigInteger;

public final class Base62 {
    private static final char[] DIGITS =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();
    private static final BigInteger BASE = BigInteger.valueOf(62);

    public static String encode(long num) {
        BigInteger val = BigInteger.valueOf(num);
        StringBuilder sb = new StringBuilder();
        while (val.compareTo(BigInteger.ZERO) > 0) {
            sb.insert(0, DIGITS[val.mod(BASE).intValue()]);
            val = val.divide(BASE);
        }
        return sb.isEmpty() ? "0" : sb.toString();
    }
}
