/**
 * Copyright (c) 2023 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.repository;

import org.eclipse.hawkbit.repository.jpa.acm.AccessController;
import org.eclipse.hawkbit.repository.model.BaseEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface that offers findAll with disabled count query.
 *
 * @param <T>
 *            entity type
 */
public interface ACMRepository<T> {

    @NonNull
    <S extends T> S save(@NonNull AccessController.Operation operation, @NonNull final S entity);

    <S extends T> List<S> saveAll(@NonNull AccessController.Operation operation, final Iterable<S> entities);

    /**
     * Returns single entry that match specification and the operation is allowed for.
     *
     * @param operation access operation
     * @param spec specification
     * @return matching entity
     */
    @NonNull
    Optional<T> findOne(@NonNull AccessController.Operation operation, @Nullable Specification<T> spec);

    /**
     * Returns all entries that match specification and the operation is allowed for.
     *
     * @param operation access operation
     * @param spec specification
     * @return matching entities
     */
    @NonNull
    List<T> findAll(@NonNull AccessController.Operation operation, @Nullable Specification<T> spec);

    @NonNull
    boolean exists(@NonNull AccessController.Operation operation, Specification<T> spec);

    /**
     * Returns count of all entries that match specification and the operation is allowed for.
     *
     * @param operation access operation
     * @param spec specification
     * @return count of matching entities
     */
    @NonNull
    long count(@NonNull AccessController.Operation operation, @Nullable Specification<T> spec);

    @NonNull
    Slice<T> findAllWithoutCount(
            @NonNull final AccessController.Operation operation, @Nullable Specification<T> spec, Pageable pageable);
}
