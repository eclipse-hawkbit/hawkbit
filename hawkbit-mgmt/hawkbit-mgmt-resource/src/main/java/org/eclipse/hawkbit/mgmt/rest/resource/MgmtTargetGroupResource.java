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

import static org.eclipse.hawkbit.mgmt.rest.resource.util.PagingUtility.sanitizeTargetSortParam;

import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.mgmt.json.model.PagedList;
import org.eclipse.hawkbit.mgmt.json.model.target.MgmtTarget;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtTargetGroupRestApi;
import org.eclipse.hawkbit.mgmt.rest.resource.mapper.MgmtTargetMapper;
import org.eclipse.hawkbit.mgmt.rest.resource.util.PagingUtility;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.model.Target;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class MgmtTargetGroupResource implements MgmtTargetGroupRestApi {

    private final TargetManagement<? extends Target> targetManagement;

    public MgmtTargetGroupResource(
            final TargetManagement <? extends Target>targetManagement) {
        this.targetManagement = targetManagement;
    }

    @Override
    public ResponseEntity<PagedList<MgmtTarget>> getAssignedTargets(
            final String group, final int pagingOffsetParam, final int pagingLimitParam, final String sortParam) {
        final Pageable pageable = PagingUtility.toPageable(pagingOffsetParam, pagingLimitParam, sanitizeTargetSortParam(sortParam));

        final Page<Target> targets = targetManagement.findTargetsByGroup(group, false, pageable);

        final List<MgmtTarget> rest = MgmtTargetMapper.toResponse(targets.getContent());
        return ResponseEntity.ok(new PagedList<>(rest, targets.getTotalElements()));
    }

    @Override
    public ResponseEntity<PagedList<MgmtTarget>> getAssignedTargetsWithSubgroups(
            final String groupFilter, final boolean subgroups, final int pagingOffsetParam, final int pagingLimitParam, final String sortParam) {
        final Pageable pageable = PagingUtility.toPageable(pagingOffsetParam, pagingLimitParam, sanitizeTargetSortParam(sortParam));

        final Page<Target> targets = targetManagement.findTargetsByGroup(groupFilter, subgroups, pageable);

        final List<MgmtTarget> rest = MgmtTargetMapper.toResponse(targets.getContent());
        return ResponseEntity.ok(new PagedList<>(rest, targets.getTotalElements()));
    }

    @Override
    public ResponseEntity<Void> assignTargetsToGroup(final String group, final List<String> controllerIds) {
        return assignTargets(group, controllerIds);
    }

    @Override
    public ResponseEntity<Void> assignTargetsToGroupWithSubgroups(final String group, final List<String> controllerIds) {
        return assignTargets(group, controllerIds);
    }

    @Override
    public ResponseEntity<Void> assignTargetsToGroupWithRsql(final String group, final String rsql) {
        return assignTargetsToGroupWithRsql0(group, rsql);
    }

    @Override
    public ResponseEntity<Void> unassignTargetsFromGroup(final List<String> controllerIds) {
        targetManagement.assignTargetsWithGroup(null, controllerIds);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> unassignTargetsFromGroupByRsql(final String rsql) {
        return assignTargetsToGroupWithRsql0(null, rsql);
    }

    @Override
    public ResponseEntity<List<String>> getTargetGroups() {
        final List<String> groups = targetManagement.findGroups();
        return ResponseEntity.ok(groups);
    }

    @Override
    public ResponseEntity<Void> assignTargetsToGroup(final String group, final String rsql) {
        return assignTargetsToGroupWithRsql0(group, rsql);
    }

    private ResponseEntity<Void> assignTargets(final String group, final List<String> controllerIds) {
        targetManagement.assignTargetsWithGroup(group, controllerIds);
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<Void> assignTargetsToGroupWithRsql0(final String group, final String rsql) {
        targetManagement.assignTargetGroupWithRsql(group, rsql);
        return ResponseEntity.noContent().build();
    }
}