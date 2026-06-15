/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository;

import jakarta.validation.constraints.NotNull;
import org.eclipse.hawkbit.auth.SpringEvalExpressions;
import org.eclipse.hawkbit.repository.model.BaseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Extension of {@link RepositoryManagement} for entities that support soft deletion,
 * providing query methods that allow filtering by soft-deleted state.
 *
 * @param <T> type of the {@link BaseEntity}
 * @param <C> type of the create request
 * @param <U> type of the update request
 */
public interface SoftDeletableRepositoryManagement<T extends BaseEntity, C, U extends Identifiable<Long>>
        extends RepositoryManagement<T, C, U> {

    /**
     * Retrieves a {@link Page} of all {@link BaseEntity}s filtered by their soft-deleted state.
     *
     * @param softDeletedMode the filter defining which entities to return based on their soft-deleted status
     * @param pageable the page request to sort and limit the result
     * @return a page of found entities matching the given soft-deleted filter
     */
    @PreAuthorize(SpringEvalExpressions.HAS_READ_REPOSITORY)
    Page<T> findAll(@NotNull SoftDeletedMode softDeletedMode, @NotNull Pageable pageable);

    /**
     * Retrieves a {@link Page} of {@link BaseEntity}s matching the given RSQL filter,
     * filtered by their soft-deleted state.
     *
     * @param rsql filter definition in RSQL syntax
     * @param softDeletedMode the filter defining which entities to return based on their soft-deleted status
     * @param pageable the page request to sort and limit the result
     * @return a page of found entities matching both the RSQL filter and the soft-deleted filter
     */
    @PreAuthorize(SpringEvalExpressions.HAS_READ_REPOSITORY)
    Page<T> findByRsql(@NotNull String rsql, @NotNull SoftDeletedMode softDeletedMode, @NotNull Pageable pageable);
}
