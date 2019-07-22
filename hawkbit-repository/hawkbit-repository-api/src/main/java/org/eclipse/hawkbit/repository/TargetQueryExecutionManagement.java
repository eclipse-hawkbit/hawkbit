/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

import java.util.Collection;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.im.authentication.SpPermission.SpringEvalExpressions;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * This interface is the central point which is responsible to execute queries
 * and return update-targets for Hawkbit.
 *
 * By design it only contains generic filter methods so given query parameters
 * can be used in a more universal, model independent manner. Thus domain
 * specific querying e.g. {@code findByFilterAndDistributionSet(...)} should
 * rather be expressed within the query syntax, or by providing a pre-select
 * with the {@code inIdList} parameter, than by introducing (new) named methods.
 */
public interface TargetQueryExecutionManagement {

    /**
     * Retrieves all targets.
     *
     * @param pageable
     *            pagination parameter
     * @return the found {@link Target}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    Page<Target> findAll(@NotNull Pageable pageable);

    /**
     * Query targets
     *
     * @param pageable
     *            pagination parameter
     * @param query
     *            in RSQL notation
     *
     * @return the found {@linkplain Target}s, never {@code null}
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    Page<Target> findByQuery(@NotNull Pageable pageable, @NotEmpty String query);

    /**
     * Query targets and limit the result to be in the provided ID-list
     *
     * @param pageable
     *            pagination parameter
     * @param query
     *            in RSQL notation
     * @param inIdList
     *            Limit the result to the provided IDs ("... AND controllerId in
     *            [id, ...]")
     *
     * @return the found {@linkplain Target}s, never {@code null}
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    Page<Target> findByQuery(@NotNull Pageable pageable, @NotEmpty String query, Collection<String> inIdList);

    /**
     * Count with a {@linkplain TargetFilterQuery#getQuery()}
     *
     * @param query
     *            filter definition in RSQL syntax
     * @return the found number {@linkplain Target}s
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    long countByQuery(@NotEmpty String query);

    /**
     * Count with a {@linkplain TargetFilterQuery#getQuery()}
     *
     * @param query
     *            filter definition in RSQL syntax
     * @param inIdList
     *            Limit the result to the provided IDs ("... AND controllerId in
     *            [id, ...]")
     * @return the found number {@linkplain Target}s
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    long countByQuery(@NotEmpty String query, @NotNull Collection<String> inIdList);

    /**
     * Counts all {@linkplain Target}s in the repository
     *
     * @return number of {@linkplain Target}s
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    long count();
}
