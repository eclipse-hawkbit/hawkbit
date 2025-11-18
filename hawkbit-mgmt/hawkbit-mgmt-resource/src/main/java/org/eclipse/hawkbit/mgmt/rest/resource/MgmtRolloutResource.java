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

import static org.eclipse.hawkbit.mgmt.rest.resource.util.PagingUtility.sanitizeRolloutSortParam;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import jakarta.validation.ValidationException;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.audit.AuditLog;
import org.eclipse.hawkbit.mgmt.json.model.PagedList;
import org.eclipse.hawkbit.mgmt.json.model.rollout.MgmtRolloutResponseBody;
import org.eclipse.hawkbit.mgmt.json.model.rollout.MgmtRolloutRestRequestBodyPost;
import org.eclipse.hawkbit.mgmt.json.model.rollout.MgmtRolloutRestRequestBodyPut;
import org.eclipse.hawkbit.mgmt.json.model.rolloutgroup.MgmtRolloutGroup;
import org.eclipse.hawkbit.mgmt.json.model.rolloutgroup.MgmtRolloutGroupResponseBody;
import org.eclipse.hawkbit.mgmt.json.model.target.MgmtTarget;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRepresentationMode;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRolloutRestApi;
import org.eclipse.hawkbit.mgmt.rest.resource.mapper.MgmtRolloutMapper;
import org.eclipse.hawkbit.mgmt.rest.resource.mapper.MgmtTargetMapper;
import org.eclipse.hawkbit.mgmt.rest.resource.util.PagingUtility;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.RolloutGroupManagement;
import org.eclipse.hawkbit.repository.RolloutManagement;
import org.eclipse.hawkbit.repository.RolloutManagement.Create;
import org.eclipse.hawkbit.repository.RolloutManagement.GroupCreate;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.RSQLParameterSyntaxException;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.RolloutGroupConditionBuilder;
import org.eclipse.hawkbit.repository.model.RolloutGroupConditions;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.eclipse.hawkbit.utils.TenantConfigHelper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    private final DistributionSetManagement<? extends DistributionSet> distributionSetManagement;
    private final TargetFilterQueryManagement<? extends TargetFilterQuery> targetFilterQueryManagement;
    private final TenantConfigHelper tenantConfigHelper;

    MgmtRolloutResource(
            final RolloutManagement rolloutManagement, final RolloutGroupManagement rolloutGroupManagement,
            final DistributionSetManagement<? extends DistributionSet> distributionSetManagement,
            final TargetFilterQueryManagement<? extends TargetFilterQuery> targetFilterQueryManagement,
            final SystemSecurityContext systemSecurityContext,
            final TenantConfigurationManagement tenantConfigurationManagement) {
        this.rolloutManagement = rolloutManagement;
        this.rolloutGroupManagement = rolloutGroupManagement;
        this.distributionSetManagement = distributionSetManagement;
        this.targetFilterQueryManagement = targetFilterQueryManagement;
        this.tenantConfigHelper = TenantConfigHelper.usingContext(systemSecurityContext, tenantConfigurationManagement);
    }

    @Override
    public ResponseEntity<PagedList<MgmtRolloutResponseBody>> getRollouts(
            final String rsqlParam, final int pagingOffsetParam, final int pagingLimitParam, final String sortParam,
            final String representationModeParam) {
        final Pageable pageable = PagingUtility.toPageable(pagingOffsetParam, pagingLimitParam, sanitizeRolloutSortParam(sortParam));
        final boolean isFullMode = parseRepresentationMode(representationModeParam) == MgmtRepresentationMode.FULL;

        final Page<Rollout> rollouts;
        final List<MgmtRolloutResponseBody> rest;
        if (isFullMode) {
            rollouts = rsqlParam == null
                    ? rolloutManagement.findAllWithDetailedStatus(false, pageable)
                    : rolloutManagement.findByRsqlWithDetailedStatus(rsqlParam, false, pageable);
            rest = MgmtRolloutMapper.toResponseRolloutWithDetails(rollouts.getContent());
        } else {
            rollouts = rsqlParam == null
                    ? rolloutManagement.findAll(false, pageable)
                    : rolloutManagement.findByRsql(rsqlParam, false, pageable);
            rest = MgmtRolloutMapper.toResponseRollout(rollouts.getContent());
        }
        return ResponseEntity.ok(new PagedList<>(rest, rollouts.getTotalElements()));
    }

    @Override
    public ResponseEntity<MgmtRolloutResponseBody> getRollout(final Long rolloutId) {
        return ResponseEntity.ok(MgmtRolloutMapper.toResponseRollout(rolloutManagement.getWithDetailedStatus(rolloutId), true));
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
        final DistributionSet distributionSet = distributionSetManagement.getValidAndComplete(rolloutRequestBody.getDistributionSetId());
        final RolloutGroupConditions rolloutGroupConditions = MgmtRolloutMapper.fromRequest(rolloutRequestBody, true);
        final Create create = MgmtRolloutMapper.fromRequest(rolloutRequestBody, distributionSet);
        final boolean confirmationFlowActive = tenantConfigHelper.isConfirmationFlowEnabled();

        final Rollout rollout;
        if (rolloutRequestBody.getGroups() != null) {
            if (rolloutRequestBody.isDynamic()) {
                throw new ValidationException("Dynamic rollouts are not supported with groups");
            }
            if (rolloutRequestBody.getAmountGroups() != null) {
                throw new ValidationException("If 'group' is set the 'amountGroups' must not be set in the request");
            }
            final List<GroupCreate> rolloutGroups = rolloutRequestBody.getGroups().stream()
                    .map(mgmtRolloutGroup -> MgmtRolloutMapper.fromRequest(
                            mgmtRolloutGroup,
                            isConfirmationRequiredForGroup(mgmtRolloutGroup, rolloutRequestBody).orElse(confirmationFlowActive)))
                    .toList();
            rollout = rolloutManagement.create(create, rolloutGroups, rolloutGroupConditions);
        } else {
            final int amountGroup = Optional.ofNullable(rolloutRequestBody.getAmountGroups()).orElse(1);
            final boolean confirmationRequired = rolloutRequestBody.getConfirmationRequired() == null
                    ? confirmationFlowActive
                    : rolloutRequestBody.getConfirmationRequired();
            rollout = rolloutManagement.create(create, amountGroup, confirmationRequired,
                    rolloutGroupConditions, MgmtRolloutMapper.fromRequest(rolloutRequestBody.getDynamicGroupTemplate()));
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(MgmtRolloutMapper.toResponseRollout(rollout, true));
    }

    @Override
    public ResponseEntity<MgmtRolloutResponseBody> update(final Long rolloutId, final MgmtRolloutRestRequestBodyPut rolloutUpdateBody) {
        final Rollout updated = rolloutManagement.update(MgmtRolloutMapper.fromRequest(rolloutUpdateBody, rolloutId));
        return ResponseEntity.ok(MgmtRolloutMapper.toResponseRollout(updated, true));
    }

    @Override
    public ResponseEntity<Void> approve(final Long rolloutId, final String remark) {
        rolloutManagement.approveOrDeny(rolloutId, Rollout.ApprovalDecision.APPROVED, remark);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deny(final Long rolloutId, final String remark) {
        rolloutManagement.approveOrDeny(rolloutId, Rollout.ApprovalDecision.DENIED, remark);
        return ResponseEntity.noContent().build();
    }

    @Override
    @AuditLog(entity = "Rollout", type = AuditLog.Type.UPDATE, description = "Start Rollout")
    public ResponseEntity<Void> start(final Long rolloutId) {
        this.rolloutManagement.start(rolloutId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @AuditLog(entity = "Rollout", type = AuditLog.Type.UPDATE, description = "Pause Rollout")
    public ResponseEntity<Void> pause(final Long rolloutId) {
        this.rolloutManagement.pauseRollout(rolloutId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @AuditLog(entity = "Rollout", type = AuditLog.Type.UPDATE, description = "Stop Rollout")
    public ResponseEntity<Void> stop(Long rolloutId) {
        this.rolloutManagement.stop(rolloutId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @AuditLog(entity = "Rollout", type = AuditLog.Type.DELETE, description = "Delete Rollout")
    public ResponseEntity<Void> delete(final Long rolloutId) {
        this.rolloutManagement.delete(rolloutId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @AuditLog(entity = "Rollout", type = AuditLog.Type.UPDATE, description = "Resume Rollout")
    public ResponseEntity<Void> resume(final Long rolloutId) {
        this.rolloutManagement.resumeRollout(rolloutId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<PagedList<MgmtRolloutGroupResponseBody>> getRolloutGroups(
            final Long rolloutId,
            final String rsqlParam, final int pagingOffsetParam, final int pagingLimitParam, final String sortParam,
            final String representationModeParam) {
        final Pageable pageable = PagingUtility.toPageable(pagingOffsetParam, pagingLimitParam, sanitizeRolloutSortParam(sortParam));
        final boolean isFullMode = parseRepresentationMode(representationModeParam) == MgmtRepresentationMode.FULL;

        final Page<RolloutGroup> rolloutGroups;
        if (rsqlParam == null) {
            if (isFullMode) {
                rolloutGroups = this.rolloutGroupManagement.findByRolloutWithDetailedStatus(rolloutId, pageable);
            } else {
                rolloutGroups = this.rolloutGroupManagement.findByRollout(rolloutId, pageable);
            }
        } else {
            if (isFullMode) {
                rolloutGroups = this.rolloutGroupManagement.findByRolloutAndRsqlWithDetailedStatus(rolloutId, rsqlParam, pageable);
            } else {
                rolloutGroups = this.rolloutGroupManagement.findByRolloutAndRsql(rolloutId, rsqlParam, pageable);
            }
        }

        final List<MgmtRolloutGroupResponseBody> rest = MgmtRolloutMapper.toResponseRolloutGroup(
                rolloutGroups.getContent(), tenantConfigHelper.isConfirmationFlowEnabled(), isFullMode);
        return ResponseEntity.ok(new PagedList<>(rest, rolloutGroups.getTotalElements()));
    }

    @Override
    public ResponseEntity<MgmtRolloutGroupResponseBody> getRolloutGroup(final Long rolloutId, final Long groupId) {
        findRolloutOrThrowException(rolloutId);

        final RolloutGroup rolloutGroup = rolloutGroupManagement.getWithDetailedStatus(groupId);
        if (!Objects.equals(rolloutId, rolloutGroup.getRollout().getId())) {
            throw new EntityNotFoundException(RolloutGroup.class, groupId);
        }

        return ResponseEntity.ok(MgmtRolloutMapper.toResponseRolloutGroup(
                rolloutGroup, true, tenantConfigHelper.isConfirmationFlowEnabled()));
    }

    @Override
    public ResponseEntity<PagedList<MgmtTarget>> getRolloutGroupTargets(
            final Long rolloutId, final Long groupId,
            final String rsqlParam, final int pagingOffsetParam, final int pagingLimitParam, final String sortParam) {
        findRolloutOrThrowException(rolloutId);
        final Pageable pageable = PagingUtility.toPageable(pagingOffsetParam, pagingLimitParam, sanitizeRolloutSortParam(sortParam));
        final Page<Target> rolloutGroupTargets;
        if (rsqlParam == null) {
            rolloutGroupTargets = this.rolloutGroupManagement.findTargetsOfRolloutGroup(groupId, pageable);
        } else {
            rolloutGroupTargets = this.rolloutGroupManagement.findTargetsOfRolloutGroupByRsql(groupId, rsqlParam, pageable);
        }
        final List<MgmtTarget> rest = MgmtTargetMapper.toResponse(rolloutGroupTargets.getContent(), tenantConfigHelper);
        return ResponseEntity.ok(new PagedList<>(rest, rolloutGroupTargets.getTotalElements()));
    }

    @Override
    @AuditLog(entity = "Rollout", type = AuditLog.Type.UPDATE, description = "Trigger Next Rollout Group")
    public ResponseEntity<Void> triggerNextGroup(final Long rolloutId) {
        rolloutManagement.triggerNextGroup(rolloutId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<MgmtRolloutResponseBody> retryRollout(final Long rolloutId) {
        final Rollout rolloutForRetry = rolloutManagement.get(rolloutId);
        if (rolloutForRetry.isDeleted()) {
            throw new EntityNotFoundException(Rollout.class, rolloutId);
        }

        if (!rolloutForRetry.getStatus().equals(Rollout.RolloutStatus.FINISHED)) {
            throw new ValidationException("Rollout must be finished in order to be retried!");
        }

        final Create create = MgmtRolloutMapper.fromRetriedRollout(rolloutForRetry);
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