package org.waterwood.waterfunservice.service.authServices;

public interface VerifyCodeService {
    /**
     * Save verify code to repository
     * @param uuid uuid of the code
     * @param code string code
     */
    void saveCode(String uuid,String code);
    /**
     * Get verify code from repository
     * @param uuid uuid of code
     * @return the verify code
     */
    String getCode(String uuid);
    /**
     * Remove and destroy the code from repository
     * @param uuid uuid of code
     */
    void removeCode(String uuid);
    /**
     * Generate the verify code
     * @return verify code
     */
    Object generateVerifyCode();
}
