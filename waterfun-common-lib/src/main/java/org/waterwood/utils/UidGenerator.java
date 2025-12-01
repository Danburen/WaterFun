package org.waterwood.utils;

public interface UidGenerator {
    /**
     * Generate a uid
     * @return uid
     */
    String generateUid();
    /**
     * Generate a uid
     * @param length uid length
     * @return uid
     */
    String generateUid(int length);
}
