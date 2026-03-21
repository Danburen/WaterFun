package org.waterwood.common.jpa;

import org.waterwood.common.constratin.UniquenessChecker;
/**
 * JPA-specific uniqueness checker for entities with a "slug" field.
 * This interface is intended to be implemented by Spring Data JPA repositories
 * that support slug-based uniqueness checks.
 */
public interface SlugUniquenessChecker extends UniquenessChecker {
    boolean existsTagBySlug(String slug);

    default boolean exist(String value){
        return existsTagBySlug(value);
    }
}
