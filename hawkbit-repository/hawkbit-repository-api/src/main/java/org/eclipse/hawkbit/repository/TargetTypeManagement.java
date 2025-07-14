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

import static org.eclipse.hawkbit.im.authentication.SpringEvalExpressions.BRACKET_CLOSE;
import static org.eclipse.hawkbit.im.authentication.SpringEvalExpressions.BRACKET_OPEN;
import static org.eclipse.hawkbit.im.authentication.SpringEvalExpressions.HAS_AUTH_AND;
import static org.eclipse.hawkbit.im.authentication.SpringEvalExpressions.HAS_AUTH_CREATE_TARGET_TYPE;
import static org.eclipse.hawkbit.im.authentication.SpringEvalExpressions.HAS_AUTH_DELETE_TARGET_TYPE;
import static org.eclipse.hawkbit.im.authentication.SpringEvalExpressions.HAS_AUTH_PREFIX;
import static org.eclipse.hawkbit.im.authentication.SpringEvalExpressions.HAS_AUTH_READ_TARGET_TYPE;
import static org.eclipse.hawkbit.im.authentication.SpringEvalExpressions.HAS_AUTH_SUFFIX;
import static org.eclipse.hawkbit.im.authentication.SpringEvalExpressions.HAS_AUTH_UPDATE_TARGET_TYPE;

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
 */
public interface TargetTypeManagement {

    String HAS_AUTH_READ_DISTRIBUTION_SET_AND_UPDATE_TARGET_TYPE = BRACKET_OPEN +
            HAS_AUTH_PREFIX + SpPermission.READ_DISTRIBUTION_SET + HAS_AUTH_SUFFIX +
            HAS_AUTH_AND +
            HAS_AUTH_PREFIX + SpPermission.UPDATE_TARGET_TYPE + HAS_AUTH_SUFFIX +
            BRACKET_CLOSE;

    /**
     * @param key as {@link TargetType#getKey()}
     * @return {@link TargetType}
     */
    @PreAuthorize(HAS_AUTH_READ_TARGET_TYPE)
    Optional<TargetType> getByKey(@NotEmpty String key);

    /**
     * @param name as {@link TargetType#getName()}
     * @return {@link TargetType}
     */
    @PreAuthorize(HAS_AUTH_READ_TARGET_TYPE)
    Optional<TargetType> getByName(@NotEmpty String name);

    /**
     * @return total count
     */
    @PreAuthorize(HAS_AUTH_READ_TARGET_TYPE)
    long count();

    /**
     * @param name as {@link TargetType#getName()}
     * @return total count by name
     */
    @PreAuthorize(HAS_AUTH_READ_TARGET_TYPE)
    long countByName(String name);

    /**
     * @param create TargetTypeCreate
     * @return targetType
     */
    @PreAuthorize(HAS_AUTH_CREATE_TARGET_TYPE)
    TargetType create(@NotNull @Valid TargetTypeCreate create);

    /**
     * @param creates List of TargetTypeCreate
     * @return List of targetType
     */
    @PreAuthorize(HAS_AUTH_CREATE_TARGET_TYPE)
    List<TargetType> create(@NotEmpty @Valid Collection<TargetTypeCreate> creates);

    /**
     * @param id targetTypeId
     */
    @PreAuthorize(HAS_AUTH_DELETE_TARGET_TYPE)
    void delete(@NotNull Long id);

    /**
     * @param pageable Page
     * @return TargetType page
     */
    @PreAuthorize(HAS_AUTH_READ_TARGET_TYPE)
    Slice<TargetType> findAll(@NotNull Pageable pageable);

    /**
     * @param rsql query param
     * @param pageable Page
     * @return Target type
     */
    @PreAuthorize(HAS_AUTH_READ_TARGET_TYPE)
    Page<TargetType> findByRsql(@NotEmpty String rsql, @NotNull Pageable pageable);

    /**
     * Retrieves {@link TargetType}s by filtering on the given parameters.
     *
     * @param name has text of filters to be applied.
     * @param pageable page parameter
     * @return the page of found {@link TargetType}
     */
    @PreAuthorize(HAS_AUTH_READ_TARGET_TYPE)
    Slice<TargetType> findByName(String name, @NotNull Pageable pageable);

    /**
     * @param id Target type ID
     * @return Target Type
     */
    @PreAuthorize(HAS_AUTH_READ_TARGET_TYPE)
    Optional<TargetType> get(long id);

    /**
     * @param ids List of Target type ID
     * @return Target type list
     */
    @PreAuthorize(HAS_AUTH_READ_TARGET_TYPE)
    List<TargetType> get(@NotEmpty Collection<Long> ids);

    /**
     * @param update TargetTypeUpdate
     * @return Target Type
     */
    @PreAuthorize(HAS_AUTH_UPDATE_TARGET_TYPE)
    TargetType update(@NotNull @Valid TargetTypeUpdate update);

    /**
     * @param id Target type ID
     * @param distributionSetTypeIds Distribution set ID
     * @return Target type
     */
    @PreAuthorize(HAS_AUTH_READ_DISTRIBUTION_SET_AND_UPDATE_TARGET_TYPE)
    TargetType assignCompatibleDistributionSetTypes(long id, @NotEmpty Collection<Long> distributionSetTypeIds);

    /**
     * @param id Target type ID
     * @param distributionSetTypeIds Distribution set ID
     * @return Target type
     */
    @PreAuthorize(HAS_AUTH_READ_DISTRIBUTION_SET_AND_UPDATE_TARGET_TYPE)
    TargetType unassignDistributionSetType(long id, long distributionSetTypeIds);
}