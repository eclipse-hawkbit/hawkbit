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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    public ResponseEntity<PagedList<MgmtTarget>> getAssignedTargets(String targetGroup, int pagingOffsetParam, int pagingLimitParam, String sortParam) {
        final Pageable pageable = PagingUtility.toPageable(pagingOffsetParam, pagingLimitParam, sanitizeTargetSortParam(sortParam));

        final Page<Target> targets = targetManagement.findTargetsByGroup(targetGroup, pageable);

        final List<MgmtTarget> rest = MgmtTargetMapper.toResponse(targets.getContent(), tenantConfigHelper);
        return ResponseEntity.ok(new PagedList<>(rest, targets.getTotalElements()));
    }

    @Override
    public ResponseEntity<PagedList<MgmtTarget>> getAssignedTargetsWithSubgroups(String groupFilter, boolean subgroups, int pagingOffsetParam, int pagingLimitParam, String sortParam) {
        final Pageable pageable = PagingUtility.toPageable(pagingOffsetParam, pagingLimitParam, sanitizeTargetSortParam(sortParam));

        Page<Target> targets;
        if (subgroups) {
            String groupFilterRegex = "^[^*%]*\\*?$";
            Matcher matcher = Pattern.compile(groupFilterRegex).matcher(groupFilter);
            if (!matcher.matches()) {
                log.error("Provided group filter {} contains wildcard in different place than in the end", groupFilter);
                return ResponseEntity.badRequest().build();
            }
            groupFilter = groupFilter.replace("*", "%");

            targets = targetManagement.findTargetsByGroupFilter(groupFilter, pageable);
        } else {
            targets = targetManagement.findTargetsByGroup(groupFilter, pageable);
        }

        final List<MgmtTarget> rest = MgmtTargetMapper.toResponse(targets.getContent(), tenantConfigHelper);
        return ResponseEntity.ok(new PagedList<>(rest, targets.getTotalElements()));
    }

    @Override
    public ResponseEntity<Void> assignTargetsToGroup(String targetGroup, List<String> controllerIds) {
        targetManagement.updateTargetsWithGroup(targetGroup, controllerIds);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> assignTargetsToGroupWithSubgroups(String targetGroup, List<String> controllerIds) {
        targetManagement.updateTargetsWithGroup(targetGroup, controllerIds);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> unassignTargetsFromGroup(List<String> controllerIds) {
        targetManagement.updateTargetsWithGroup(null, controllerIds);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<List<String>> getTargetGroups() {
        final List<String> groups = targetManagement.findGroups();
        return ResponseEntity.ok(groups);
    }

    @Override
    public ResponseEntity<Void> assignTargetToGroup(String controllerId, String targetGroup) {

        log.info("Updating target group invoked...");
        targetManagement.updateTargetGroup(controllerId, targetGroup);

        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> assignTargetsToGroup(final String targetGroup, final String rsql) {
        targetManagement.updateTargetGroupsWithRsql(targetGroup, rsql);
        return ResponseEntity.ok().build();
    }
}
