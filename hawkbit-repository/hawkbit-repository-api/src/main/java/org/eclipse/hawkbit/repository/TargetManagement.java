/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

import java.net.URI;
import java.util.Collection;
import java.util.List;

import javax.validation.constraints.NotNull;

import org.eclipse.hawkbit.im.authentication.SpPermission.SpringEvalExpressions;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.exception.RSQLParameterSyntaxException;
import org.eclipse.hawkbit.repository.exception.RSQLParameterUnsupportedFieldException;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Tag;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.eclipse.hawkbit.repository.model.TargetIdName;
import org.eclipse.hawkbit.repository.model.TargetInfo;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.repository.model.TargetTagAssignmentResult;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Management service for {@link Target}s.
 *
 */
public interface TargetManagement {

    /**
     * Assign a {@link TargetTag} assignment to given {@link Target}s.
     *
     * @param targetIds
     *            to assign for
     * @param tag
     *            to assign
     * @return list of assigned targets
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_TARGET)
    List<Target> assignTag(@NotEmpty Collection<String> targetIds, @NotNull TargetTag tag);

    /**
     * Counts number of targets with given
     * {@link Target#getAssignedDistributionSet()}.
     *
     * @param distId
     *            to search for
     *
     * @return number of found {@link Target}s.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    Long countTargetByAssignedDistributionSet(@NotNull Long distId);

    /**
     * Count {@link Target}s for all the given filter parameters.
     *
     * @param status
     *            find targets having on of these {@link TargetUpdateStatus}s.
     *            Set to <code>null</code> in case this is not required.
     * @param searchText
     *            to find targets having the text anywhere in name or
     *            description. Set <code>null</code> in case this is not
     *            required.
     * @param installedOrAssignedDistributionSetId
     *            to find targets having the {@link DistributionSet} as
     *            installed or assigned. Set to <code>null</code> in case this
     *            is not required.
     * @param tagNames
     *            to find targets which are having any one in this tag names.
     *            Set <code>null</code> in case this is not required.
     * @param selectTargetWithNoTag
     *            flag to select targets with no tag assigned
     *
     * @return the found number {@link Target}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    Long countTargetByFilters(Collection<TargetUpdateStatus> status, String searchText,
            Long installedOrAssignedDistributionSetId, Boolean selectTargetWithNoTag, String... tagNames);

    /**
     * Counts number of targets with given
     * {@link TargetInfo#getInstalledDistributionSet()}.
     *
     * @param distId
     *            to search for
     * @return number of found {@link Target}s.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    Long countTargetByInstalledDistributionSet(@NotNull Long distId);

    /**
     * Count {@link TargetFilterQuery}s for given target filter query.
     *
     * @param targetFilterQuery
     *            {link TargetFilterQuery}
     * @return the found number {@link TargetFilterQuery}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    Long countTargetByTargetFilterQuery(@NotEmpty String targetFilterQuery);

    /**
     * Count {@link TargetFilterQuery}s for given filter parameter.
     *
     * @param targetFilterQuery
     *            {link TargetFilterQuery}
     * @return the found number {@link TargetFilterQuery}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    Long countTargetByTargetFilterQuery(@NotNull TargetFilterQuery targetFilterQuery);

    /**
     * Counts all {@link Target}s in the repository.
     *
     * @return number of targets
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    Long countTargetsAll();

    /**
     * creating a new {@link Target}.
     *
     * @param target
     *            to be created
     * @return the created {@link Target}
     *
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_CREATE_TARGET + SpringEvalExpressions.HAS_AUTH_OR
            + SpringEvalExpressions.IS_CONTROLLER)
    Target createTarget(@NotNull Target target);

    /**
     * creating new {@link Target}s including poll status data. useful
     * especially in plug and play scenarios.
     *
     * @param target
     *            to be created *
     * @param status
     *            of the target
     * @param lastTargetQuery
     *            if a plug and play case
     * @param address
     *            if a plug and play case
     *
     * @throws EntityAlreadyExistsException
     *             if {@link Target} with given {@link Target#getControllerId()}
     *             already exists.
     *
     * @return created {@link Target}
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_CREATE_TARGET + SpringEvalExpressions.HAS_AUTH_OR
            + SpringEvalExpressions.IS_CONTROLLER)
    Target createTarget(@NotNull Target target, @NotNull TargetUpdateStatus status, Long lastTargetQuery, URI address);

    /**
     * creates multiple {@link Target}s. If some of the given {@link Target}s
     * already exists in the DB a {@link EntityAlreadyExistsException} is
     * thrown. {@link Target}s contain all objects of the parameter targets,
     * including duplicates.
     *
     * @param targets
     *            to be created.
     * @return the created {@link Target}s
     *
     * @throws EntityAlreadyExistsException
     *             of one of the given targets already exist.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_CREATE_TARGET)
    List<Target> createTargets(@NotNull Collection<Target> targets);

    /**
     * creating a new {@link Target} including poll status data. useful
     * especially in plug and play scenarios.
     *
     * @param targets
     *            to be created *
     * @param status
     *            of the target
     * @param lastTargetQuery
     *            if a plug and play case
     * @param address
     *            if a plug and play case
     *
     * @return newly created target
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_CREATE_TARGET)
    List<Target> createTargets(@NotNull Collection<Target> targets, @NotNull TargetUpdateStatus status,
            Long lastTargetQuery, URI address);

    /**
     * Deletes all targets with the given IDs.
     *
     * @param targetIDs
     *            the technical IDs of the targets to be deleted
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_DELETE_TARGET)
    void deleteTargets(@NotEmpty Long... targetIDs);

    /**
     * finds all {@link Target#getControllerId()} which are currently in the
     * database.
     *
     * @return all IDs of all {@link Target} in the system
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    List<TargetIdName> findAllTargetIds();

    /**
     * Finds all targets for all the given parameters but returns not the full
     * target but {@link TargetIdName}.
     *
     * @param pageRequest
     *            the pageRequest to enhance the query for paging and sorting
     * 
     * @param filterByStatus
     *            find targets having this {@link TargetUpdateStatus}s. Set to
     *            <code>null</code> in case this is not required.
     * @param filterBySearchText
     *            to find targets having the text anywhere in name or
     *            description. Set <code>null</code> in case this is not
     *            required.
     * @param installedOrAssignedDistributionSetId
     *            to find targets having the {@link DistributionSet} as
     *            installed or assigned. Set to <code>null</code> in case this
     *            is not required.
     * @param filterByTagNames
     *            to find targets which are having any one in this tag names.
     *            Set <code>null</code> in case this is not required.
     * @param selectTargetWithNoTag
     *            flag to select targets with no tag assigned
     *
     * @return the found {@link TargetIdName}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    List<TargetIdName> findAllTargetIdsByFilters(@NotNull Pageable pageRequest,
            Collection<TargetUpdateStatus> filterByStatus, String filterBySearchText,
            Long installedOrAssignedDistributionSetId, Boolean selectTargetWithNoTag, String... filterByTagNames);

    /**
     * Finds all targets for all the given parameter {@link TargetFilterQuery}
     * and returns not the full target but {@link TargetIdName}.
     *
     * @param pageRequest
     *            the pageRequest to enhance the query for paging and sorting
     * @param targetFilterQuery
     *            {@link TargetFilterQuery}
     * @return the found {@link TargetIdName}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    List<TargetIdName> findAllTargetIdsByTargetFilterQuery(@NotNull Pageable pageRequest,
            @NotNull TargetFilterQuery targetFilterQuery);

    /**
     * retrieves {@link Target}s by the assigned {@link DistributionSet} without
     * details, i.e. NO {@link Target#getTags()} and {@link Target#getActions()}
     * possible.
     *
     *
     * @param distributionSetID
     *            the ID of the {@link DistributionSet}
     * @param pageReq
     *            page parameter
     * @return the found {@link Target}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY_AND_READ_TARGET)
    Page<Target> findTargetByAssignedDistributionSet(@NotNull Long distributionSetID, @NotNull Pageable pageReq);

    /**
     * Retrieves {@link Target}s by the assigned {@link DistributionSet} without
     * details, i.e. NO {@link Target#getTags()} and {@link Target#getActions()}
     * possible including additional filtering based on the given {@code spec}.
     *
     * @param distributionSetID
     *            the ID of the {@link DistributionSet}
     * @param rsqlParam
     *            the specification to filter the result set
     * @param pageReq
     *            page parameter
     * @return the found {@link Target}s, never {@code null}
     * @throws RSQLParameterUnsupportedFieldException
     *             if a field in the RSQL string is used but not provided by the
     *             given {@code fieldNameProvider}
     * @throws RSQLParameterSyntaxException
     *             if the RSQL syntax is wrong
     * 
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY_AND_READ_TARGET)
    Page<Target> findTargetByAssignedDistributionSet(@NotNull Long distributionSetID, @NotNull String rsqlParam,
            @NotNull Pageable pageReq);

    /**
     * Find {@link Target} based on given ID returns found Target without
     * details, i.e. NO {@link Target#getTags()} and {@link Target#getActions()}
     * possible.
     *
     * @param controllerIDs
     *            to look for.
     * @return List of found{@link Target}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    List<Target> findTargetByControllerID(@NotEmpty Collection<String> controllerIDs);

    /**
     * Find {@link Target} based on given ID returns found Target without
     * details, i.e. NO {@link Target#getTags()} and {@link Target#getActions()}
     * possible.
     *
     * @param controllerId
     *            to look for.
     * @return {@link Target} or <code>null</code> if it does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    Target findTargetByControllerID(@NotEmpty String controllerId);

    /**
     * Find {@link Target} based on given ID returns found Target with details,
     * i.e. {@link Target#getTags()} and {@link Target#getActions()} are
     * possible.
     *
     * Note: try to use {@link #findTargetByControllerID(String)} as much as
     * possible.
     *
     * @param controllerId
     *            to look for.
     * @return {@link Target} or <code>null</code> if it does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    Target findTargetByControllerIDWithDetails(@NotEmpty String controllerId);

    /**
     * Filter {@link Target}s for all the given parameters. If all parameters
     * except pageable are null, all available {@link Target}s are returned.
     *
     * @param pageable
     *            page parameters
     * @param status
     *            find targets having this {@link TargetUpdateStatus}s. Set to
     *            <code>null</code> in case this is not required.
     * @param searchText
     *            to find targets having the text anywhere in name or
     *            description. Set <code>null</code> in case this is not
     *            required.
     * @param installedOrAssignedDistributionSetId
     *            to find targets having the {@link DistributionSet} as
     *            installed or assigned. Set to <code>null</code> in case this
     *            is not required.
     * @param tagNames
     *            to find targets which are having any one in this tag names.
     *            Set <code>null</code> in case this is not required.
     * @param selectTargetWithNoTag
     *            flag to select targets with no tag assigned
     *
     * @return the found {@link Target}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    Slice<Target> findTargetByFilters(@NotNull Pageable pageable, Collection<TargetUpdateStatus> status,
            String searchText, Long installedOrAssignedDistributionSetId, Boolean selectTargetWithNoTag,
            String... tagNames);

    /**
     * retrieves {@link Target}s by the installed {@link DistributionSet}without
     * details, i.e. NO {@link Target#getTags()} and {@link Target#getActions()}
     * possible.
     *
     * @param distributionSetID
     *            the ID of the {@link DistributionSet}
     * @param pageReq
     *            page parameter
     * @return the found {@link Target}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY_AND_READ_TARGET)
    Page<Target> findTargetByInstalledDistributionSet(@NotNull Long distributionSetID, @NotNull Pageable pageReq);

    /**
     * retrieves {@link Target}s by the installed {@link DistributionSet}without
     * details, i.e. NO {@link Target#getTags()} and {@link Target#getActions()}
     * possible including additional filtering based on the given {@code spec}.
     *
     * @param distributionSetId
     *            the ID of the {@link DistributionSet}
     * @param rsqlParam
     *            the specification to filter the result
     * @param pageable
     *            page parameter
     * @return the found {@link Target}s, never {@code null}
     * 
     * @throws RSQLParameterUnsupportedFieldException
     *             if a field in the RSQL string is used but not provided by the
     *             given {@code fieldNameProvider}
     * @throws RSQLParameterSyntaxException
     *             if the RSQL syntax is wrong
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY_AND_READ_TARGET)
    Page<Target> findTargetByInstalledDistributionSet(@NotNull Long distributionSetId, @NotNull String rsqlParam,
            @NotNull Pageable pageable);

    /**
     * Retrieves the {@link Target} which have a certain
     * {@link TargetUpdateStatus} without details, i.e. NO
     * {@link Target#getTags()} and {@link Target#getActions()} possible.
     *
     * @param pageable
     *            page parameter
     * @param status
     *            the {@link TargetUpdateStatus} to be filtered on
     * @return the found {@link Target}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    Page<Target> findTargetByUpdateStatus(@NotNull Pageable pageable, @NotNull TargetUpdateStatus status);

    /**
     * Retrieves all targets without details, i.e. NO {@link Target#getTags()}
     * and {@link Target#getActions()} possible
     *
     * @param pageable
     *            pagination parameter
     * @return the found {@link Target}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    Slice<Target> findTargetsAll(@NotNull Pageable pageable);

    /**
     * Retrieves all targets without details, i.e. NO {@link Target#getTags()}
     * and {@link Target#getActions()} possible based on
     * {@link TargetFilterQuery#getQuery()}
     *
     * @param targetFilterQuery
     *            in string notation
     * @param pageable
     *            pagination parameter
     * @return the found {@link Target}s, never {@code null}
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    Page<Target> findTargetsAll(@NotNull String targetFilterQuery, @NotNull Pageable pageable);

    /**
     * Retrieves all targets without details, i.e. NO {@link Target#getTags()}
     * and {@link Target#getActions()} possible based on
     * {@link TargetFilterQuery#getQuery()}
     *
     * @param targetFilterQuery
     *            the specification for the query
     * @param pageable
     *            pagination parameter
     * 
     * @return the found {@link Target}s, never {@code null}
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    Slice<Target> findTargetsAll(@NotNull TargetFilterQuery targetFilterQuery, @NotNull Pageable pageable);

    /**
     * method retrieves all {@link Target}s from the repo in the following
     * order:
     * <p>
     * 1) {@link Target}s which have the given {@link DistributionSet} as
     * {@link Target#getTargetInfo()}
     * {@link TargetInfo#getInstalledDistributionSet()}
     * <p>
     * 2) {@link Target}s which have the given {@link DistributionSet} as
     * {@link Target#getAssignedDistributionSet()}
     * <p>
     * 3) {@link Target}s which have no connection to the given
     * {@link DistributionSet}.
     *
     * @param pageable
     *            the page request to page the result set
     * @param orderByDistributionId
     *            {@link DistributionSet#getId()} to be ordered by
     * @param filterByDistributionId
     *            {@link DistributionSet#getId()} to be filter the result. Set
     *            to <code>null</code> in case this is not required.
     * @param filterByStatus
     *            find targets having this {@link TargetUpdateStatus}s. Set to
     *            <code>null</code> in case this is not required.
     * @param filterBySearchText
     *            to find targets having the text anywhere in name or
     *            description. Set <code>null</code> in case this is not
     *            required.
     * @param installedOrAssignedDistributionSetId
     *            to find targets having the {@link DistributionSet} as
     *            installed or assigned. Set to <code>null</code> in case this
     *            is not required.
     * @param filterByTagNames
     *            to find targets which are having any one in this tag names.
     *            Set <code>null</code> in case this is not required.
     * @param selectTargetWithNoTag
     *            flag to select targets with no tag assigned
     * @return a paged result {@link Page} of the {@link Target}s in a defined
     *         order.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    Slice<Target> findTargetsAllOrderByLinkedDistributionSet(@NotNull Pageable pageable,
            @NotNull Long orderByDistributionId, Long filterByDistributionId,
            Collection<TargetUpdateStatus> filterByStatus, String filterBySearchText, Boolean selectTargetWithNoTag,
            String... filterByTagNames);

    /**
     * retrieves a list of {@link Target}s by their controller ID with details,
     * i.e. {@link Target#getTags()} are possible.
     *
     * Note: try to use {@link #findTargetByControllerID(String)} as much as
     * possible.
     *
     * @param controllerIDs
     *            {@link Target}s Names parameter
     * @return the found {@link Target}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    List<Target> findTargetsByControllerIDsWithTags(@NotNull List<String> controllerIDs);

    /**
     * Find targets by tag name.
     *
     * @param tagName
     *            tag name
     * @return list of matching targets
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    List<Target> findTargetsByTag(@NotEmpty String tagName);

    /**
     * Toggles {@link TargetTag} assignment to given {@link Target}s by means
     * that if some (or all) of the targets in the list have the {@link Tag} not
     * yet assigned, they will be. If all of theme have the tag already assigned
     * they will be removed instead.
     *
     * @param targetIds
     *            to toggle for
     * @param tagName
     *            to toggle
     * @return TagAssigmentResult with all meta data of the assignment outcome.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_TARGET)
    TargetTagAssignmentResult toggleTagAssignment(@NotEmpty Collection<String> targetIds, @NotEmpty String tagName);

    /**
     * {@link Target} based method call for
     * {@link #toggleTagAssignment(Collection, String)}.
     *
     * @param targets
     *            to toggle for
     * @param tag
     *            to toggle
     * @return TagAssigmentResult with all meta data of the assignment outcome.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_TARGET)
    TargetTagAssignmentResult toggleTagAssignment(@NotEmpty Collection<Target> targets, @NotNull TargetTag tag);

    /**
     * Un-assign all {@link Target} from a given {@link TargetTag} .
     *
     * @param tag
     *            to un-assign all targets
     * @return list of unassigned targets
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_TARGET)
    List<Target> unAssignAllTargetsByTag(@NotNull TargetTag tag);

    /**
     * Un-assign a {@link TargetTag} assignment to given {@link Target}.
     *
     * @param controllerID
     *            to un-assign for
     * @param targetTag
     *            to un-assign
     * @return the unassigned target or <null> if no target is unassigned
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_TARGET)
    Target unAssignTag(@NotEmpty String controllerID, @NotNull TargetTag targetTag);

    /**
     * updates the {@link Target}.
     *
     * @param target
     *            to be updated
     * @return the updated {@link Target}
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_TARGET + SpringEvalExpressions.HAS_AUTH_OR
            + SpringEvalExpressions.IS_CONTROLLER)
    Target updateTarget(@NotNull Target target);

    /**
     * updates multiple {@link Target}s.
     *
     * @param targets
     *            to be updated
     * @return the updated {@link Target}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_TARGET + SpringEvalExpressions.HAS_AUTH_OR
            + SpringEvalExpressions.IS_CONTROLLER)
    List<Target> updateTargets(@NotNull Collection<Target> targets);

    /**
     * Generates an empty {@link Target} without persisting it.
     * 
     * @param controllerID
     *            of the {@link Target}
     * 
     * @return {@link Target} object
     */
    Target generateTarget(@NotEmpty String controllerID);

}