/**
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.jspecify.annotations.Nullable;

/**
 * Repository interface that offers JpaSpecificationExecutor#findOne/All methods with entity graph loading
 *
 * @param <T> entity type
 */
public interface JpaSpecificationEntityGraphExecutor<T> {

    /**
     * Returns a single entity matching the given {@link Specification} with the entity graph hint or {@link Optional#empty()} if none found.
     * @see org.springframework.data.jpa.repository.JpaSpecificationExecutor#findOne(Specification)
     *
     * @param spec must not be {@literal null}.
     * @param entityGraph the entity graph hint to use
     * @return never {@literal null}.
     * @throws org.springframework.dao.IncorrectResultSizeDataAccessException if more than one entity found.
     */
    Optional<T> findOne(Specification<T> spec, String entityGraph);

    /**
     * Returns all entities matching the given {@link Specification} with the entity graph hint.
     * <p>
     * @see org.springframework.data.jpa.repository.JpaSpecificationExecutor#findAll(Specification)
     *
     * @param spec can be {@literal null}.
     * @param entityGraph the entity graph hint to use
     * @return never {@literal null}.
     */
    List<T> findAll(@Nullable Specification<T> spec, String entityGraph);

    /**
     * Returns a {@link Page} of entities matching the given {@link Specification} with the entity graph hint.
     * <p>
     * @see org.springframework.data.jpa.repository.JpaSpecificationExecutor#findAll(Specification, Pageable)
     *
     * @param spec can be {@literal null}.
     * @param entityGraph the entity graph hint to use
     * @param pageable must not be {@literal null}.
     * @return never {@literal null}.
     */
    Page<T> findAll(@Nullable Specification<T> spec, String entityGraph, Pageable pageable);

    /**
     * Returns all entities matching the given {@link Specification} and {@link Sort} with the entity graph hint.
     * <p>
     * @see org.springframework.data.jpa.repository.JpaSpecificationExecutor#findAll(Specification, Sort)
     *
     * @param spec can be {@literal null}.
     * @param entityGraph the entity graph hint to use
     * @param sort must not be {@literal null}.
     * @return never {@literal null}.
     */
    List<T> findAll(@Nullable Specification<T> spec, String entityGraph, Sort sort);
}