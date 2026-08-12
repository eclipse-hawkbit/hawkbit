/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.rest.resource;

import static org.eclipse.hawkbit.repository.model.AutoAssignment.AutoAssignApprovalDecision.APPROVED;
import static org.eclipse.hawkbit.repository.model.AutoAssignment.AutoAssignApprovalDecision.DENIED;

import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.audit.AuditLog;
import org.eclipse.hawkbit.mgmt.json.model.PagedList;
import org.eclipse.hawkbit.mgmt.json.model.autoassignment.MgmtAutoAssignmentResponseBody;
import org.eclipse.hawkbit.mgmt.json.model.autoassignment.MgmtAutoAssignmentRestRequestBodyPost;
import org.eclipse.hawkbit.mgmt.json.model.autoassignment.MgmtAutoAssignmentRestRequestBodyPut;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtAutoAssignmentRestApi;
import org.eclipse.hawkbit.mgmt.rest.resource.mapper.MgmtAutoAssignmentMapper;
import org.eclipse.hawkbit.mgmt.rest.resource.util.PagingUtility;
import org.eclipse.hawkbit.repository.AutoAssignmentManagement;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.model.AutoAssignment;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Resource handling auto assignment CRUD operations.
 */
@Slf4j
@RestController
public class MgmtAutoAssignmentResource implements MgmtAutoAssignmentRestApi {

    private final AutoAssignmentManagement<? extends AutoAssignment> autoAssignmentManagement;
    private final DistributionSetManagement<? extends DistributionSet> distributionSetManagement;

    public MgmtAutoAssignmentResource(
            final AutoAssignmentManagement<? extends AutoAssignment> autoAssignmentManagement,
            final DistributionSetManagement<? extends DistributionSet> distributionSetManagement) {
        this.autoAssignmentManagement = autoAssignmentManagement;
        this.distributionSetManagement = distributionSetManagement;
    }

    @Override
    @AuditLog(entity = "AutoAssignment", type = AuditLog.Type.CREATE, description = "Create Auto Assignment")
    public ResponseEntity<MgmtAutoAssignmentResponseBody> create(final MgmtAutoAssignmentRestRequestBodyPost autoAssignmentRequestBody) {
        final AutoAssignment created = autoAssignmentManagement.create(
                MgmtAutoAssignmentMapper.fromRequest(autoAssignmentRequestBody, distributionSetManagement.get(autoAssignmentRequestBody
                        .getDistributionSetId())));
        return ResponseEntity.status(HttpStatus.CREATED).body(MgmtAutoAssignmentMapper.toResponseAutoAssignment(created));
    }

    @Override
    public ResponseEntity<PagedList<MgmtAutoAssignmentResponseBody>> getAutoAssignments(
            final String rsqlParam, final int pagingOffsetParam, final int pagingLimitParam, final String sortParam) {
        final Pageable pageable = PagingUtility.toPageable(pagingOffsetParam, pagingLimitParam,
                PagingUtility.sanitizeAutoAssignmentSortParam(sortParam));
        final Page<AutoAssignment> autoAssignments = autoAssignmentManagement.findAutoAssignmentByRsql(rsqlParam, pageable);
        final List<MgmtAutoAssignmentResponseBody> rest = MgmtAutoAssignmentMapper.toResponseAutoAssignment(autoAssignments.getContent());
        return ResponseEntity.ok(new PagedList<>(rest, autoAssignments.getTotalElements()));
    }

    @Override
    public ResponseEntity<MgmtAutoAssignmentResponseBody> getAutoAssignment(final long id) {
        return ResponseEntity.ok(MgmtAutoAssignmentMapper.toResponseAutoAssignment(autoAssignmentManagement.get(id)));
    }

    @Override
    @AuditLog(entity = "AutoAssignment", type = AuditLog.Type.UPDATE, description = "Update Auto Assignment")
    public ResponseEntity<MgmtAutoAssignmentResponseBody> update(
            final long id, final MgmtAutoAssignmentRestRequestBodyPut autoAssignmentRequestBody) {
        final AutoAssignment updated = autoAssignmentManagement.update(MgmtAutoAssignmentMapper.fromRequest(autoAssignmentRequestBody, id));
        return ResponseEntity.ok(MgmtAutoAssignmentMapper.toResponseAutoAssignment(updated));
    }

    @Override
    @AuditLog(entity = "AutoAssignment", type = AuditLog.Type.UPDATE, description = "Approve Auto Assignment")
    public ResponseEntity<Void> approve(final long id, final String remark) {
        autoAssignmentManagement.approveOrDeny(id, APPROVED, remark);
        return ResponseEntity.noContent().build();
    }

    @Override
    @AuditLog(entity = "AutoAssignment", type = AuditLog.Type.UPDATE, description = "Deny Auto Assignment")
    public ResponseEntity<Void> deny(final long id, final String remark) {
        autoAssignmentManagement.approveOrDeny(id, DENIED, remark);
        return ResponseEntity.noContent().build();
    }

    @Override
    @AuditLog(entity = "AutoAssignment", type = AuditLog.Type.UPDATE, description = "Start Auto Assignment")
    public ResponseEntity<Void> start(final long id) {
        autoAssignmentManagement.start(id);
        return ResponseEntity.noContent().build();
    }

    @Override
    @AuditLog(entity = "AutoAssignment", type = AuditLog.Type.UPDATE, description = "Pause Auto Assignment")
    public ResponseEntity<Void> pause(final long id) {
        autoAssignmentManagement.pause(id);
        return ResponseEntity.noContent().build();
    }

    @Override
    @AuditLog(entity = "AutoAssignment", type = AuditLog.Type.UPDATE, description = "Resume Auto Assignment")
    public ResponseEntity<Void> resume(final long id) {
        autoAssignmentManagement.resume(id);
        return ResponseEntity.noContent().build();
    }

    @Override
    @AuditLog(entity = "AutoAssignment", type = AuditLog.Type.DELETE, description = "Delete Auto Assignment")
    public ResponseEntity<Void> delete(final long id) {
        autoAssignmentManagement.delete(id);
        return ResponseEntity.noContent().build();
    }
}