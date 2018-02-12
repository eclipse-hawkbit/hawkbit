/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import javax.validation.ConstraintViolationException;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.eclipse.hawkbit.im.authentication.SpPermission.SpringEvalExpressions;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.EntityReadOnlyException;
import org.eclipse.hawkbit.repository.exception.RSQLParameterSyntaxException;
import org.eclipse.hawkbit.repository.exception.RSQLParameterUnsupportedFieldException;
import org.eclipse.hawkbit.repository.model.BaseEntity;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Generic management methods common to (software) repository content.
 *
 * @param <T>
 *            type of the {@link BaseEntity}
 * @param <C>
 *            entity create builder
 * @param <U>
 *            entity update builder
 */
public interface RepositoryManagement<T, C, U> {

    /**
     * Creates multiple {@link BaseEntity}s.
     *
     * @param creates
     *            to create
     * @return created Entity
     * 
     * @throws ConstraintViolationException
     *             if fields are not filled as specified. Check
     *             {@link BaseEntity} for field constraints.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_CREATE_REPOSITORY)
    List<T> create(@NotNull @Valid Collection<C> creates);

    /**
     * Creates new {@link SoftwareModuleType}.
     *
     * @param create
     *            to create
     * @return created Entity
     * 
     * @throws ConstraintViolationException
     *             if fields are not filled as specified. Check
     *             {@link BaseEntity} for field constraints.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_CREATE_REPOSITORY)
    T create(@NotNull @Valid C create);

    /**
     * Updates existing {@link BaseEntity}.
     *
     * @param update
     *            to update
     * 
     * @return updated Entity
     * 
     * @throws EntityReadOnlyException
     *             if the {@link BaseEntity} cannot be updated (e.g. is already
     *             in use)
     * @throws EntityNotFoundException
     *             in case the {@link BaseEntity} does not exists and cannot be
     *             updated
     * @throws ConstraintViolationException
     *             if fields are not filled as specified. Check
     *             {@link BaseEntity} for field constraints.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    T update(@NotNull @Valid U update);

    /**
     * @return number of {@link BaseEntity}s in the repository.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    long count();

    /**
     * Deletes or marks as delete in case the {@link BaseEntity} is in use.
     *
     * @param id
     *            to delete
     * 
     * @throws EntityNotFoundException
     *             BaseEntity with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_DELETE_REPOSITORY)
    void delete(long id);

    /**
     * Delete {@link BaseEntity}s by their IDs. That is either a soft delete of
     * the entities have been linked to another entity before or a hard delete
     * if not.
     *
     * @param ids
     *            to be deleted
     * 
     * @throws EntityNotFoundException
     *             if (at least one) given distribution set does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_DELETE_REPOSITORY)
    void delete(@NotEmpty Collection<Long> ids);

    /**
     * Retrieves all {@link BaseEntity}s without details.
     *
     * @param ids
     *            the ids to for
     * @return the found {@link BaseEntity}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    List<T> get(@NotEmpty Collection<Long> ids);

    /**
     * Verifies that {@link BaseEntity} with given ID exists in the repository.
     * 
     * @param id
     *            of entity to check existence
     * @return <code>true</code> if entity with given ID exists
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    boolean exists(long id);

    /**
     * Retrieve {@link BaseEntity}
     *
     * @param id
     *            to search for
     * @return {@link BaseEntity} in the repository with given
     *         {@link BaseEntity#getId()}
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Optional<T> get(long id);

    /**
     * Retrieves {@link Page} of all {@link BaseEntity} of given type.
     * 
     * @param pageable
     *            paging parameter
     * @return all {@link BaseEntity}s in the repository.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Slice<T> findAll(@NotNull Pageable pageable);

    /**
     * Retrieves all {@link BaseEntity}s with a given specification.
     * 
     * @param pageable
     *            pagination parameter
     * @param rsqlParam
     *            filter definition in RSQL syntax
     *
     * @return the found {@link BaseEntity}s
     * 
     * @throws RSQLParameterUnsupportedFieldException
     *             if a field in the RSQL string is used but not provided by the
     *             given {@code fieldNameProvider}
     * @throws RSQLParameterSyntaxException
     *             if the RSQL syntax is wrong
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Page<T> findByRsql(@NotNull Pageable pageable, @NotNull String rsqlParam);
}
