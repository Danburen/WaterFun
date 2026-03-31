package org.waterwood.utils.generator;

import org.jetbrains.annotations.NotNull;
import org.waterwood.common.constratin.UniquenessChecker;

public interface IdentifierGenerator {
    /**
     * Generate a unique slug
     * with separator of "-" and DownCase letter like "my-code"
     * @param raw raw string to generate slug
     * @param checker code uniqueness that the target must implement.
     * @return slug
     */
    String generateSlug(String raw, UniquenessChecker checker);
    /**
     * Generate a unique code
     * with separator of "_" and UpCase letter like "MY_CODE"
     * @param raw raw string to generate slug
     * @param checker slug uniqueness that the target must implement.
     * @return slug
     */
    String generateCode(String raw, UniquenessChecker checker);
    /**
     * Uniquify a slug.
     * if slug is already exists, add a number to the end of slug
     * slug-2, slug-3...
     * @param base base slug
     * @param checker slug uniqueness that the target must implement.
     * @return uniquified slug
     */
    String uniquify(String base, UniquenessChecker checker);

    /**
     * Check whether present code is existing, if exists,
     * we use fallback to generate a new code, and check again until we get a unique code.
     * <p>with separator of "_" and UpCase letter like "MY_CODE"</p>
     * if code is empty or null, we directly use fallback.
     * @param code source code
     * @param fallback fallback code
     * @param checker code uniqueness that the target must implement.
     * @return uniquified code
     */
    String fromCode(String code, @NotNull String fallback, UniquenessChecker checker);
    /**
     * Check whether present slug is existing, if exists,
     * we use fallback to generate a new code, and check again until we get a unique code.
     * <p>with separator of "-" and DownCase letter like "my-code"</p>
     * <p>if code is empty or null, we directly use fallback.</p>
     * @param slug source slug
     * @param fallback fallback slug
     * @param checker cslug uniqueness that the target must implement.
     * @return uniquified slug
     */
    String fromSlug(String slug,@NotNull String fallback, UniquenessChecker checker);
}
