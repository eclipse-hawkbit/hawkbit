/**
 * Copyright (c) 2021 Bosch.IO GmbH and others
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
import java.util.Optional;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.repository.builder.TargetTypeCreate;
import org.eclipse.hawkbit.repository.builder.TargetTypeUpdate;
import org.eclipse.hawkbit.repository.model.TargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Management service for {@link TargetType}s.
 *
 */
public interface TargetTypeManagement {

    /**
     * @param key
     *            as {@link TargetType#getKey()}
     * @return {@link TargetType}
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    Optional<TargetType> getByKey(@NotEmpty String key);

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
     * @param name
     *            as {@link TargetType#getName()}
     * @return total count by name
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    long countByName(String name);

    /**
     * @param create
     *            TargetTypeCreate
     * @return targetType
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_CREATE_TARGET)
    TargetType create(@NotNull @Valid TargetTypeCreate create);

    /**
     * @param creates
     *            List of TargetTypeCreate
     * @return List of targetType
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_CREATE_TARGET)
    List<TargetType> create(@NotEmpty @Valid Collection<TargetTypeCreate> creates);

    /**
     * @param id
     *            targetTypeId
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_DELETE_TARGET)
    void delete(@NotNull Long id);

    /**
     * @param pageable
     *            Page
     * @return TargetType page
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    Slice<TargetType> findAll(@NotNull Pageable pageable);

    /**
     * @param pageable
     *            Page
     * @param rsqlParam
     *            query param
     * @return Target type
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    Page<TargetType> findByRsql(@NotNull Pageable pageable, @NotEmpty String rsqlParam);

    /**
     * Retrieves {@link TargetType}s by filtering on the given parameters.
     *
     * @param pageable
     *            page parameter
     * @param name
     *            has text of filters to be applied.
     * @return the page of found {@link TargetType}
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    Slice<TargetType> findByName(@NotNull Pageable pageable, String name);

    /**
     * @param id
     *            Target type ID
     * @return Target Type
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    Optional<TargetType> get(long id);

    /**
     * @param ids
     *            List of Target type ID
     * @return Target type list
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    List<TargetType> get(@NotEmpty Collection<Long> ids);

    /**
     * @param update
     *            TargetTypeUpdate
     * @return Target Type
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_UPDATE_TARGET)
    TargetType update(@NotNull @Valid TargetTypeUpdate update);

    /**
     * @param id
     *            Target type ID
     * @param distributionSetTypeIds
     *            Distribution set ID
     * @return Target type
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY_AND_UPDATE_TARGET)
    TargetType assignCompatibleDistributionSetTypes(long id,
            @NotEmpty Collection<Long> distributionSetTypeIds);

    /**
     * @param id
     *            Target type ID
     * @param distributionSetTypeIds
     *            Distribution set ID
     * @return Target type
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY_AND_UPDATE_TARGET)
    TargetType unassignDistributionSetType(long id, long distributionSetTypeIds);
}
