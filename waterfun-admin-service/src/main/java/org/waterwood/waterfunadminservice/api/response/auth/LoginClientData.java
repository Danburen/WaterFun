package org.waterwood.waterfunadminservice.api.response.auth;

import lombok.Value;

import java.io.Serializable;

@Value
public class LoginClientData implements Serializable {
    String accessToken;
    Long exp;
}
