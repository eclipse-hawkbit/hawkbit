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
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Management service for {@link DistributionSetTag}s.
 *
 */
public interface DistributionSetTagManagement {

    /**
     * Creates a {@link DistributionSetTag}.
     *
     * @param create
     *            to be created.
     * @return the new {@link DistributionSet}
     * 
     * @throws EntityAlreadyExistsException
     *             if distributionSetTag already exists
     * @throws ConstraintViolationException
     *             if fields are not filled as specified. Check
     *             {@link TagCreate} for field constraints.
     *
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_CREATE_REPOSITORY)
    DistributionSetTag createDistributionSetTag(@NotNull TagCreate create);

    /**
     * Creates multiple {@link DistributionSetTag}s.
     *
     * @param creates
     *            to be created
     * @return the new {@link DistributionSetTag}
     * 
     * @throws EntityAlreadyExistsException
     *             if a given entity already exists
     * @throws ConstraintViolationException
     *             if fields are not filled as specified. Check
     *             {@link TagCreate} for field constraints.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_CREATE_REPOSITORY)
    List<DistributionSetTag> createDistributionSetTags(@NotNull Collection<TagCreate> creates);

    /**
     * Deletes {@link DistributionSetTag} by given
     * {@link DistributionSetTag#getName()}.
     *
     * @param tagName
     *            to be deleted
     * 
     * @throws EntityNotFoundException
     *             if tag with given name does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_DELETE_REPOSITORY)
    void deleteDistributionSetTag(@NotEmpty String tagName);

    /**
     * returns all {@link DistributionSetTag}s.
     *
     * @param pageReq
     *            page parameter
     * @return all {@link DistributionSetTag}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Page<DistributionSetTag> findAllDistributionSetTags(@NotNull Pageable pageReq);

    /**
     * Retrieves all DistributionSet tags based on the given specification.
     *
     * @param rsqlParam
     *            rsql query string
     * @param pageable
     *            pagination parameter
     * @return the found {@link DistributionSetTag}s, never {@code null}
     * 
     * @throws RSQLParameterUnsupportedFieldException
     *             if a field in the RSQL string is used but not provided by the
     *             given {@code fieldNameProvider}
     * @throws RSQLParameterSyntaxException
     *             if the RSQL syntax is wrong
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Page<DistributionSetTag> findAllDistributionSetTags(@NotNull String rsqlParam, @NotNull Pageable pageable);

    /**
     * Find {@link DistributionSet} based on given name.
     *
     * @param name
     *            to look for.
     * @return {@link DistributionSet}
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Optional<DistributionSetTag> findDistributionSetTag(@NotEmpty String name);

    /**
     * Finds {@link DistributionSetTag} by given id.
     *
     * @param id
     *            to search for
     * @return the found {@link DistributionSetTag}
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Optional<DistributionSetTag> findDistributionSetTagById(@NotNull Long id);

    /**
     * Finds all {@link TargetTag} assigned to given {@link Target}.
     * 
     * @param pageable
     *            information for page size, offset and sort order.
     *
     * @param setId
     *            of the {@link DistributionSet}
     * @return page of the found {@link TargetTag}s
     * 
     * @throws EntityNotFoundException
     *             if {@link DistributionSet} with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Page<DistributionSetTag> findDistributionSetTagsByDistributionSet(@NotNull Pageable pageable, @NotNull Long setId);

    /**
     * Updates an existing {@link DistributionSetTag}.
     *
     * @param update
     *            to be updated
     * 
     * @return the updated {@link DistributionSet}
     * 
     * @throws EntityNotFoundException
     *             in case the {@link DistributionSetTag} does not exists and
     *             cannot be updated
     * @throws ConstraintViolationException
     *             if fields are not filled as specified. Check
     *             {@link TagUpdate} for field constraints.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    DistributionSetTag updateDistributionSetTag(@NotNull TagUpdate update);

}
