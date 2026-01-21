/**
 * Copyright (c) 2022 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.rest.resource;

import static org.eclipse.hawkbit.mgmt.rest.resource.util.PagingUtility.sanitizeActionSortParam;

import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.audit.AuditLog;
import org.eclipse.hawkbit.mgmt.json.model.PagedList;
import org.eclipse.hawkbit.mgmt.json.model.action.MgmtAction;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtActionRestApi;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRepresentationMode;
import org.eclipse.hawkbit.mgmt.rest.resource.mapper.MgmtActionMapper;
import org.eclipse.hawkbit.mgmt.rest.resource.util.PagingUtility;
import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.model.Action;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@ConditionalOnProperty(name = "hawkbit.rest.MgmtActionResource.enabled", matchIfMissing = true)
public class MgmtActionResource implements MgmtActionRestApi {

    private final DeploymentManagement deploymentManagement;

    MgmtActionResource(final DeploymentManagement deploymentManagement) {
        this.deploymentManagement = deploymentManagement;
    }

    @Override
    public ResponseEntity<PagedList<MgmtAction>> getActions(
            final String rsqlParam, final int pagingOffsetParam, final int pagingLimitParam, final String sortParam,
            final String representationModeParam) {
        final Pageable pageable = PagingUtility.toPageable(pagingOffsetParam, pagingLimitParam, sanitizeActionSortParam(sortParam));

        final Slice<Action> actions;
        final long totalActionCount;
        if (rsqlParam != null) {
            actions = this.deploymentManagement.findActions(rsqlParam, pageable);
            totalActionCount = this.deploymentManagement.countActions(rsqlParam);
        } else {
            actions = this.deploymentManagement.findActionsAll(pageable);
            totalActionCount = this.deploymentManagement.countActionsAll();
        }

        final MgmtRepresentationMode repMode = getRepresentationModeFromString(representationModeParam);
        return ResponseEntity.ok(new PagedList<>(MgmtActionMapper.toResponse(actions.getContent(), repMode), totalActionCount));
    }

    @Override
    public ResponseEntity<MgmtAction> getAction(final Long actionId) {
        final Action action = deploymentManagement.findAction(actionId)
                .orElseThrow(() -> new EntityNotFoundException(Action.class, actionId));

        return ResponseEntity.ok(MgmtActionMapper.toResponse(action, MgmtRepresentationMode.FULL));
    }

    @Override
    @AuditLog(entity = "Actions", type = AuditLog.Type.DELETE, description = "Delete Action", logResponse = true)
    public ResponseEntity<Void> deleteAction(Long actionId) {
        deploymentManagement.deleteAction(actionId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @AuditLog(entity = "Actions", type = AuditLog.Type.DELETE, description = "Delete Actions", logResponse = true)
    public ResponseEntity<Void> deleteActions(String rsqlParam, List<Long> actionIds) {

        final boolean isActionIdsNotEmpty = !ObjectUtils.isEmpty(actionIds);
        if (!ObjectUtils.isEmpty(rsqlParam)) {

            if (isActionIdsNotEmpty) {
                throw new IllegalArgumentException("Only one of the parameters should be provided!");
            }

            deploymentManagement.deleteActionsByRsql(rsqlParam);
        } else if (isActionIdsNotEmpty) {
            deploymentManagement.deleteActionsByIds(actionIds);
        } else {
            throw new IllegalArgumentException("Either action id list or rsql filter should be provided.");
        }

        return ResponseEntity.noContent().build();
    }

    private MgmtRepresentationMode getRepresentationModeFromString(final String representationModeParam) {
        return MgmtRepresentationMode.fromValue(representationModeParam)
                .orElseGet(() -> {
                    // no need for a 400, just apply a safe fallback
                    log.warn("Received an invalid representation mode: {}", representationModeParam);
                    return MgmtRepresentationMode.COMPACT;
                });
    }
}