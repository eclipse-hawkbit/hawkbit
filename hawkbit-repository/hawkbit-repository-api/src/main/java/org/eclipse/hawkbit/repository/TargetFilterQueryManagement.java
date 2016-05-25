/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

import javax.validation.constraints.NotNull;

import org.eclipse.hawkbit.im.authentication.SpPermission.SpringEvalExpressions;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Management service for {@link TargetFilterQuery}s.
 *
 */
public interface TargetFilterQueryManagement {

    /**
     * creating new {@link TargetFilterQuery}.
     *
     * @param customTargetFilter
     * @return the created {@link TargetFilterQuery}
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_CREATE_TARGET)
    TargetFilterQuery createTargetFilterQuery(@NotNull TargetFilterQuery customTargetFilter);

    /**
     * Delete target filter query.
     *
     * @param targetFilterQueryId
     *            IDs of target filter query to be deleted
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_DELETE_TARGET)
    void deleteTargetFilterQuery(@NotNull Long targetFilterQueryId);

    /**
     *
     * Retrieves all target filter query{@link TargetFilterQuery}.
     *
     * @param pageable
     *            pagination parameter
     * @return the found {@link TargetFilterQuery}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    Page<TargetFilterQuery> findAllTargetFilterQuery(@NotNull Pageable pageable);

    /**
     * Retrieves all target filter query which {@link TargetFilterQuery}.
     *
     *
     * @param pageable
     *            pagination parameter
     * @param name
     *            target filter query name
     * @return the page with the found {@link TargetFilterQuery}
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    Page<TargetFilterQuery> findTargetFilterQueryByFilters(@NotNull Pageable pageable, String name);

    /**
     * Find target filter query by id.
     *
     * @param targetFilterQueryId
     *            Target filter query id
     * @return the found {@link TargetFilterQuery}
     *
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    TargetFilterQuery findTargetFilterQueryById(@NotNull Long targetFilterQueryId);

    /**
     * Find target filter query by name.
     *
     * @param targetFilterQueryName
     *            Target filter query name
     * @return the found {@link TargetFilterQuery}
     *
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    TargetFilterQuery findTargetFilterQueryByName(@NotNull String targetFilterQueryName);

    /**
     * updates the {@link TargetFilterQuery}.
     *
     * @param targetFilterQuery
     *            to be updated
     * @return the updated {@link TargetFilterQuery}
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_TARGET)
    TargetFilterQuery updateTargetFilterQuery(@NotNull TargetFilterQuery targetFilterQuery);

    /**
     * Generates an empty {@link TargetFilterQuery} without persisting it.
     * 
     * @return {@link TargetFilterQuery} object
     */
    TargetFilterQuery generateTargetFilterQuery();
}