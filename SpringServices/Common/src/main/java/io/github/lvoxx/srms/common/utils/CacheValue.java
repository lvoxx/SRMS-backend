package io.github.lvoxx.srms.common.utils;

import lombok.experimental.FieldNameConstants;

@FieldNameConstants(onlyExplicitlyIncluded = true)
public enum CacheValue {
    @FieldNameConstants.Include
    CUSTOMERS,
    @FieldNameConstants.Include
    CONTACTOR,
    @FieldNameConstants.Include
    CONTACTOR_PAGE,
    @FieldNameConstants.Include
    CONTACTOR_TYPE,
    @FieldNameConstants.Include
    CONTACTOR_SEARCH,
    @FieldNameConstants.Include
    CONTACTOR_EMAIL
}
