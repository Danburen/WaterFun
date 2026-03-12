package org.waterwood.waterfunservicecore.services.sys.storage;

public enum CloudResOperationType {
    READ("rd"),
    WRITE("wr"),
    DELETE("del");
    private final String key;
    CloudResOperationType(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
