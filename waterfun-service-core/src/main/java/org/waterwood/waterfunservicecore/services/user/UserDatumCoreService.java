package org.waterwood.waterfunservicecore.services.user;

import org.jetbrains.annotations.Nullable;
import org.waterwood.waterfunservicecore.entity.user.UserDatum;

public interface UserDatumCoreService {
    UserDatum getUserDatum(long userUid);

    /**
     * Save new email to repo
     * @param userUid target user id
     * @param email new email
     */
    UserDatum saveNewEmailVerified(long userUid, String email);

    /**
     * Save new phone to repo with verified
     * @param userUid target user id
     * @param phone new phone
     * @return UserDatum
     */
    UserDatum saveNewPhoneVerified(long userUid, String phone);
    String getRawPhone(long userUid);
    @Nullable String getRawEmail(long userUid);
}
