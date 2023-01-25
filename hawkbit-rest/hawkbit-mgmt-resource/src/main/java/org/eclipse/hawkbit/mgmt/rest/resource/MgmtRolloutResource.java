/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.rest.resource;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.validation.ValidationException;

import org.eclipse.hawkbit.mgmt.json.model.PagedList;
import org.eclipse.hawkbit.mgmt.json.model.rollout.MgmtRolloutResponseBody;
import org.eclipse.hawkbit.mgmt.json.model.rollout.MgmtRolloutRestRequestBody;
import org.eclipse.hawkbit.mgmt.json.model.rolloutgroup.MgmtRolloutGroup;
import org.eclipse.hawkbit.mgmt.json.model.rolloutgroup.MgmtRolloutGroupResponseBody;
import org.eclipse.hawkbit.mgmt.json.model.target.MgmtTarget;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRepresentationMode;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRolloutRestApi;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.OffsetBasedPageRequest;
import org.eclipse.hawkbit.repository.RolloutGroupManagement;
import org.eclipse.hawkbit.repository.RolloutManagement;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.builder.RolloutCreate;
import org.eclipse.hawkbit.repository.builder.RolloutGroupCreate;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.RSQLParameterSyntaxException;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.RolloutGroupConditions;
import org.eclipse.hawkbit.repository.model.Target;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.eclipse.hawkbit.utils.TenantConfigHelper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Resource handling rollout CRUD operations.
 *
 */
@RestController
public class MgmtRolloutResource implements MgmtRolloutRestApi {

    private static final Logger LOG = LoggerFactory.getLogger(MgmtRolloutResource.class);

    private final RolloutManagement rolloutManagement;

    private final RolloutGroupManagement rolloutGroupManagement;

    private final DistributionSetManagement distributionSetManagement;

    private final TargetFilterQueryManagement targetFilterQueryManagement;

    private final EntityFactory entityFactory;
    private final TenantConfigHelper tenantConfigHelper;

    MgmtRolloutResource(final RolloutManagement rolloutManagement, final RolloutGroupManagement rolloutGroupManagement,
            final DistributionSetManagement distributionSetManagement,
            final TargetFilterQueryManagement targetFilterQueryManagement, final EntityFactory entityFactory,
            final SystemSecurityContext systemSecurityContext,
            final TenantConfigurationManagement tenantConfigurationManagement) {
        this.rolloutManagement = rolloutManagement;
        this.rolloutGroupManagement = rolloutGroupManagement;
        this.distributionSetManagement = distributionSetManagement;
        this.targetFilterQueryManagement = targetFilterQueryManagement;
        this.entityFactory = entityFactory;
        this.tenantConfigHelper = TenantConfigHelper.usingContext(systemSecurityContext, tenantConfigurationManagement);
    }

