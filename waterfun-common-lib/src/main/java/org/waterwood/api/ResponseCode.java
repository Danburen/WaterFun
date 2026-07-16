package org.waterwood.api;

public interface ResponseCode {
    static int toHttpStatus(int code) {
        return (code >= 200 && code <= 600) ? code : code / 100;
    }

    ResponseCode toNoArgsResponse();

    String getCode();
}
