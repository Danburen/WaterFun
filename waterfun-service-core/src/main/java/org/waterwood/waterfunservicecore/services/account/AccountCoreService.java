package org.waterwood.waterfunservicecore.services.account;

import org.jetbrains.annotations.Nullable;
import org.waterwood.common.TokenResult;
import org.waterwood.waterfunservicecore.api.resp.auth.CodeResult;

/**
 * Core account management service.
 * <p>Handles password/email/phone operations, distinct from login/auth semantics.</p>
 */
public interface AccountCoreService {

    /**
     * Update password (authenticated via reAuthToken).
     */
    void changePwd(long userUid, String newPwd, String confirmPwd);

    /**
     * Set password for users who don't have one yet (authenticated via reAuthToken).
     */
    void setPassword(long userUid, String newPwd, String confirmPwd);

    /**
     * Activate email binding (authenticated via reAuthToken).
     */
    void activateEmail(long userUid, String email);

    /**
     * Change email — saves as unverified, sends activation code to the new email.
     */
    CodeResult changeEmail(long userUid, String newEmail);

    /**
     * Verify the activation code for a pending email change and mark the email as verified.
     */
    void verifyChangeEmail(String verifyKey, String code, long userUid);

    /**
     * Clean up unverified email records whose expiry has passed.
     */
    void cleanUnverifiedEmail();

    /**
     * Change phone number (authenticated via reAuthToken).
     * Saves new phone as unverified and sends activate code.
     */
    CodeResult changePhone(long userUid, String newPhone);

    /**
     * Verify the activation code for a pending phone change and mark the phone as verified.
     */
    void verifyChangePhone(String verifyKey, String code, long userUid);

    /**
     * Activate phone number (authenticated via reAuthToken).
     */
    void activatePhone(long userUid, String phone);

    /**
     * Unbind email (authenticated via reAuthToken).
     */
    void unbindEmail(long userUid, String email);

    /**
     * Reset password using a reAuthToken (consumed one-time).
     * Called after the forgot-password SMS verification flow.
     * Records {@link org.waterwood.waterfunservicecore.entity.audit.AuditLogActionType#FORGOT_PASSWORD}.
     */
    void resetPasswordByToken(Long userUid, String newPwd, String confirmPwd);

    /**
     * Step 1: Initiate forgot-password re-authentication.
     * <p>Resolves identifier → uid → bound phone, sends SMS code,
     * stores the phone in Redis key {@code op:verify:context:fp:{verifyKey}} for later verification.</p>
     *
     * @param identifier phone/email/username
     * @return reAuthKey (the SMS verify key), or {@code null} if no bound phone found
     */
    @Nullable
    String initiateForgotPasswordReAuth(String identifier);

    /**
     * Step 2: Verify forgot-password SMS code and generate a one-time token.
     * <p>Reads the phone from Redis key {@code op:verify:context:fp:{reAuthKey}} (not deleted),
     * {@link org.waterwood.waterfunservicecore.services.auth.code.VerificationService#verifyCode verifies}
     * the code, then deletes the Redis key only on success — allowing retry on wrong code.</p>
     *
     * @param reAuthKey the verify key returned from step 1
     * @param code      the SMS code from the user
     * @return {@link TokenResult} containing the one-time token, or {@code null} if expired
     */
    @Nullable
    TokenResult verifyForgotPasswordReAuth(String reAuthKey, String code);
}
