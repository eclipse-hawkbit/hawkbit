/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.rest.resource;

import jakarta.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.mgmt.json.model.PagedList;
import org.eclipse.hawkbit.mgmt.json.model.target.MgmtTarget;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtTargetGroupRestApi;
import org.eclipse.hawkbit.mgmt.rest.resource.mapper.MgmtTargetMapper;
import org.eclipse.hawkbit.mgmt.rest.resource.util.PagingUtility;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.eclipse.hawkbit.utils.TenantConfigHelper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.eclipse.hawkbit.mgmt.rest.resource.util.PagingUtility.sanitizeTargetSortParam;

@Slf4j
@RestController
public class MgmtTargetGroupResource implements MgmtTargetGroupRestApi {

    private final TargetManagement targetManagement;
    private final TenantConfigHelper tenantConfigHelper;

    public MgmtTargetGroupResource(final TargetManagement targetManagement, final SystemSecurityContext systemSecurityContext,
            final TenantConfigurationManagement tenantConfigurationManagement) {
        this.targetManagement = targetManagement;
        this.tenantConfigHelper = TenantConfigHelper.usingContext(systemSecurityContext, tenantConfigurationManagement);
    }

    @Override
    public ResponseEntity<PagedList<MgmtTarget>> getAssignedTargets(String group, int pagingOffsetParam, int pagingLimitParam, String sortParam) {
        final Pageable pageable = PagingUtility.toPageable(pagingOffsetParam, pagingLimitParam, sanitizeTargetSortParam(sortParam));

        final Page<Target> targets = targetManagement.findTargetsByGroup(group, false, pageable);

        final List<MgmtTarget> rest = MgmtTargetMapper.toResponse(targets.getContent(), tenantConfigHelper);
        return ResponseEntity.ok(new PagedList<>(rest, targets.getTotalElements()));
    }

    @Override
    public ResponseEntity<PagedList<MgmtTarget>> getAssignedTargetsWithSubgroups(String groupFilter, boolean subgroups, int pagingOffsetParam, int pagingLimitParam, String sortParam) {
        final Pageable pageable = PagingUtility.toPageable(pagingOffsetParam, pagingLimitParam, sanitizeTargetSortParam(sortParam));

        final Page<Target> targets = targetManagement.findTargetsByGroup(groupFilter, subgroups, pageable);

        final List<MgmtTarget> rest = MgmtTargetMapper.toResponse(targets.getContent(), tenantConfigHelper);
        return ResponseEntity.ok(new PagedList<>(rest, targets.getTotalElements()));
    }

    @Override
    public ResponseEntity<Void> assignTargetsToGroup(String group, List<String> controllerIds) {
        targetManagement.assignTargetsWithGroup(group, controllerIds);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> assignTargetsToGroupWithSubgroups(String group, List<String> controllerIds) {
        targetManagement.assignTargetsWithGroup(group, controllerIds);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> assignTargetsToGroupWithRsql(String group, String rsql) {
        targetManagement.assignTargetGroupWithRsql(group, rsql);
        return null;
    }

    @Override
    public ResponseEntity<Void> unassignTargetsFromGroup(List<String> controllerIds, String rsql) {
        if (!ObjectUtils.isEmpty(controllerIds)) {
            log.debug("Unassigning group from list of controllerIds {}", controllerIds);
            targetManagement.assignTargetsWithGroup(null, controllerIds);
        } else if (!ObjectUtils.isEmpty(rsql)) {
            log.debug("Unassigning group from target rsql filter {} .", rsql);
            targetManagement.assignTargetGroupWithRsql(null, rsql);
        } else {
            throw new ValidationException("Parameters controllerIds & rsql cannot be both null or empty.");
        }

        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> unnasignTargetFromGroup(String controllerId) {
        targetManagement.assignTargetGroup(controllerId, null);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<List<String>> getTargetGroups() {
        final List<String> groups = targetManagement.findGroups();
        return ResponseEntity.ok(groups);
    }

    @Override
    public ResponseEntity<Void> assignTargetsToGroup(final String group, final String rsql) {
        targetManagement.assignTargetGroupWithRsql(group, rsql);
        return ResponseEntity.ok().build();
    }
}
