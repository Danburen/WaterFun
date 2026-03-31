package org.waterwood.waterfunadminservice.api.request.perm;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class removePermUsersReq implements Serializable {
    @NotNull
    private List<Long> userUids;
}
