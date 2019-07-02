/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.im.authentication.SpPermission.SpringEvalExpressions;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Collection;

/**
 * This interface is the central point which is responsible to execute queries
 * and return update-targets for Hawkbit.
 *
 * By design it should be as minimal as possible in order to make it easy to
 * plug-in different sources of {@linkplain Target Targets}.
 * 
 * For any use-case specific query use the query-/count-methods which limit the
 * result to a predefined set of IDs.
 */
public interface TargetQueryExecutionManagement<T extends Target> {

    /**
     * Retrieves all targets.
     *
     * @param pageable
     *            pagination parameter
     * @return the found {@link Target}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    Page<T> findAll(@NotNull Pageable pageable);

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
    Page<T> findByQuery(@NotNull Pageable pageable, @NotEmpty String query);

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
    Page<T> findByQuery(@NotNull Pageable pageable, @NotEmpty String query, Collection<String> inIdList);

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
}
