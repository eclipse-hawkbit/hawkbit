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

import java.util.List;
import java.util.Optional;

import org.eclipse.hawkbit.repository.jpa.acm.AccessController;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.domain.Specification;
import org.jspecify.annotations.Nullable;

/**
 * Repository interface that offers some actions that takes in account a target operation.
 *
 * @param <T> entity type
 */
public interface ACMRepository<T> {

    /**
     * Saves only if the caller have access for the operation over the entity. This method could be used to
     * check CREATE access in creating an entity (save without operation would check for UPDATE access).
     *
     * @param operation access operationIf operation is <code>null</code> no access is checked! Should be used only for tenant context.
     * @param entity the entity to save
     * @return the saved entity
     */
    @NonNull
    <S extends T> S save(AccessController.Operation operation, @NonNull S entity);

    /**
     * Saves only if the caller have access for the operation over all entities. This method could be used to
     * check CREATE access in creating an entity (save without operation would check for UPDATE access).
     *
     * @param operation access operationIf operation is <code>null</code> no access is checked! Should be used only for tenant context.
     * @param entities the entities to save
     * @return the saved entities
     */
    <S extends T> List<S> saveAll(AccessController.Operation operation, Iterable<S> entities);

    /**
     * Returns single entry that match specification and the operation is allowed for.
     *
     * @param operation access operation. If operation is <code>null</code> no access is checked! Should be used only for tenant context.
     * @param spec specification
     * @return matching entity
     */
    @NonNull
    Optional<T> findOne(AccessController.Operation operation, @NonNull Specification<T> spec);

    /**
     * Returns all entries that match specification and the operation is allowed for.
     *
     * @param operation access operation. If operation is <code>null</code> no access is checked! Should be used only for tenant context.
     * @param spec specification
     * @return matching entities
     */
    @NonNull
    List<T> findAll(AccessController.Operation operation, @Nullable Specification<T> spec);

    /**
     * Returns all entries that match specification and the operation is allowed for.
     *
     * @param operation access operation. If operation is <code>null</code> no access is checked! Should be used
     *         only for tenant context.
     * @param spec specification
     * @return matching entities
     */
    boolean exists(AccessController.Operation operation, Specification<T> spec);

    /**
     * Returns count of all entries that match specification and the operation is allowed for.
     *
     * @param operation access operation. If operation is <code>null</code> no access is checked! Should be used
     *         only for tenant context.
     * @param spec specification
     * @return count of matching entities
     */
    long count(AccessController.Operation operation, @Nullable Specification<T> spec);

    /**
     * Returns all entries, without count, that match specification and the operation is allowed for.
     *
     * @param operation access operation. If operation is <code>null</code> no access is checked! Should be used
     *         only for tenant context.
     * @param spec specification
     * @param pageable pageable
     * @return count of matching entities
     */
    @NonNull
    Slice<T> findAllWithoutCount(AccessController.Operation operation, @Nullable Specification<T> spec, Pageable pageable);

    @NonNull
    Class<T> getDomainClass();
}