package org.waterwood.utils.generator;

import org.waterwood.common.jpa.SlugUniquenessChecker;

public interface SlugGenerator {
    /**
     * Generate a uniquifyed slug
     * @param raw raw string to generate slug
     * @param checker slug uniqueness that the target must implement.
     * @return slug
     */
    String generateSlug(String raw, SlugUniquenessChecker checker);

    /**
     * Uniquify a slug.
     * if slug is already exists, add a number to the end of slug
     * slug-2, slug-3...
     * @param base base slug
     * @param checker slug uniqueness that the target must implement.
     * @return uniquified slug
     */
    String uniquify(String base,SlugUniquenessChecker checker);
}
