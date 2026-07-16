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

import static org.eclipse.hawkbit.repository.model.TargetFilterQuery.AutoAssignApprovalDecision.APPROVED;
import static org.eclipse.hawkbit.repository.model.TargetFilterQuery.AutoAssignApprovalDecision.DENIED;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import jakarta.validation.ConstraintViolationException;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.audit.AuditLog;
import org.eclipse.hawkbit.mgmt.json.model.PagedList;
import org.eclipse.hawkbit.mgmt.json.model.targetfilter.MgmtAutoAssignmentResponseBody;
import org.eclipse.hawkbit.mgmt.json.model.targetfilter.MgmtAutoAssignmentRestRequestBodyPost;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtAutoAssignmentRestApi;
import org.eclipse.hawkbit.mgmt.rest.resource.mapper.MgmtAutoAssignmentMapper;
import org.eclipse.hawkbit.mgmt.rest.resource.util.PagingUtility;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement.AutoAssignDistributionSetUpdate;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
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

    private final TargetFilterQueryManagement<? extends TargetFilterQuery> targetFilterQueryManagement;

    public MgmtAutoAssignmentResource(final TargetFilterQueryManagement<? extends TargetFilterQuery> targetFilterQueryManagement) {
        this.targetFilterQueryManagement = targetFilterQueryManagement;
    }

    @Override
    @AuditLog(entity = "TargetFilter", type = AuditLog.Type.CREATE, description = "Create Auto Assignment")
    public ResponseEntity<MgmtAutoAssignmentResponseBody> create(final MgmtAutoAssignmentRestRequestBodyPost autoAssignmentRequestBody) {
        Optional<TargetFilterQuery> targetFilterQuery = targetFilterQueryManagement.findByName(autoAssignmentRequestBody.getName());
        long targetFilterQueryId;
        if (targetFilterQuery.isEmpty()) {
            targetFilterQueryId = targetFilterQueryManagement.create(TargetFilterQueryManagement.Create.builder()
                    .name(autoAssignmentRequestBody.getName())
                    .query(autoAssignmentRequestBody.getTargetFilterQuery()).build()).getId();
        } else {
            if (!targetFilterQuery.get().getQuery().equals(autoAssignmentRequestBody.getTargetFilterQuery())) {
                throw new ConstraintViolationException("Mismatch between queries in request and existing target filter query", Set.of());
            }
            targetFilterQueryId = targetFilterQuery.get().getId();
        }
        final TargetFilterQuery created = targetFilterQueryManagement.updateAutoAssignDS(
                MgmtAutoAssignmentMapper.fromRequest(autoAssignmentRequestBody, targetFilterQueryId));
        return ResponseEntity.status(HttpStatus.CREATED).body(MgmtAutoAssignmentMapper.toResponseAutoAssignment(created));
    }

    @Override
    public ResponseEntity<PagedList<MgmtAutoAssignmentResponseBody>> getAutoAssignments(final String rsqlParam, final int pagingOffsetParam,
            final int pagingLimitParam, final String sortParam) {
        final Pageable pageable = PagingUtility.toPageable(pagingOffsetParam, pagingLimitParam,
                PagingUtility.sanitizeTargetFilterQuerySortParam(sortParam));
        final Page<TargetFilterQuery> targetFilterQueries = targetFilterQueryManagement.findWithAutoAssignDSByRsql(rsqlParam, pageable);
        final List<MgmtAutoAssignmentResponseBody> rest = MgmtAutoAssignmentMapper.toResponseAutoAssignment(targetFilterQueries.getContent());
        return ResponseEntity.ok(new PagedList<>(rest, targetFilterQueries.getTotalElements()));
    }

    @Override
    public ResponseEntity<MgmtAutoAssignmentResponseBody> getAutoAssignment(final long id) {
        final TargetFilterQuery targetFilterQuery = targetFilterQueryManagement.get(id);
        if (targetFilterQuery.getAutoAssignDistributionSet() == null) {
            throw new EntityNotFoundException(TargetFilterQuery.class, id);
        }
        return ResponseEntity.ok(MgmtAutoAssignmentMapper.toResponseAutoAssignment(targetFilterQuery));
    }

    @Override
    @AuditLog(entity = "TargetFilter", type = AuditLog.Type.UPDATE, description = "Approve Auto Assignment")
    public ResponseEntity<Void> approve(final long id, final String remark) {
        targetFilterQueryManagement.approveOrDeny(id, APPROVED, remark);
        return ResponseEntity.noContent().build();
    }

    @Override
    @AuditLog(entity = "TargetFilter", type = AuditLog.Type.UPDATE, description = "Deny Auto Assignment")
    public ResponseEntity<Void> deny(final long id, final String remark) {
        targetFilterQueryManagement.approveOrDeny(id, DENIED, remark);
        return ResponseEntity.noContent().build();
    }

    @Override
    @AuditLog(entity = "TargetFilter", type = AuditLog.Type.UPDATE, description = "Start Auto Assignment")
    public ResponseEntity<Void> start(final long id) {
        targetFilterQueryManagement.startAutoAssignDS(id);
        return ResponseEntity.noContent().build();
    }

    @Override
    @AuditLog(entity = "TargetFilter", type = AuditLog.Type.UPDATE, description = "Pause Auto Assignment")
    public ResponseEntity<Void> pause(final long id) {
        targetFilterQueryManagement.pauseAutoAssignDS(id);
        return ResponseEntity.noContent().build();
    }

    @Override
    @AuditLog(entity = "TargetFilter", type = AuditLog.Type.UPDATE, description = "Resume Auto Assignment")
    public ResponseEntity<Void> resume(final long id) {
        targetFilterQueryManagement.resumeAutoAssignDS(id);
        return ResponseEntity.noContent().build();
    }

    @Override
    @AuditLog(entity = "TargetFilter", type = AuditLog.Type.DELETE, description = "Delete Auto Assignment")
    public ResponseEntity<Void> delete(final long id) {
        targetFilterQueryManagement.updateAutoAssignDS(new AutoAssignDistributionSetUpdate(id).ds(null));
        return ResponseEntity.noContent().build();
    }
}
