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

import java.util.AbstractMap.SimpleEntry;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.validation.ValidationException;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.mgmt.json.model.MgmtId;
import org.eclipse.hawkbit.mgmt.json.model.MgmtMetadata;
import org.eclipse.hawkbit.mgmt.json.model.MgmtMetadataBodyPut;
import org.eclipse.hawkbit.mgmt.json.model.PagedList;
import org.eclipse.hawkbit.mgmt.json.model.action.MgmtAction;
import org.eclipse.hawkbit.mgmt.json.model.action.MgmtActionRequestBodyPut;
import org.eclipse.hawkbit.mgmt.json.model.action.MgmtActionStatus;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtActionType;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtDistributionSet;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtTargetAssignmentResponseBody;
import org.eclipse.hawkbit.mgmt.json.model.tag.MgmtTag;
import org.eclipse.hawkbit.mgmt.json.model.target.MgmtDistributionSetAssignments;
import org.eclipse.hawkbit.mgmt.json.model.target.MgmtTarget;
import org.eclipse.hawkbit.mgmt.json.model.target.MgmtTargetAttributes;
import org.eclipse.hawkbit.mgmt.json.model.target.MgmtTargetAutoConfirm;
import org.eclipse.hawkbit.mgmt.json.model.target.MgmtTargetAutoConfirmUpdate;
import org.eclipse.hawkbit.mgmt.json.model.target.MgmtTargetRequestBody;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtTargetRestApi;
import org.eclipse.hawkbit.mgmt.rest.resource.util.PagingUtility;
import org.eclipse.hawkbit.repository.ConfirmationManagement;
import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.OffsetBasedPageRequest;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.eclipse.hawkbit.repository.model.DeploymentRequest;
import org.eclipse.hawkbit.repository.model.DistributionSetAssignmentResult;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetMetadata;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.eclipse.hawkbit.utils.TenantConfigHelper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Resource handling target CRUD operations.
 */
@Slf4j
@RestController
public class MgmtTargetResource implements MgmtTargetRestApi {

    private static final String ACTION_TARGET_MISSING_ASSIGN_WARN = "given action ({}) is not assigned to given target ({}).";

    private final TargetManagement targetManagement;
    private final ConfirmationManagement confirmationManagement;
    private final DeploymentManagement deploymentManagement;
    private final EntityFactory entityFactory;
    private final TenantConfigHelper tenantConfigHelper;

    MgmtTargetResource(
            final TargetManagement targetManagement, final DeploymentManagement deploymentManagement,
            final ConfirmationManagement confirmationManagement, final EntityFactory entityFactory,
            final SystemSecurityContext systemSecurityContext,
            final TenantConfigurationManagement tenantConfigurationManagement) {
        this.targetManagement = targetManagement;
        this.deploymentManagement = deploymentManagement;
        this.confirmationManagement = confirmationManagement;
        this.entityFactory = entityFactory;
        this.tenantConfigHelper = TenantConfigHelper.usingContext(systemSecurityContext, tenantConfigurationManagement);
    }

    @Override
    public ResponseEntity<MgmtTarget> getTarget(final String targetId) {
        final Target findTarget = findTargetWithExceptionIfNotFound(targetId);
        // to single response include poll status
        final MgmtTarget response = MgmtTargetMapper.toResponse(findTarget, tenantConfigHelper, null);
        MgmtTargetMapper.addTargetLinks(response);

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<PagedList<MgmtTarget>> getTargets(
            final int pagingOffsetParam, final int pagingLimitParam, final String sortParam, final String rsqlParam) {
        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeTargetSortParam(sortParam);

        final Pageable pageable = new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting);
        final Slice<Target> findTargetsAll;
        final long countTargetsAll;
        if (rsqlParam != null) {
            findTargetsAll = targetManagement.findByRsql(pageable, rsqlParam);
            countTargetsAll = targetManagement.countByRsql(rsqlParam);
        } else {
            findTargetsAll = targetManagement.findAll(pageable);
            countTargetsAll = targetManagement.count();
        }

        final List<MgmtTarget> rest = MgmtTargetMapper.toResponse(findTargetsAll.getContent(), tenantConfigHelper);
        return ResponseEntity.ok(new PagedList<>(rest, countTargetsAll));
    }