    @Override
    public ResponseEntity<PagedList<MgmtRolloutResponseBody>> getRollouts(
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) final int pagingOffsetParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) final int pagingLimitParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false) final String sortParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false) final String rsqlParam,
            final String representationModeParam) {

        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeRolloutSortParam(sortParam);

        final Pageable pageable = new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting);
        final Page<Rollout> findRolloutsAll;
        if (rsqlParam != null) {
            findRolloutsAll = this.rolloutManagement.findByRsql(pageable, rsqlParam, false);
        } else {
            findRolloutsAll = this.rolloutManagement.findAll(pageable, false);
        }

        final MgmtRepresentationMode repMode = MgmtRepresentationMode.fromValue(representationModeParam)
                .orElseGet(() -> {
                    // no need for a 400, just apply a safe fallback
                    LOG.warn("Received an invalid representation mode: {}", representationModeParam);
                    return MgmtRepresentationMode.COMPACT;
                });

        final List<MgmtRolloutResponseBody> rest = MgmtRolloutMapper.toResponseRollout(findRolloutsAll.getContent(),
                repMode == MgmtRepresentationMode.FULL);
        return ResponseEntity.ok(new PagedList<>(rest, findRolloutsAll.getTotalElements()));
    }

    @Override
    public ResponseEntity<MgmtRolloutResponseBody> getRollout(@PathVariable("rolloutId") final Long rolloutId) {
        final Rollout findRolloutById = rolloutManagement.getWithDetailedStatus(rolloutId)
                .orElseThrow(() -> new EntityNotFoundException(Rollout.class, rolloutId));

        return ResponseEntity.ok(MgmtRolloutMapper.toResponseRollout(findRolloutById, true));
    }

    @Override
    public ResponseEntity<MgmtRolloutResponseBody> create(
            @RequestBody final MgmtRolloutRestRequestBody rolloutRequestBody) {

        // first check the given RSQL query if it's well formed, otherwise and
        // exception is thrown
        final String targetFilterQuery = rolloutRequestBody.getTargetFilterQuery();
        if (targetFilterQuery == null) {
            // Use RSQLParameterSyntaxException due to backwards compatibility
            throw new RSQLParameterSyntaxException("Cannot create a Rollout with an empty target query filter!");
        }
        targetFilterQueryManagement.verifyTargetFilterQuerySyntax(targetFilterQuery);
        final DistributionSet distributionSet = distributionSetManagement
                .getValidAndComplete(rolloutRequestBody.getDistributionSetId());
        final RolloutGroupConditions rolloutGroupConditions = MgmtRolloutMapper.fromRequest(rolloutRequestBody, true);
        final RolloutCreate create = MgmtRolloutMapper.fromRequest(entityFactory, rolloutRequestBody, distributionSet);
        final boolean confirmationFlowActive = tenantConfigHelper.isConfirmationFlowEnabled();

        Rollout rollout;
        if (rolloutRequestBody.getGroups() != null) {
            final List<RolloutGroupCreate> rolloutGroups = rolloutRequestBody.getGroups().stream()
                    .map(mgmtRolloutGroup -> {
                        final boolean confirmationRequired = isConfirmationRequiredForGroup(mgmtRolloutGroup,
                                rolloutRequestBody).orElse(confirmationFlowActive);
                        return MgmtRolloutMapper.fromRequest(entityFactory, mgmtRolloutGroup)
                                .confirmationRequired(confirmationRequired);
                    }).collect(Collectors.toList());
            rollout = rolloutManagement.create(create, rolloutGroups, rolloutGroupConditions);

        } else if (rolloutRequestBody.getAmountGroups() != null) {
            final boolean confirmationRequired = rolloutRequestBody.isConfirmationRequired() == null
                    ? confirmationFlowActive
                    : rolloutRequestBody.isConfirmationRequired();
            rollout = rolloutManagement.create(create, rolloutRequestBody.getAmountGroups(), confirmationRequired,
                    rolloutGroupConditions);

        } else {
            throw new ValidationException("Either 'amountGroups' or 'groups' must be defined in the request");
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(MgmtRolloutMapper.toResponseRollout(rollout, true));
    }

    private Optional<Boolean> isConfirmationRequiredForGroup(final MgmtRolloutGroup group,
            final MgmtRolloutRestRequestBody request) {
        if (group.isConfirmationRequired() != null) {
            return Optional.of(group.isConfirmationRequired());
        } else if (request.isConfirmationRequired() != null) {
            return Optional.of(request.isConfirmationRequired());
        }
        return Optional.empty();
    }

    @Override
    public ResponseEntity<Void> approve(@PathVariable("rolloutId") final Long rolloutId, final String remark) {
        rolloutManagement.approveOrDeny(rolloutId, Rollout.ApprovalDecision.APPROVED, remark);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> deny(@PathVariable("rolloutId") final Long rolloutId, final String remark) {
        rolloutManagement.approveOrDeny(rolloutId, Rollout.ApprovalDecision.DENIED, remark);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> start(@PathVariable("rolloutId") final Long rolloutId) {
        this.rolloutManagement.start(rolloutId);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> pause(@PathVariable("rolloutId") final Long rolloutId) {
        this.rolloutManagement.pauseRollout(rolloutId);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> delete(@PathVariable("rolloutId") final Long rolloutId) {
        this.rolloutManagement.delete(rolloutId);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> resume(@PathVariable("rolloutId") final Long rolloutId) {
        this.rolloutManagement.resumeRollout(rolloutId);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<PagedList<MgmtRolloutGroupResponseBody>> getRolloutGroups(
            @PathVariable("rolloutId") final Long rolloutId,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) final int pagingOffsetParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) final int pagingLimitParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false) final String sortParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false) final String rsqlParam) {
        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeRolloutGroupSortParam(sortParam);

        final Pageable pageable = new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting);

        final Page<RolloutGroup> findRolloutGroupsAll;
        if (rsqlParam != null) {
            findRolloutGroupsAll = this.rolloutGroupManagement.findByRolloutAndRsql(pageable, rolloutId, rsqlParam);
        } else {
            findRolloutGroupsAll = this.rolloutGroupManagement.findByRollout(pageable, rolloutId);
        }

        final List<MgmtRolloutGroupResponseBody> rest = MgmtRolloutMapper
                .toResponseRolloutGroup(findRolloutGroupsAll.getContent(), tenantConfigHelper.isConfirmationFlowEnabled());
        return ResponseEntity.ok(new PagedList<>(rest, findRolloutGroupsAll.getTotalElements()));
    }

    @Override
    public ResponseEntity<MgmtRolloutGroupResponseBody> getRolloutGroup(@PathVariable("rolloutId") final Long rolloutId,
            @PathVariable("groupId") final Long groupId) {
        findRolloutOrThrowException(rolloutId);

        final RolloutGroup rolloutGroup = rolloutGroupManagement.getWithDetailedStatus(groupId)
                .orElseThrow(() -> new EntityNotFoundException(RolloutGroup.class, rolloutId));
        return ResponseEntity.ok(MgmtRolloutMapper.toResponseRolloutGroup(rolloutGroup, true,
                tenantConfigHelper.isConfirmationFlowEnabled()));
    }

    private void findRolloutOrThrowException(final Long rolloutId) {
        if (!rolloutManagement.exists(rolloutId)) {
            throw new EntityNotFoundException(Rollout.class, rolloutId);
        }
    }

    @Override
    public ResponseEntity<PagedList<MgmtTarget>> getRolloutGroupTargets(@PathVariable("rolloutId") final Long rolloutId,
            @PathVariable("groupId") final Long groupId,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) final int pagingOffsetParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) final int pagingLimitParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false) final String sortParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false) final String rsqlParam) {
        findRolloutOrThrowException(rolloutId);
        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeTargetSortParam(sortParam);

        final Pageable pageable = new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting);

        final Page<Target> rolloutGroupTargets;
        if (rsqlParam != null) {
            rolloutGroupTargets = this.rolloutGroupManagement.findTargetsOfRolloutGroupByRsql(pageable, groupId,
                    rsqlParam);
        } else {
            final Page<Target> pageTargets = this.rolloutGroupManagement.findTargetsOfRolloutGroup(pageable, groupId);
            rolloutGroupTargets = pageTargets;
        }
        final List<MgmtTarget> rest = MgmtTargetMapper.toResponse(rolloutGroupTargets.getContent(), tenantConfigHelper);
        return ResponseEntity.ok(new PagedList<>(rest, rolloutGroupTargets.getTotalElements()));
    }

    @Override
    public ResponseEntity<Void> triggerNextGroup(@PathVariable("rolloutId") final Long rolloutId) {
        this.rolloutManagement.triggerNextGroup(rolloutId);
        return ResponseEntity.ok().build();
    }
}
