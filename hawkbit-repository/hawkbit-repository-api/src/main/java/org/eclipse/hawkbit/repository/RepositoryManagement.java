/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import org.eclipse.hawkbit.auth.SpringEvalExpressions;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.EntityReadOnlyException;
import org.eclipse.hawkbit.repository.exception.RSQLParameterSyntaxException;
import org.eclipse.hawkbit.repository.exception.RSQLParameterUnsupportedFieldException;
import org.eclipse.hawkbit.repository.model.BaseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Generic management methods common to (software) repository content.
 *
 * @param <T> type of the {@link BaseEntity}
 * @param <C> type of the create request
 * @param <U> type of the update request
 */
public interface RepositoryManagement<T extends BaseEntity, C, U extends Identifiable<Long>> extends PermissionSupport {

    /**
     * Creates new {@link BaseEntity}.
     *
     * @param create bean with properties of the object to create
     * @return created Entity
     * @throws ConstraintViolationException if fields are not filled as specified. Check {@link BaseEntity} for field constraints.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_CREATE_REPOSITORY)
    T create(@NotNull @Valid C create);

    /**
     * Creates multiple {@link BaseEntity}s.
     *
     * @param create beans with properties of the object to create
     * @return created Entity
     * @throws ConstraintViolationException if fields are not filled as specified. Check {@link BaseEntity} for field constraints.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_CREATE_REPOSITORY)
    List<T> create(@NotNull @Valid Collection<C> create);

    /**
     * Retrieve {@link BaseEntity} and throws exception if not found.
     *
     * @param id to search for
     * @return {@link BaseEntity} in the repository with given {@link BaseEntity#getId()}
     * @throws EntityNotFoundException if no entity with given ID exists
     */
    @PreAuthorize(SpringEvalExpressions.HAS_READ_REPOSITORY)
    T get(long id);

    /**
     * Retrieve {@link BaseEntity}
     *
     * @param id to search for
     * @return {@link BaseEntity} in the repository with given {@link BaseEntity#getId()}
     */
    @PreAuthorize(SpringEvalExpressions.HAS_READ_REPOSITORY)
    Optional<T> find(long id);

    /**
     * Retrieves {@link BaseEntity}s by id and throws exception if any of the requested entities are not found.
     *
     * @param ids the ids to for
     * @return the found {@link BaseEntity}s
     * @throws EntityNotFoundException if at least one of the given ids does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_READ_REPOSITORY)
    List<T> get(@NotEmpty Collection<Long> ids);

    /**
     * Retrieves {@link BaseEntity}s by id and skips not found.
     *
     * @param ids the ids to for
     * @return the found {@link BaseEntity}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_READ_REPOSITORY)
    List<T> find(@NotEmpty Collection<Long> ids);

    /**
     * Retrieves {@link Page} of all {@link BaseEntity} of given type.
     *
     * @param pageable paging parameter
     * @return all {@link BaseEntity}s in the repository.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_READ_REPOSITORY)
    Page<T> findAll(@NotNull Pageable pageable);

    /**
     * Retrieves all {@link BaseEntity}s with a given specification.
     *
     * @param rsql filter definition in RSQL syntax
     * @param pageable pagination parameter
     * @return the found {@link BaseEntity}s
     * @throws RSQLParameterUnsupportedFieldException if a field in the RSQL string is used but not provided by the given
     *         {@code fieldNameProvider}
     * @throws RSQLParameterSyntaxException if the RSQL syntax is wrong
     */
    @PreAuthorize(SpringEvalExpressions.HAS_READ_REPOSITORY)
    Page<T> findByRsql(@NotNull String rsql, @NotNull Pageable pageable);

    /**
     * Verifies that {@link BaseEntity} with given ID exists in the repository.
     *
     * @param id of entity to check existence
     * @return <code>true</code> if entity with given ID exists
     */
    @PreAuthorize(SpringEvalExpressions.HAS_READ_REPOSITORY)
    boolean exists(long id);

    /**
     * @return number of {@link BaseEntity}s in the repository.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_READ_REPOSITORY)
    long count();

    /**
     * Counts the number of {@link BaseEntity}s matching the given RSQL filter.
     *
     * @param rsql filter definition in RSQL syntax
     * @return number of matching {@link BaseEntity}s in the repository.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_READ_REPOSITORY)
    long countByRsql(String rsql);

    /**
     * Updates existing {@link BaseEntity}.
     *
     * @param update bean with properties of the object to update
     * @return updated Entity
     * @throws EntityReadOnlyException if the {@link BaseEntity} cannot be updated (e.g. is already in use)
     * @throws EntityNotFoundException in case the {@link BaseEntity} does not exist and cannot be updated
     * @throws ConstraintViolationException if fields are not filled as specified. Check {@link BaseEntity} for field constraints.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_UPDATE_REPOSITORY)
    T update(@NotNull @Valid U update);

    /**
     * Updates existing {@link BaseEntity}s.
     *
     * @param update bean with properties of the object to update
     * @return updated entity map -> key is the ID of the entity, value is the updated entity
     * @throws EntityReadOnlyException if the {@link BaseEntity} cannot be updated (e.g. is already in use)
     * @throws EntityNotFoundException in case the {@link BaseEntity} does not exist and cannot be updated
     * @throws ConstraintViolationException if fields are not filled as specified. Check {@link BaseEntity} for field constraints.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_UPDATE_REPOSITORY)
    Map<Long, T> update(@NotNull @Valid Collection<U> update);

    /**
     * Deletes or marks as delete in case the {@link BaseEntity} is in use.
     *
     * @param id to delete
     * @throws EntityNotFoundException BaseEntity with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_DELETE_REPOSITORY)
    void delete(long id);

    /**
     * Delete {@link BaseEntity}s by their IDs. That is either a soft delete of the entities have been linked to another entity before or a hard
     * delete if not.
     *
     * @param ids to be deleted
     * @throws EntityNotFoundException if (at least one) given distribution set does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_DELETE_REPOSITORY)
    void delete(@NotEmpty Collection<Long> ids);

    @Override
    default String permissionGroup() {
        return "REPOSITORY";
    }
}