package org.waterwood.common.jpa;

import org.waterwood.common.constratin.UniquenessChecker;

public interface CodeUniquenessChecker extends UniquenessChecker {
    boolean existsTagByCode(String slug);

    default boolean exist(String value){
        return existsTagByCode(value);
    }
}
