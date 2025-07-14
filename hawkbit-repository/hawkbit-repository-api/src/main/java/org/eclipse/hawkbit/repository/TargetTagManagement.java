/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
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

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import org.eclipse.hawkbit.im.authentication.SpringEvalExpressions;
import org.eclipse.hawkbit.repository.builder.TagCreate;
import org.eclipse.hawkbit.repository.builder.TagUpdate;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.RSQLParameterSyntaxException;
import org.eclipse.hawkbit.repository.exception.RSQLParameterUnsupportedFieldException;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Management service for {@link TargetTag}s.
 */
public interface TargetTagManagement {

    /**
     * Count {@link TargetTag}s.
     *
     * @return size of {@link TargetTag}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    long count();

    /**
     * Creates a new {@link TargetTag}.
     *
     * @param create to be created
     * @return the new created {@link TargetTag}
     * @throws EntityAlreadyExistsException if given object already exists
     * @throws ConstraintViolationException if fields are not filled as specified. Check {@link TagCreate} for field constraints.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_CREATE_TARGET)
    TargetTag create(@NotNull @Valid TagCreate create);

    /**
     * Created multiple {@link TargetTag}s.
     *
     * @param creates to be created
     * @return the new created {@link TargetTag}s
     * @throws EntityAlreadyExistsException if given object has already an ID.
     * @throws ConstraintViolationException if fields are not filled as specified. Check {@link TagCreate} for field constraints.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_CREATE_TARGET)
    List<TargetTag> create(@NotNull @Valid Collection<TagCreate> creates);

    /**
     * Deletes {@link TargetTag} with given name.
     *
     * @param targetTagName tag name of the {@link TargetTag} to be deleted
     * @throws EntityNotFoundException if tag with given name does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_DELETE_TARGET)
    void delete(@NotEmpty String targetTagName);

    /**
     * returns all {@link TargetTag}s.
     *
     * @param pageable page parameter
     * @return all {@link TargetTag}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    Page<TargetTag> findAll(@NotNull Pageable pageable);

    /**
     * Retrieves all target tags based on the given specification.
     *
     * @param rsql rsql query string
     * @param pageable pagination parameter
     * @return the found {@link Target}s, never {@code null}
     * @throws RSQLParameterUnsupportedFieldException if a field in the RSQL string is used but not provided by the given {@code fieldNameProvider}
     * @throws RSQLParameterSyntaxException if the RSQL syntax is wrong
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    Page<TargetTag> findByRsql(@NotNull String rsql, @NotNull Pageable pageable);

    /**
     * Find {@link TargetTag} based on given Name.
     *
     * @param name to look for.
     * @return {@link TargetTag}
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    Optional<TargetTag> getByName(@NotEmpty String name);

    /**
     * Finds {@link TargetTag} by given id.
     *
     * @param id to search for
     * @return the found {@link TargetTag}
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    Optional<TargetTag> get(long id);

    /**
     * Finds {@link TargetTag} by given ids.
     *
     * @param ids the ids to for
     * @return the found {@link TargetTag}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    List<TargetTag> get(@NotEmpty Collection<Long> ids);

    /**
     * Updates the {@link TargetTag}.
     *
     * @param update the {@link TargetTag} with updated values
     * @return the updated {@link TargetTag}
     * @throws EntityNotFoundException in case the {@link TargetTag} does not exist and cannot be updated
     * @throws ConstraintViolationException if fields are not filled as specified. Check {@link TagUpdate} for field constraints.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_TARGET)
    TargetTag update(@NotNull @Valid TagUpdate update);
}