package org.waterwood.waterfunservicecore.api.auth;

import org.waterwood.waterfunservicecore.entity.user.User;

public final record LoginResult(User user, boolean isNewUser) {
}
