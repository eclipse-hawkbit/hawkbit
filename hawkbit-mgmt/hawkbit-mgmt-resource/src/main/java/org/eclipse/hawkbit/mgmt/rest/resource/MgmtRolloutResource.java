/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.rest.resource;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.validation.ValidationException;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.mgmt.json.model.PagedList;
import org.eclipse.hawkbit.mgmt.json.model.rollout.MgmtRolloutResponseBody;
import org.eclipse.hawkbit.mgmt.json.model.rollout.MgmtRolloutRestRequestBodyPost;
import org.eclipse.hawkbit.mgmt.json.model.rollout.MgmtRolloutRestRequestBodyPut;
import org.eclipse.hawkbit.mgmt.json.model.rolloutgroup.MgmtRolloutGroup;
import org.eclipse.hawkbit.mgmt.json.model.rolloutgroup.MgmtRolloutGroupResponseBody;
import org.eclipse.hawkbit.mgmt.json.model.target.MgmtTarget;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRepresentationMode;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRolloutRestApi;
import org.eclipse.hawkbit.mgmt.rest.resource.util.PagingUtility;
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
import org.eclipse.hawkbit.repository.model.RolloutGroupConditionBuilder;
import org.eclipse.hawkbit.repository.model.RolloutGroupConditions;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.eclipse.hawkbit.utils.TenantConfigHelper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Resource handling rollout CRUD operations.
 */
@Slf4j
@RestController
public class MgmtRolloutResource implements MgmtRolloutRestApi {

    private final RolloutManagement rolloutManagement;
    private final RolloutGroupManagement rolloutGroupManagement;
    private final DistributionSetManagement distributionSetManagement;
    private final TargetFilterQueryManagement targetFilterQueryManagement;
    private final EntityFactory entityFactory;
    private final TenantConfigHelper tenantConfigHelper;

    MgmtRolloutResource(
            final RolloutManagement rolloutManagement, final RolloutGroupManagement rolloutGroupManagement,
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
            final int pagingOffsetParam, final int pagingLimitParam, final String sortParam, final String rsqlParam,
            final String representationModeParam) {
        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeRolloutSortParam(sortParam);

        final boolean isFullMode = parseRepresentationMode(representationModeParam) == MgmtRepresentationMode.FULL;

        final Pageable pageable = new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting);
        final Page<Rollout> rollouts;
        if (rsqlParam != null) {
            rollouts = this.rolloutManagement.findByRsql(pageable, rsqlParam, false);
        } else {
            rollouts = this.rolloutManagement.findAll(pageable, false);
        }

        final long totalElements = rollouts.getTotalElements();

        if (isFullMode) {
            this.rolloutManagement.setRolloutStatusDetails(rollouts);
        }

        final List<MgmtRolloutResponseBody> rest = MgmtRolloutMapper.toResponseRollout(rollouts.getContent(),
                isFullMode);

