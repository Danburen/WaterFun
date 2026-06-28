package org.waterwood.waterfunservice.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateNicknameRequest {

    @NotBlank(message = "{user.nickname.required}")
    @Size(min = 1, max = 30, message = "{user.nickname.length}")
    private String nickname;

}
