/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.repository.builder.TargetTypeCreate;
import org.eclipse.hawkbit.repository.builder.TargetTypeUpdate;
import org.eclipse.hawkbit.repository.model.TargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Management service for {@link TargetType}s.
 *
 */
public interface TargetTypeManagement {

    /**
     * @param name
     *            as {@link TargetType#getName()}
     * @return {@link TargetType}
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    Optional<TargetType> getByName(@NotEmpty String name);

    /**
     * @return total count
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    long count();

    /**
     * @param create
     *          TargetTypeCreate
     * @return targetType
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_CREATE_TARGET)
    TargetType create(@NotNull @Valid TargetTypeCreate create);

    /**
     * @param creates
     *          List of TargetTypeCreate
     * @return List of targetType
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_CREATE_TARGET)
    List<TargetType> create(@NotNull @Valid Collection<TargetTypeCreate> creates);

    /**
     * @param id targetTypeId
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_DELETE_TARGET)
    void delete(@NotNull Long id);

    /**
     * @param pageable
     *          Page
     * @return TargetType page
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    Page<TargetType> findAll(@NotNull Pageable pageable);


    /**
     * @param pageable
     *          page
     * @param controllerId
     *          Target ID
     * @return Target type
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    Page<TargetType> findByTarget(@NotNull Pageable pageable, @NotEmpty String controllerId);


    /**
     * @param pageable
     *          Page
     * @param rsqlParam
     *          query param
     * @return Target type
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    Page<TargetType> findByRsql(@NotNull Pageable pageable, @NotNull String rsqlParam);


    /**
     * @param id
     *          Target type ID
     * @return Target Type
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    Optional<TargetType> get(long id);


    /**
     * @param ids
     *          List of Target type ID
     * @return Target type list
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    List<TargetType> get(@NotEmpty Collection<Long> ids);


    /**
     * @param update
     *          TargetTypeUpdate
     * @return Target Type
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_UPDATE_TARGET)
    TargetType update(@NotNull @Valid TargetTypeUpdate update);

    /**
     * @param targetTypeId
     *          Target type ID
     * @param distributionSetTypeIds
     *          Distribution set ID
     * @return Target type
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY_AND_UPDATE_TARGET)
    TargetType assignDistributionSetTypes(long targetTypeId,
                                                           @NotEmpty Collection<Long> distributionSetTypeIds);

    /**
     * @param targetTypeId
     *          Target type ID
     * @param distributionSetTypeIds
     *          Distribution set ID
     * @return Target type
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY_AND_UPDATE_TARGET)
    TargetType unassignDistributionSetType(long targetTypeId, long distributionSetTypeIds);


}
