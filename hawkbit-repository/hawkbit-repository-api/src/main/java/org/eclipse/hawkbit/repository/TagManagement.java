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

import javax.validation.constraints.NotNull;

import org.eclipse.hawkbit.im.authentication.SpPermission.SpringEvalExpressions;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.exception.RSQLParameterSyntaxException;
import org.eclipse.hawkbit.repository.exception.RSQLParameterUnsupportedFieldException;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.repository.model.Tag;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Management service for {@link Tag}s.
 *
 */
public interface TagManagement {

    /**
     * count {@link TargetTag}s.
     * 
     * @return size of {@link TargetTag}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    long countTargetTags();

    /**
     * Creates a {@link DistributionSet}.
     *
     * @param distributionSetTag
     *            to be created.
     * @return the new {@link DistributionSet}
     * @throws EntityAlreadyExistsException
     *             if distributionSetTag already exists
     *
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_CREATE_REPOSITORY)
    DistributionSetTag createDistributionSetTag(@NotNull DistributionSetTag distributionSetTag);

    /**
     * Creates multiple {@link DistributionSetTag}s.
     *
     * @param distributionSetTags
     *            to be created
     * @return the new {@link DistributionSetTag}
     * @throws EntityAlreadyExistsException
     *             if a given entity already exists
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_CREATE_REPOSITORY)
    List<DistributionSetTag> createDistributionSetTags(@NotNull Collection<DistributionSetTag> distributionSetTags);

    /**
     * Creates a new {@link TargetTag}.
     * 
     * @param targetTag
     *            to be created
     *
     * @return the new created {@link TargetTag}
     *
     * @throws EntityAlreadyExistsException
     *             if given object already exists
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_CREATE_TARGET)
    TargetTag createTargetTag(@NotNull TargetTag targetTag);

    /**
     * created multiple {@link TargetTag}s.
     * 
     * @param targetTags
     *            to be created
     * @return the new created {@link TargetTag}s
     *
     * @throws EntityAlreadyExistsException
     *             if given object has already an ID.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_CREATE_TARGET)
    List<TargetTag> createTargetTags(@NotNull Collection<TargetTag> targetTags);

    /**
     * Deletes {@link DistributionSetTag} by given
     * {@link DistributionSetTag#getName()}.
     *
     * @param tagName
     *            to be deleted
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_DELETE_REPOSITORY)
    void deleteDistributionSetTag(@NotEmpty String tagName);

    /**
     * Deletes {@link TargetTag} with given name.
     * 
     * @param targetTagName
     *            tag name of the {@link TargetTag} to be deleted
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_DELETE_TARGET)
    void deleteTargetTag(@NotEmpty String targetTagName);

    /**
     * 
     * @return all {@link DistributionSetTag}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    List<DistributionSetTag> findAllDistributionSetTags();

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
     * @return all {@link TargetTag}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    List<TargetTag> findAllTargetTags();

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
     * Find {@link DistributionSet} based on given name.
     *
     * @param name
     *            to look for.
     * @return {@link DistributionSet} or <code>null</code> if it does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    DistributionSetTag findDistributionSetTag(@NotEmpty String name);

    /**
     * Finds {@link DistributionSetTag} by given id.
     *
     * @param id
     *            to search for
     * @return the found {@link DistributionSetTag}s or <code>null</code> if not
     *         found.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    DistributionSetTag findDistributionSetTagById(@NotNull Long id);

    /**
     * Find {@link TargetTag} based on given Name.
     *
     * @param name
     *            to look for.
     * @return {@link TargetTag} or <code>null</code> if it does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    TargetTag findTargetTag(@NotEmpty String name);

    /**
     * Finds {@link TargetTag} by given id.
     *
     * @param id
     *            to search for
     * @return the found {@link TargetTag}s or <code>null</code> if not found.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    TargetTag findTargetTagById(@NotNull Long id);

    /**
     * Updates an existing {@link DistributionSetTag}.
     *
     * @param distributionSetTag
     *            to be updated
     * @return the updated {@link DistributionSet}
     * @throws NullPointerException
     *             of {@link DistributionSetTag#getName()} is <code>null</code>
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    DistributionSetTag updateDistributionSetTag(@NotNull DistributionSetTag distributionSetTag);

    /**
     * updates the {@link TargetTag}.
     *
     * @param targetTag
     *            the {@link TargetTag} with updated values
     * @return the updated {@link TargetTag}
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_TARGET)
    TargetTag updateTargetTag(@NotNull TargetTag targetTag);

    /**
     * Generates an empty {@link TargetTag} without persisting it.
     * 
     * @return {@link TargetTag} object
     */
    TargetTag generateTargetTag();

    /**
     * Generates an empty {@link DistributionSetTag} without persisting it.
     * 
     * @return {@link DistributionSetTag} object
     */
    DistributionSetTag generateDistributionSetTag();

    /**
     * Generates a {@link TargetTag} without persisting it.
     * 
     * @param name
     *            of the tag
     * @param description
     *            of the tag
     * @param colour
     *            of the tag
     * @return {@link TargetTag} object
     */
    TargetTag generateTargetTag(String name, String description, String colour);

    /**
     * Generates a {@link TargetTag} without persisting it.
     * 
     * @param name
     *            of the tag
     * @return {@link TargetTag} object
     */
    TargetTag generateTargetTag(String name);

    /**
     * Generates a {@link DistributionSetTag} without persisting it.
     * 
     * @param name
     *            of the tag
     * @param description
     *            of the tag
     * @param colour
     *            of the tag
     * @return {@link DistributionSetTag} object
     */
    DistributionSetTag generateDistributionSetTag(String name, String description, String colour);

    /**
     * Generates a {@link DistributionSetTag} without persisting it.
     * 
     * @param name
     *            of the tag
     * @return {@link DistributionSetTag} object
     */
    DistributionSetTag generateDistributionSetTag(String name);

}