    @Override
    public ResponseEntity<List<MgmtTarget>> createTargets(final List<MgmtTargetRequestBody> targets) {
        log.debug("creating {} targets", targets.size());
        final Collection<Target> createdTargets = this.targetManagement
                .create(MgmtTargetMapper.fromRequest(entityFactory, targets));
        log.debug("{} targets created, return status {}", targets.size(), HttpStatus.CREATED);
        return new ResponseEntity<>(MgmtTargetMapper.toResponse(createdTargets, tenantConfigHelper),
                HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<MgmtTarget> updateTarget(final String targetId, final MgmtTargetRequestBody targetRest) {
        if (targetRest.getRequestAttributes() != null) {
            if (Boolean.TRUE.equals(targetRest.getRequestAttributes())) {
                targetManagement.requestControllerAttributes(targetId);
            } else {
                return ResponseEntity.badRequest().build();
            }
        }

        final Target updateTarget;
        if (targetRest.getTargetType() != null && targetRest.getTargetType() == -1L) {
            // if targetType in request is -1 - unassign targetType from target
            this.targetManagement.unassignType(targetId);
            // update target without targetType here ...
            updateTarget = this.targetManagement.update(entityFactory.target().update(targetId)
                    .name(targetRest.getName()).description(targetRest.getDescription()).address(targetRest.getAddress())
                    .securityToken(targetRest.getSecurityToken()).requestAttributes(targetRest.getRequestAttributes()));

        } else {
            updateTarget = this.targetManagement.update(
                    entityFactory.target().update(targetId).name(targetRest.getName()).description(targetRest.getDescription())
                            .address(targetRest.getAddress()).targetType(targetRest.getTargetType())
                            .securityToken(targetRest.getSecurityToken())
                            .requestAttributes(targetRest.getRequestAttributes()));

        }

        final MgmtTarget response = MgmtTargetMapper.toResponse(updateTarget, tenantConfigHelper, null);
        MgmtTargetMapper.addTargetLinks(response);

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<Void> deleteTarget(final String targetId) {
        this.targetManagement.deleteByControllerID(targetId);
        log.debug("{} target deleted, return status {}", targetId, HttpStatus.OK);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> unassignTargetType(final String targetId) {
        this.targetManagement.unassignType(targetId);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> assignTargetType(final String targetId, final MgmtId targetTypeId) {
        this.targetManagement.assignType(targetId, targetTypeId.getId());
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<MgmtTargetAttributes> getAttributes(final String targetId) {
        final Map<String, String> controllerAttributes = targetManagement.getControllerAttributes(targetId);
        if (controllerAttributes.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        final MgmtTargetAttributes result = new MgmtTargetAttributes();
        result.putAll(controllerAttributes);

        return ResponseEntity.ok(result);
    }

    @Override
    public ResponseEntity<PagedList<MgmtAction>> getActionHistory(
            final String targetId,
            final int pagingOffsetParam, final int pagingLimitParam, final String sortParam, final String rsqlParam) {
        findTargetWithExceptionIfNotFound(targetId);

        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeActionSortParam(sortParam);
        final Pageable pageable = new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting);

        final Slice<Action> activeActions;
        final long totalActionCount;
        if (rsqlParam != null) {
            activeActions = this.deploymentManagement.findActionsByTarget(rsqlParam, targetId, pageable);
            totalActionCount = this.deploymentManagement.countActionsByTarget(rsqlParam, targetId);
        } else {
            activeActions = this.deploymentManagement.findActionsByTarget(targetId, pageable);
            totalActionCount = this.deploymentManagement.countActionsByTarget(targetId);
        }

        return ResponseEntity.ok(new PagedList<>(MgmtTargetMapper.toResponse(targetId, activeActions.getContent()), totalActionCount));
    }

    @Override
    public ResponseEntity<MgmtAction> getAction(final String targetId, final Long actionId) {
        final Action action = deploymentManagement.findAction(actionId)
                .orElseThrow(() -> new EntityNotFoundException(Action.class, actionId));
        if (!action.getTarget().getControllerId().equals(targetId)) {
            log.warn(ACTION_TARGET_MISSING_ASSIGN_WARN, action.getId(), targetId);
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(MgmtTargetMapper.toResponseWithLinks(targetId, action));
    }

    @Override
    public ResponseEntity<Void> cancelAction(final String targetId, final Long actionId, final boolean force) {
        final Action action = deploymentManagement.findAction(actionId)
                .orElseThrow(() -> new EntityNotFoundException(Action.class, actionId));

        if (!action.getTarget().getControllerId().equals(targetId)) {
            log.warn(ACTION_TARGET_MISSING_ASSIGN_WARN, actionId, targetId);
            return ResponseEntity.notFound().build();
        }

        if (force) {
            this.deploymentManagement.forceQuitAction(actionId);
        } else {
            this.deploymentManagement.cancelAction(actionId);
        }
        // both functions will throw an exception, when action is in wrong
        // state, which is mapped by MgmtResponseExceptionHandler.

        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<MgmtAction> updateAction(final String targetId, final Long actionId, final MgmtActionRequestBodyPut actionUpdate) {

        Action action = deploymentManagement.findAction(actionId)
                .orElseThrow(() -> new EntityNotFoundException(Action.class, actionId));
        if (!action.getTarget().getControllerId().equals(targetId)) {
            log.warn(ACTION_TARGET_MISSING_ASSIGN_WARN, action.getId(), targetId);
            return ResponseEntity.notFound().build();
        }

        if (MgmtActionType.FORCED != actionUpdate.getActionType()) {
            throw new ValidationException("Resource supports only switch to FORCED.");
        }

        action = deploymentManagement.forceTargetAction(actionId);

        return ResponseEntity.ok(MgmtTargetMapper.toResponseWithLinks(targetId, action));
    }

    @Override
    public ResponseEntity<PagedList<MgmtActionStatus>> getActionStatusList(
            final String targetId, final Long actionId,
            final int pagingOffsetParam, final int pagingLimitParam, final String sortParam) {
        final Target target = findTargetWithExceptionIfNotFound(targetId);

        final Action action = deploymentManagement.findAction(actionId)
                .orElseThrow(() -> new EntityNotFoundException(Action.class, actionId));

        if (!action.getTarget().getId().equals(target.getId())) {
            log.warn(ACTION_TARGET_MISSING_ASSIGN_WARN, action.getId(), target.getId());
            return ResponseEntity.notFound().build();
        }

        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeActionStatusSortParam(sortParam);

        final Page<ActionStatus> statusList = this.deploymentManagement.findActionStatusByAction(
                new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting), action.getId());

        return ResponseEntity.ok(new PagedList<>(
                MgmtTargetMapper.toActionStatusRestResponse(statusList.getContent(), deploymentManagement),
                statusList.getTotalElements()));
    }

    @Override
    public ResponseEntity<MgmtDistributionSet> getAssignedDistributionSet(final String targetId) {
        final MgmtDistributionSet distributionSetRest = deploymentManagement.getAssignedDistributionSet(targetId)
                .map(ds -> {
                    final MgmtDistributionSet response = MgmtDistributionSetMapper.toResponse(ds);
                    MgmtDistributionSetMapper.addLinks(ds, response);

                    return response;
                }).orElse(null);

        if (distributionSetRest == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(distributionSetRest);
    }

    @Override
    public ResponseEntity<MgmtTargetAssignmentResponseBody> postAssignedDistributionSet(
            final String targetId, final MgmtDistributionSetAssignments dsAssignments, final Boolean offline) {
        if (offline != null && offline) {
            final List<Entry<String, Long>> offlineAssignments = dsAssignments.stream()
                    .map(dsAssignment -> new SimpleEntry<>(targetId, dsAssignment.getId()))
                    .collect(Collectors.toList());
            return ResponseEntity.ok(MgmtDistributionSetMapper
                    .toResponse(deploymentManagement.offlineAssignedDistributionSets(offlineAssignments)));
        }
        findTargetWithExceptionIfNotFound(targetId);

        final List<DeploymentRequest> deploymentRequests = dsAssignments.stream().map(dsAssignment -> {
            final boolean isConfirmationRequired = dsAssignment.getConfirmationRequired() == null
                    ? tenantConfigHelper.isConfirmationFlowEnabled()
                    : dsAssignment.getConfirmationRequired();
            return MgmtDeploymentRequestMapper.createAssignmentRequestBuilder(dsAssignment, targetId)
                    .setConfirmationRequired(isConfirmationRequired).build();
        }).toList();

        final List<DistributionSetAssignmentResult> assignmentResults = deploymentManagement
                .assignDistributionSets(deploymentRequests);
        return ResponseEntity.ok(MgmtDistributionSetMapper.toResponse(assignmentResults));
    }

    @Override
    public ResponseEntity<MgmtDistributionSet> getInstalledDistributionSet(final String targetId) {
        final MgmtDistributionSet distributionSetRest = deploymentManagement.getInstalledDistributionSet(targetId)
                .map(set -> {
                    final MgmtDistributionSet response = MgmtDistributionSetMapper.toResponse(set);
                    MgmtDistributionSetMapper.addLinks(set, response);

                    return response;
                }).orElse(null);

        if (distributionSetRest == null) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(distributionSetRest);
    }

    @Override
    public ResponseEntity<List<MgmtTag>> getTags(final String targetId) {
        final Set<TargetTag> tags = targetManagement.getTagsByControllerId(targetId);
        return ResponseEntity.ok(MgmtTagMapper.toResponse(tags == null ? Collections.emptyList() : tags.stream().toList()));
    }

    @Override
    public ResponseEntity<PagedList<MgmtMetadata>> getMetadata(
            final String targetId,
            final int pagingOffsetParam, final int pagingLimitParam, final String sortParam, final String rsqlParam) {
        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeDistributionSetMetadataSortParam(sortParam);

        final Pageable pageable = new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting);
        final Page<TargetMetadata> metaDataPage;

        if (rsqlParam != null) {
            metaDataPage = targetManagement.findMetaDataByControllerIdAndRsql(pageable, targetId, rsqlParam);
        } else {
            metaDataPage = targetManagement.findMetaDataByControllerId(pageable, targetId);
        }

        return ResponseEntity.ok(new PagedList<>(MgmtTargetMapper.toResponseTargetMetadata(
                metaDataPage.getContent()), metaDataPage.getTotalElements()));
    }

    @Override
    public ResponseEntity<MgmtMetadata> getMetadataValue(final String targetId, final String metadataKey) {
        final TargetMetadata findOne = targetManagement.getMetaDataByControllerId(targetId, metadataKey)
                .orElseThrow(() -> new EntityNotFoundException(TargetMetadata.class, targetId, metadataKey));
        return ResponseEntity.ok(MgmtTargetMapper.toResponseTargetMetadata(findOne));
    }

    @Override
    public ResponseEntity<MgmtMetadata> updateMetadata(final String targetId, final String metadataKey, final MgmtMetadataBodyPut metadata) {
        final TargetMetadata updated = targetManagement.updateMetadata(targetId,
                entityFactory.generateTargetMetadata(metadataKey, metadata.getValue()));
        return ResponseEntity.ok(MgmtTargetMapper.toResponseTargetMetadata(updated));
    }

    @Override
    public ResponseEntity<Void> deleteMetadata(final String targetId, final String metadataKey) {
        targetManagement.deleteMetaData(targetId, metadataKey);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<List<MgmtMetadata>> createMetadata(final String targetId, final List<MgmtMetadata> metadataRest) {
        final List<TargetMetadata> created = targetManagement.createMetaData(targetId,
                MgmtTargetMapper.fromRequestTargetMetadata(metadataRest, entityFactory));
        return new ResponseEntity<>(MgmtTargetMapper.toResponseTargetMetadata(created), HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<MgmtTargetAutoConfirm> getAutoConfirmStatus(final String targetId) {
        final Target findTarget = targetManagement.getWithAutoConfigurationStatus(targetId);
        final MgmtTargetAutoConfirm state = MgmtTargetMapper.getTargetAutoConfirmResponse(findTarget);
        return ResponseEntity.ok(state);
    }

    @Override
    public ResponseEntity<Void> activateAutoConfirm(final String targetId, final MgmtTargetAutoConfirmUpdate update) {
        final String initiator = getNullIfEmpty(update, MgmtTargetAutoConfirmUpdate::getInitiator);
        final String remark = getNullIfEmpty(update, MgmtTargetAutoConfirmUpdate::getRemark);
        confirmationManagement.activateAutoConfirmation(targetId, initiator, remark);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Void> deactivateAutoConfirm(final String targetId) {
        confirmationManagement.deactivateAutoConfirmation(targetId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    private Target findTargetWithExceptionIfNotFound(final String targetId) {
        return targetManagement.getByControllerID(targetId)
                .orElseThrow(() -> new EntityNotFoundException(Target.class, targetId));
    }

    private <T, R> R getNullIfEmpty(final T object, final Function<T, R> extractMethod) {
        return object == null ? null : extractMethod.apply(object);
    }
}