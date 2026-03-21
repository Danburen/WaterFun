package org.waterwood.utils.generator;

import org.waterwood.common.constratin.UniquenessChecker;

public interface IdentifierGenerator {
    /**
     * Generate a unique slug
     * with separator of "-" and DownCase letter like "my-code"
     * @param raw raw string to generate slug
     * @param checker slug uniqueness that the target must implement.
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
}
