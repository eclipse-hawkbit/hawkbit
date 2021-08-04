/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import org.eclipse.hawkbit.repository.model.BaseEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.Nullable;

/**
 * Repository interface that offers findAll with disabled count query.
 *
 * @param <T>
 *            entity type
 */
public interface NoCountSliceRepository<T> {

    /**
     * Retrieves a slice of {@link BaseEntity}s without keeping the count updated
     *
     * @param pageable
     *            page to keep track of slices
     * @return {@link BaseEntity}
     */
    Slice<T> findAllWithoutCount(Pageable pageable);

    /**
     * Retrieves a slice of {@link BaseEntity}s based on spec without keeping the
     * count updated
     *
     * @param spec
     *            to search for
     * @param pageable
     *            page to keep track of slices
     * @return {@link BaseEntity}
     */
    Slice<T> findAllWithoutCount(@Nullable Specification<T> spec, Pageable pageable);
}
