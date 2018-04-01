/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

import java.util.Optional;

import javax.validation.ConstraintViolationException;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.eclipse.hawkbit.im.authentication.SpPermission.SpringEvalExpressions;
import org.eclipse.hawkbit.repository.builder.TargetFilterQueryCreate;
import org.eclipse.hawkbit.repository.builder.TargetFilterQueryUpdate;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.RSQLParameterSyntaxException;
import org.eclipse.hawkbit.repository.exception.RSQLParameterUnsupportedFieldException;
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
     * @param create
     *            to create
     * @return the created {@link TargetFilterQuery}
     * @throws ConstraintViolationException
     *             if fields are not filled as specified. Check
     *             {@link TargetFilterQueryCreate} for field constraints.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_CREATE_TARGET)
    TargetFilterQuery create(@NotNull @Valid TargetFilterQueryCreate create);

    /**
     * Delete target filter query.
     *
     * @param targetFilterQueryId
     *            IDs of target filter query to be deleted
     * 
     * @throws EntityNotFoundException
     *             if filter with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_DELETE_TARGET)
    void delete(long targetFilterQueryId);

    /**
     * Verifies provided filter syntax.
     * 
     * @param query
     *            to verify
     * 
     * @return <code>true</code> if syntax is valid
     * 
     * @throws RSQLParameterUnsupportedFieldException
     *             if a field in the RSQL string is used but not provided by the
     *             given {@code fieldNameProvider}
     * @throws RSQLParameterSyntaxException
     *             if the RSQL syntax is wrong
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    boolean verifyTargetFilterQuerySyntax(@NotNull String query);

    /**
     *
     * Retrieves all target filter query{@link TargetFilterQuery}.
     *
     * @param pageable
     *            pagination parameter
     * @return the found {@link TargetFilterQuery}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    Page<TargetFilterQuery> findAll(@NotNull Pageable pageable);

    /**
     * Counts all target filter queries
     * 
     * @return the number of all target filter queries
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    long count();

    /**
     * Retrieves all target filter query which {@link TargetFilterQuery}.
     *
     *
     * @param pageable
     *            pagination parameter
     * @param name
     *            name filter
     * @return the page with the found {@link TargetFilterQuery}
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    Page<TargetFilterQuery> findByName(@NotNull Pageable pageable, @NotNull String name);

    /**
     * Retrieves all target filter query which {@link TargetFilterQuery}.
     *
     *
     * @param pageable
     *            pagination parameter
     * @param rsqlFilter
     *            RSQL filter string
     * @return the page with the found {@link TargetFilterQuery}
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    Page<TargetFilterQuery> findByRsql(@NotNull Pageable pageable, @NotNull String rsqlFilter);

    /**
     * Retrieves all target filter query which have exactly the provided query.
     *
     * @param pageable
     *            pagination parameter
     * @param query
     *            the query saved in the target filter query
     * @return the page with the found {@link TargetFilterQuery}
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    Page<TargetFilterQuery> findByQuery(@NotNull Pageable pageable, @NotNull String query);

    /**
     * Retrieves all target filter query which {@link TargetFilterQuery}.
     *
     *
     * @param pageable
     *            pagination parameter
     * @param setId
     *            the auto assign distribution set
     * @param rsqlParam
     *            RSQL filter
     * @return the page with the found {@link TargetFilterQuery}
     * 
     * @throws EntityNotFoundException
     *             if DS with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    Page<TargetFilterQuery> findByAutoAssignDSAndRsql(@NotNull Pageable pageable, long setId,
            String rsqlParam);

    /**
     * Retrieves all target filter query with auto assign DS which
     * {@link TargetFilterQuery}.
     *
     *
     * @return the page with the found {@link TargetFilterQuery}
     * @param pageable
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    Page<TargetFilterQuery> findWithAutoAssignDS(@NotNull Pageable pageable);

    /**
     * Find target filter query by id.
     *
     * @param targetFilterQueryId
     *            Target filter query id
     * @return the found {@link TargetFilterQuery}
     *
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    Optional<TargetFilterQuery> get(long targetFilterQueryId);

    /**
     * Find target filter query by name.
     *
     * @param targetFilterQueryName
     *            Target filter query name
     * @return the found {@link TargetFilterQuery}
     *
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    Optional<TargetFilterQuery> getByName(@NotNull String targetFilterQueryName);

    /**
     * updates the {@link TargetFilterQuery}.
     *
     * @param update
     *            to be updated
     * 
     * @return the updated {@link TargetFilterQuery}
     * 
     * @throws EntityNotFoundException
     *             if either {@link TargetFilterQuery} and/or autoAssignDs are
     *             provided but not found
     * @throws ConstraintViolationException
     *             if fields are not filled as specified. Check
     *             {@link TargetFilterQueryUpdate} for field constraints.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_TARGET)
    TargetFilterQuery update(@NotNull @Valid TargetFilterQueryUpdate update);

    /**
     * updates the {@link TargetFilterQuery#getAutoAssignDistributionSet()}.
     *
     * @param queryId
     *            to be updated
     * @param dsId
     *            to be updated or <code>null</code> in order to remove it
     * @return the updated {@link TargetFilterQuery}
     * 
     * @throws EntityNotFoundException
     *             if either {@link TargetFilterQuery} and/or autoAssignDs are
     *             provided but not found
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_TARGET)
    TargetFilterQuery updateAutoAssignDS(long queryId, Long dsId);

}
