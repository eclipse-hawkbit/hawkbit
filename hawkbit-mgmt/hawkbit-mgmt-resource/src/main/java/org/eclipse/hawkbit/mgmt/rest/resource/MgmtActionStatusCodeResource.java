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

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.audit.AuditLog;
import org.eclipse.hawkbit.mgmt.json.model.PagedList;
import org.eclipse.hawkbit.mgmt.json.model.actionstatuscode.MgmtActionStatusCode;
import org.eclipse.hawkbit.mgmt.json.model.actionstatuscode.MgmtActionStatusCodeRequestBodyPost;
import org.eclipse.hawkbit.mgmt.json.model.actionstatuscode.MgmtActionStatusCodeRequestBodyPut;
import org.eclipse.hawkbit.mgmt.json.model.actionstatuscode.MgmtUndocumentedActionStatusCode;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtActionStatusCodeRestApi;
import org.eclipse.hawkbit.mgmt.rest.resource.mapper.MgmtActionStatusCodeMapper;
import org.eclipse.hawkbit.mgmt.rest.resource.util.PagingUtility;
import org.eclipse.hawkbit.repository.ActionStatusCodeManagement;
import org.eclipse.hawkbit.repository.model.ActionStatusCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Resource for action status code catalogue operations.
 */
@Slf4j
@RestController
public class MgmtActionStatusCodeResource implements MgmtActionStatusCodeRestApi {

    private final ActionStatusCodeManagement<ActionStatusCode> actionStatusCodeManagement;

    public MgmtActionStatusCodeResource(
            final ActionStatusCodeManagement<ActionStatusCode> actionStatusCodeManagement) {
        this.actionStatusCodeManagement = actionStatusCodeManagement;
    }

    @Override
    public ResponseEntity<PagedList<MgmtActionStatusCode>> getActionStatusCodes(
            final String rsqlParam,
            final int pagingOffsetParam,
            final int pagingLimitParam,
            final String sortParam) {

        final Pageable pageable = PagingUtility.toPageable(
                pagingOffsetParam,
                pagingLimitParam,
                PagingUtility.sanitizeActionStatusCodeSortParam(sortParam));

        final Page<ActionStatusCode> page = actionStatusCodeManagement.findByRsql(rsqlParam, pageable);
        final var rest = page.getContent().stream()
                .map(MgmtActionStatusCodeMapper::toResponse)
                .toList();

        return ResponseEntity.ok(new PagedList<>(rest, page.getTotalElements()));
    }

    @Override
    public ResponseEntity<MgmtActionStatusCode> getActionStatusCode(final long id) {
        final ActionStatusCode entity = actionStatusCodeManagement.get(id);
        return ResponseEntity.ok(MgmtActionStatusCodeMapper.toResponse(entity));
    }

    @Override
    @AuditLog(entity = "ActionStatusCode", type = AuditLog.Type.CREATE, description = "Create Action Status Code")
    public ResponseEntity<MgmtActionStatusCode> create(final MgmtActionStatusCodeRequestBodyPost body) {
        final ActionStatusCode created = actionStatusCodeManagement.create(
                ActionStatusCodeManagement.Create.builder()
                        .code(body.getCode())
                        .name(body.getName())
                        .description(body.getDescription())
                        .severity(MgmtActionStatusCodeMapper.toRepoSeverity(body.getSeverity()))
                        .build());

        return ResponseEntity.status(HttpStatus.CREATED).body(MgmtActionStatusCodeMapper.toResponse(created));
    }

    @Override
    @AuditLog(entity = "ActionStatusCode", type = AuditLog.Type.UPDATE, description = "Update Action Status Code")
    public ResponseEntity<MgmtActionStatusCode> update(final long id, final MgmtActionStatusCodeRequestBodyPut body) {
        final ActionStatusCode updated = actionStatusCodeManagement.update(
                ActionStatusCodeManagement.Update.builder()
                        .id(id)
                        .name(body.getName())
                        .description(body.getDescription())
                        .severity(body.getSeverity() != null ? MgmtActionStatusCodeMapper.toRepoSeverity(body.getSeverity()) : null)
                        .build());

        return ResponseEntity.ok(MgmtActionStatusCodeMapper.toResponse(updated));
    }

    @Override
    public ResponseEntity<PagedList<MgmtUndocumentedActionStatusCode>> getUndocumentedActionStatusCodes(
            final int pagingOffsetParam,
            final int pagingLimitParam) {

        final Pageable pageable = PagingUtility.toPageable(pagingOffsetParam, pagingLimitParam, Sort.unsorted());
        final Page<Integer> page = actionStatusCodeManagement.findUndocumentedCodes(pageable);
        final var rest = page.getContent().stream()
                .map(MgmtUndocumentedActionStatusCode::new)
                .toList();

        return ResponseEntity.ok(new PagedList<>(rest, page.getTotalElements()));
    }
}
