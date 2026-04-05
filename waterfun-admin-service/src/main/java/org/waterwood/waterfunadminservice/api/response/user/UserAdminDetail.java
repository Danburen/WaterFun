package org.waterwood.waterfunadminservice.api.response.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.waterwood.api.VO.ExpirableOptionVO;
import org.waterwood.waterfunservicecore.api.resp.AccountResp;

import java.io.Serializable;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserAdminDetail implements Serializable {
    private UserInfoARes info;
    private UserProfileRes profile;
    private UserCounterARes counter;
    private AccountResp maskedData;
    private Set<ExpirableOptionVO<Integer>> roles;
    private Set<ExpirableOptionVO<Integer>> permissions;
}
