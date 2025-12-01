package org.waterwood.api;

import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public interface ResponseCode {
    static int toHttpStatus(int code) {
        return (code >= 200 && code <= 600) ? code : code / 100;
    }

    ResponseCode toNoArgsResponse();

    int getCode();

    String getMsgKey();
}
