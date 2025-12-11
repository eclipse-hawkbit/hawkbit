/**
 * Copyright (c) 2021 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.repository;

import org.eclipse.hawkbit.repository.model.BaseEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.domain.Specification;
import org.jspecify.annotations.Nullable;

/**
 * Repository interface that offers findAll with disabled count query.
 *
 * @param <T> entity type
 */
public interface NoCountSliceRepository<T> {

    /**
     * Retrieves a slice of {@link BaseEntity}s without keeping the count updated
     *
     * @param pageable page to keep track of slices
     * @return {@link BaseEntity}
     */
    Slice<T> findAllWithoutCount(Pageable pageable);

    /**
     * Retrieves a slice of {@link BaseEntity}s based on spec without keeping the
     * count updated
     *
     * @param spec to search for
     * @param pageable page to keep track of slices
     * @return {@link BaseEntity}
     */
    Slice<T> findAllWithoutCount(@Nullable Specification<T> spec, Pageable pageable);
}