        return ResponseEntity.ok(new PagedList<>(rest, totalElements));
    }

    @Override
    public ResponseEntity<MgmtRolloutResponseBody> getRollout(final Long rolloutId) {
        final Rollout findRolloutById = rolloutManagement.getWithDetailedStatus(rolloutId)
                .orElseThrow(() -> new EntityNotFoundException(Rollout.class, rolloutId));

        return ResponseEntity.ok(MgmtRolloutMapper.toResponseRollout(findRolloutById, true));
    }

    @Override
    public ResponseEntity<MgmtRolloutResponseBody> create(final MgmtRolloutRestRequestBodyPost rolloutRequestBody) {
        // first check the given RSQL query if it's well-formed, otherwise and exception is thrown
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

        final Rollout rollout;
        if (rolloutRequestBody.getGroups() != null) {
            if (rolloutRequestBody.isDynamic()) {
                throw new ValidationException("Dynamic rollouts are not supported with groups");
            }
            if (rolloutRequestBody.getAmountGroups() != null) {
                throw new ValidationException("Either 'amountGroups' or 'groups' must be defined in the request");
            }
            final List<RolloutGroupCreate> rolloutGroups = rolloutRequestBody.getGroups().stream()
                    .map(mgmtRolloutGroup -> {
                        final boolean confirmationRequired = isConfirmationRequiredForGroup(mgmtRolloutGroup,
                                rolloutRequestBody).orElse(confirmationFlowActive);
                        return MgmtRolloutMapper.fromRequest(entityFactory, mgmtRolloutGroup)
                                .confirmationRequired(confirmationRequired);
                    }).collect(Collectors.toList());
            rollout = rolloutManagement.create(create, rolloutGroups, rolloutGroupConditions);
        } else if (rolloutRequestBody.getAmountGroups() != null) {
            final boolean confirmationRequired = rolloutRequestBody.getConfirmationRequired() == null
                    ? confirmationFlowActive
                    : rolloutRequestBody.getConfirmationRequired();
            rollout = rolloutManagement.create(create, rolloutRequestBody.getAmountGroups(), confirmationRequired,
                    rolloutGroupConditions, MgmtRolloutMapper.fromRequest(rolloutRequestBody.getDynamicGroupTemplate()));
        } else {
            throw new ValidationException("Either 'amountGroups' or 'groups' must be defined in the request");
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(MgmtRolloutMapper.toResponseRollout(rollout, true));
    }

    @Override
    public ResponseEntity<MgmtRolloutResponseBody> update(final Long rolloutId, final MgmtRolloutRestRequestBodyPut rolloutUpdateBody) {
        final Rollout updated = rolloutManagement.update(MgmtRolloutMapper.fromRequest(entityFactory, rolloutUpdateBody, rolloutId));
        return ResponseEntity.ok(MgmtRolloutMapper.toResponseRollout(updated, true));
    }

    @Override
    public ResponseEntity<Void> approve(final Long rolloutId, final String remark) {
        rolloutManagement.approveOrDeny(rolloutId, Rollout.ApprovalDecision.APPROVED, remark);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> deny(final Long rolloutId, final String remark) {
        rolloutManagement.approveOrDeny(rolloutId, Rollout.ApprovalDecision.DENIED, remark);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> start(final Long rolloutId) {
        this.rolloutManagement.start(rolloutId);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> pause(final Long rolloutId) {
        this.rolloutManagement.pauseRollout(rolloutId);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> delete(final Long rolloutId) {
        this.rolloutManagement.delete(rolloutId);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> resume(final Long rolloutId) {
        this.rolloutManagement.resumeRollout(rolloutId);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<PagedList<MgmtRolloutGroupResponseBody>> getRolloutGroups(
            final Long rolloutId,
            final int pagingOffsetParam, final int pagingLimitParam, final String sortParam, final String rsqlParam,
            final String representationModeParam) {
        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeRolloutGroupSortParam(sortParam);

        final boolean isFullMode = parseRepresentationMode(representationModeParam) == MgmtRepresentationMode.FULL;

        final Pageable pageable = new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting);

        final Page<RolloutGroup> rolloutGroups;
        if (rsqlParam != null) {
            if (isFullMode) {
                rolloutGroups = this.rolloutGroupManagement.findByRolloutAndRsqlWithDetailedStatus(pageable,
                        rolloutId, rsqlParam);
            } else {
                rolloutGroups = this.rolloutGroupManagement.findByRolloutAndRsql(pageable, rolloutId, rsqlParam);
            }
        } else {
            if (isFullMode) {
                rolloutGroups = this.rolloutGroupManagement.findByRolloutWithDetailedStatus(pageable, rolloutId);
            } else {
                rolloutGroups = this.rolloutGroupManagement.findByRollout(pageable, rolloutId);
            }
        }

        final List<MgmtRolloutGroupResponseBody> rest = MgmtRolloutMapper.toResponseRolloutGroup(
                rolloutGroups.getContent(), tenantConfigHelper.isConfirmationFlowEnabled(), isFullMode);
        return ResponseEntity.ok(new PagedList<>(rest, rolloutGroups.getTotalElements()));
    }

    @Override
    public ResponseEntity<MgmtRolloutGroupResponseBody> getRolloutGroup(final Long rolloutId, final Long groupId) {
        findRolloutOrThrowException(rolloutId);

        final RolloutGroup rolloutGroup = rolloutGroupManagement.getWithDetailedStatus(groupId)
                .orElseThrow(() -> new EntityNotFoundException(RolloutGroup.class, rolloutId));

        if (!Objects.equals(rolloutId, rolloutGroup.getRollout().getId())) {
            throw new EntityNotFoundException(RolloutGroup.class, groupId);
        }

        return ResponseEntity.ok(MgmtRolloutMapper.toResponseRolloutGroup(
                rolloutGroup, true, tenantConfigHelper.isConfirmationFlowEnabled()));
    }

    @Override
    public ResponseEntity<PagedList<MgmtTarget>> getRolloutGroupTargets(
            final Long rolloutId, final Long groupId,
            final int pagingOffsetParam, final int pagingLimitParam, final String sortParam, final String rsqlParam) {
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
    public ResponseEntity<Void> triggerNextGroup(final Long rolloutId) {
        this.rolloutManagement.triggerNextGroup(rolloutId);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<MgmtRolloutResponseBody> retryRollout(final Long rolloutId) {
        final Rollout rolloutForRetry = this.rolloutManagement.get(rolloutId)
                .orElseThrow(() -> new EntityNotFoundException(Rollout.class, rolloutId));

        if (rolloutForRetry.isDeleted()) {
            throw new EntityNotFoundException(Rollout.class, rolloutId);
        }

        if (!rolloutForRetry.getStatus().equals(Rollout.RolloutStatus.FINISHED)) {
            throw new ValidationException("Rollout must be finished in order to be retried!");
        }

        final RolloutCreate create = MgmtRolloutMapper.fromRetriedRollout(entityFactory, rolloutForRetry);
        final RolloutGroupConditions groupConditions = new RolloutGroupConditionBuilder().withDefaults().build();

        final Rollout retriedRollout = rolloutManagement.create(create, 1, false, groupConditions, null);

        return ResponseEntity.status(HttpStatus.CREATED).body(MgmtRolloutMapper.toResponseRollout(retriedRollout, true));
    }

    private static MgmtRepresentationMode parseRepresentationMode(final String representationModeParam) {
        return MgmtRepresentationMode.fromValue(representationModeParam).orElseGet(() -> {
            // no need for a 400, just apply a safe fallback
            log.warn("Received an invalid representation mode: {}", representationModeParam);
            return MgmtRepresentationMode.COMPACT;
        });
    }

    private Optional<Boolean> isConfirmationRequiredForGroup(final MgmtRolloutGroup group, final MgmtRolloutRestRequestBodyPost request) {
        if (group.getConfirmationRequired() != null) {
            return Optional.of(group.getConfirmationRequired());
        } else if (request.getConfirmationRequired() != null) {
            return Optional.of(request.getConfirmationRequired());
        }
        return Optional.empty();
    }

    private void findRolloutOrThrowException(final Long rolloutId) {
        if (!rolloutManagement.exists(rolloutId)) {
            throw new EntityNotFoundException(Rollout.class, rolloutId);
        }
    }
}