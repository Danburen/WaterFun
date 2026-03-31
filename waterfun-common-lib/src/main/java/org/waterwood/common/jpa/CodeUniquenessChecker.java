package org.waterwood.common.jpa;

import org.waterwood.common.constratin.UniquenessChecker;

public interface CodeUniquenessChecker extends UniquenessChecker {
    boolean existsByCode(String code);

    default boolean existsWithUniqueIdentify(String value){
        return existsByCode(value);
    }
}
