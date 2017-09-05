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
import javax.validation.constraints.NotNull;

import org.eclipse.hawkbit.im.authentication.SpPermission.SpringEvalExpressions;
import org.eclipse.hawkbit.repository.builder.SoftwareModuleTypeCreate;
import org.eclipse.hawkbit.repository.builder.SoftwareModuleTypeUpdate;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.EntityReadOnlyException;
import org.eclipse.hawkbit.repository.exception.RSQLParameterSyntaxException;
import org.eclipse.hawkbit.repository.exception.RSQLParameterUnsupportedFieldException;
import org.eclipse.hawkbit.repository.model.BaseEntity;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.hibernate.validator.constraints.NotEmpty;
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
// FIXME: generic descriptions and exception
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
    List<T> create(@NotNull Collection<C> creates);

    /**
     * Creates new {@link SoftwareModuleType}.
     *
     * @param create
     *            to create
     * @return created Entity
     * 
     * @throws ConstraintViolationException
     *             if fields are not filled as specified. Check
     *             {@link SoftwareModuleTypeCreate} for field constraints.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_CREATE_REPOSITORY)
    T create(@NotNull C create);

    /**
     * Updates existing {@link BaseEntity}.
     *
     * @param update
     *            to update
     * 
     * @return updated Entity
     * 
     * @throws EntityReadOnlyException
     *             if the {@link DistributionSetType} is already in use by a
     *             {@link DistributionSet} and user tries to change list of
     *             {@link SoftwareModuleType}s
     * @throws EntityNotFoundException
     *             in case the {@link SoftwareModuleType} does not exists and
     *             cannot be updated
     * @throws ConstraintViolationException
     *             if fields are not filled as specified. Check
     *             {@link SoftwareModuleTypeUpdate} for field constraints.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    T update(@NotNull U update);

    /**
     * @return number of {@link SoftwareModuleType}s in the repository.
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
     *             not found is type with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_DELETE_REPOSITORY)
    void delete(@NotNull Long id);

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
     * @return the found {@link DistributionSet}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    List<T> get(@NotEmpty Collection<Long> ids);

    /**
     * @param id
     *            of entity to check existence
     * @return <code>true</code> if entity with given ID exists
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    boolean exists(@NotNull Long id);

    /**
     * Retrieve {@link BaseEntity}
     *
     * @param id
     *            to search for
     * @return {@link SoftwareModuleType} in the repository with given
     *         {@link SoftwareModuleType#getId()}
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Optional<T> get(@NotNull Long id);

    /**
     * Retrieves {@link Page} of all {@link BaseEntity} of given type.
     * 
     * @param pageable
     *            parameter
     * @return all {@link SoftwareModuleType}s in the repository.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Slice<T> findAll(@NotNull Pageable pageable);

    /**
     * Retrieves all {@link SoftwareModuleType}s with a given specification.
     * 
     * @param pageable
     *            pagination parameter
     * @param rsqlParam
     *            filter definition in RSQL syntax
     *
     * @return the found {@link SoftwareModuleType}s
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
