package org.waterwood.waterfunservicecore.infrastructure.utils.context;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public final class AuthContext implements Serializable {
    private Long userUid;
    private Set<String> roles = Collections.emptySet();
    private Set<String> permissions = Collections.emptySet();
    private String jti;
    private String did;

    public boolean isAdmin() {
        return this.roles != null && (this.roles.contains("admin") || this.roles.contains("ROLE_admin"));
    }

    public boolean hasPermission(String permission) {
        return this.permissions != null && this.permissions.contains(permission);
    }
}
