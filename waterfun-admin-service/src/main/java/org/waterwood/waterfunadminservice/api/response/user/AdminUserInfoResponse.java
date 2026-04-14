package org.waterwood.waterfunadminservice.api.response.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.waterwood.waterfunservicecore.api.resp.user.UserInfoResponse;

import java.io.Serializable;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
public class AdminUserInfoResponse extends UserInfoResponse implements Serializable {
    private List<String> roles;
    private List<String> permissions;
    private Boolean isAdmin;
}
