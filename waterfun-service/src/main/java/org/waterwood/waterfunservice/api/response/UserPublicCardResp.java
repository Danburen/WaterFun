package org.waterwood.waterfunservice.api.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPublicCardResp implements Serializable {
    private Long userUid;
    private String username;
    private String nickname;
    private Byte level;
    private Short visible;

    public UserPublicCardResp(short visible) {
        this.visible = visible;
    }
}
