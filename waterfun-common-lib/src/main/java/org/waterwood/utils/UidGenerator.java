package org.waterwood.utils;

public interface UidGenerator {
    /**
     * Generate a uid
     * @return uid
     */
    Long generateUid();
    /**
     * Generate a uid
     *
     * @param length uid length
     * @return uid
     */
    Long generateUid(int length);
}
