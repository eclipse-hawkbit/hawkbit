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
import org.eclipse.hawkbit.repository.builder.TagCreate;
import org.eclipse.hawkbit.repository.builder.TagUpdate;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.RSQLParameterSyntaxException;
import org.eclipse.hawkbit.repository.exception.RSQLParameterUnsupportedFieldException;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Management service for {@link TargetTag}s.
 *
 */
public interface TargetTagManagement {

    /**
     * count {@link TargetTag}s.
     * 
     * @return size of {@link TargetTag}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    long countTargetTags();

    /**
     * Creates a new {@link TargetTag}.
     * 
     * @param create
     *            to be created
     *
     * @return the new created {@link TargetTag}
     *
     * @throws EntityAlreadyExistsException
     *             if given object already exists
     * @throws ConstraintViolationException
     *             if fields are not filled as specified. Check
     *             {@link TagCreate} for field constraints.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_CREATE_TARGET)
    TargetTag createTargetTag(@NotNull TagCreate create);

    /**
     * created multiple {@link TargetTag}s.
     * 
     * @param creates
     *            to be created
     * @return the new created {@link TargetTag}s
     *
     * @throws EntityAlreadyExistsException
     *             if given object has already an ID.
     * @throws ConstraintViolationException
     *             if fields are not filled as specified. Check
     *             {@link TagCreate} for field constraints.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_CREATE_TARGET)
    List<TargetTag> createTargetTags(@NotNull Collection<TagCreate> creates);

    /**
     * Deletes {@link TargetTag} with given name.
     * 
     * @param targetTagName
     *            tag name of the {@link TargetTag} to be deleted
     * 
     * @throws EntityNotFoundException
     *             if tag with given name does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_DELETE_TARGET)
    void deleteTargetTag(@NotEmpty String targetTagName);

    /**
     * returns all {@link TargetTag}s.
     * 
     * @param pageable
     *            page parameter
     *
     * @return all {@link TargetTag}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    Page<TargetTag> findAllTargetTags(@NotNull Pageable pageable);

    /**
     * Returns all {@link TargetTag}s assigned to {@link Target} with given ID.
     * 
     * @param pageable
     *            page parameter
     * @param controllerId
     *
     * @return {@link TargetTag}s assigned to {@link Target} with given ID
     * 
     * @throws EntityNotFoundException
     *             if target with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    Page<TargetTag> findAllTargetTags(@NotNull Pageable pageable, @NotEmpty String controllerId);

    /**
     * Retrieves all target tags based on the given specification.
     *
     * @param rsqlParam
     *            rsql query string
     * @param pageable
     *            pagination parameter
     * @return the found {@link Target}s, never {@code null}
     * 
     * @throws RSQLParameterUnsupportedFieldException
     *             if a field in the RSQL string is used but not provided by the
     *             given {@code fieldNameProvider}
     * @throws RSQLParameterSyntaxException
     *             if the RSQL syntax is wrong
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    Page<TargetTag> findAllTargetTags(@NotNull String rsqlParam, @NotNull Pageable pageable);

    /**
     * Find {@link TargetTag} based on given Name.
     *
     * @param name
     *            to look for.
     * @return {@link TargetTag}
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    Optional<TargetTag> findTargetTag(@NotEmpty String name);

    /**
     * Finds {@link TargetTag} by given id.
     *
     * @param id
     *            to search for
     * @return the found {@link TargetTag}
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    Optional<TargetTag> findTargetTagById(@NotNull Long id);

    /**
     * updates the {@link TargetTag}.
     *
     * @param update
     *            the {@link TargetTag} with updated values
     * @return the updated {@link TargetTag}
     * 
     * @throws EntityNotFoundException
     *             in case the {@link TargetTag} does not exists and cannot be
     *             updated
     * @throws ConstraintViolationException
     *             if fields are not filled as specified. Check
     *             {@link TagUpdate} for field constraints.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_TARGET)
    TargetTag updateTargetTag(@NotNull TagUpdate update);

}
