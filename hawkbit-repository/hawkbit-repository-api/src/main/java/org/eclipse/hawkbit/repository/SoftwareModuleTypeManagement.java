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
import org.eclipse.hawkbit.repository.exception.RSQLParameterSyntaxException;
import org.eclipse.hawkbit.repository.exception.RSQLParameterUnsupportedFieldException;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Service for managing {@link SoftwareModuleType}s.
 *
 */
public interface SoftwareModuleTypeManagement {

    /**
     * @return number of {@link SoftwareModuleType}s in the repository.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Long countSoftwareModuleTypesAll();

    /**
     * Creates multiple {@link SoftwareModuleType}s.
     *
     * @param creates
     *            to create
     * @return created Entity
     * 
     * @throws ConstraintViolationException
     *             if fields are not filled as specified. Check
     *             {@link SoftwareModuleTypeCreate} for field constraints.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_CREATE_REPOSITORY)
    List<SoftwareModuleType> createSoftwareModuleType(@NotNull Collection<SoftwareModuleTypeCreate> creates);

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
    SoftwareModuleType createSoftwareModuleType(@NotNull SoftwareModuleTypeCreate create);

    /**
     * Deletes or marks as delete in case the type is in use.
     *
     * @param typeId
     *            to delete
     * 
     * @throws EntityNotFoundException
     *             not found is type with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_DELETE_REPOSITORY)
    void deleteSoftwareModuleType(@NotNull Long typeId);

    /**
     *
     * @param id
     *            to search for
     * @return {@link SoftwareModuleType} in the repository with given
     *         {@link SoftwareModuleType#getId()}
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Optional<SoftwareModuleType> findSoftwareModuleTypeById(@NotNull Long id);

    /**
     *
     * @param key
     *            to search for
     * @return {@link SoftwareModuleType} in the repository with given
     *         {@link SoftwareModuleType#getKey()}
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Optional<SoftwareModuleType> findSoftwareModuleTypeByKey(@NotEmpty String key);

    /**
     *
     * @param name
     *            to search for
     * @return all {@link SoftwareModuleType}s in the repository with given
     *         {@link SoftwareModuleType#getName()}
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Optional<SoftwareModuleType> findSoftwareModuleTypeByName(@NotEmpty String name);

    /**
     * @param pageable
     *            parameter
     * @return all {@link SoftwareModuleType}s in the repository.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Page<SoftwareModuleType> findSoftwareModuleTypesAll(@NotNull Pageable pageable);

    /**
     * Retrieves all {@link SoftwareModuleType}s with a given specification.
     *
     * @param rsqlParam
     *            filter definition in RSQL syntax
     * @param pageable
     *            pagination parameter
     * @return the found {@link SoftwareModuleType}s
     * 
     * @throws RSQLParameterUnsupportedFieldException
     *             if a field in the RSQL string is used but not provided by the
     *             given {@code fieldNameProvider}
     * @throws RSQLParameterSyntaxException
     *             if the RSQL syntax is wrong
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Page<SoftwareModuleType> findSoftwareModuleTypesAll(@NotNull String rsqlParam, @NotNull Pageable pageable);

    /**
     * Updates existing {@link SoftwareModuleType}.
     *
     * @param update
     *            to update
     * 
     * @return updated Entity
     * 
     * @throws EntityNotFoundException
     *             in case the {@link SoftwareModuleType} does not exists and
     *             cannot be updated
     * @throws ConstraintViolationException
     *             if fields are not filled as specified. Check
     *             {@link SoftwareModuleTypeUpdate} for field constraints.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    SoftwareModuleType updateSoftwareModuleType(@NotNull SoftwareModuleTypeUpdate update);

}
