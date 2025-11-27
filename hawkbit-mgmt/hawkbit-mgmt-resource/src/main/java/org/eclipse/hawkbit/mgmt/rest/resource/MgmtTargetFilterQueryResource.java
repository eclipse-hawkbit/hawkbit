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

import static org.eclipse.hawkbit.mgmt.rest.resource.util.PagingUtility.sanitizeTargetFilterQuerySortParam;

import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.audit.AuditLog;
import org.eclipse.hawkbit.mgmt.json.model.PagedList;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtDistributionSet;
import org.eclipse.hawkbit.mgmt.json.model.targetfilter.MgmtDistributionSetAutoAssignment;
import org.eclipse.hawkbit.mgmt.json.model.targetfilter.MgmtTargetFilterQuery;
import org.eclipse.hawkbit.mgmt.json.model.targetfilter.MgmtTargetFilterQueryRequestBody;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRepresentationMode;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtTargetFilterQueryRestApi;
import org.eclipse.hawkbit.mgmt.rest.resource.mapper.MgmtDistributionSetMapper;
import org.eclipse.hawkbit.mgmt.rest.resource.mapper.MgmtTargetFilterQueryMapper;
import org.eclipse.hawkbit.mgmt.rest.resource.util.PagingUtility;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement.AutoAssignDistributionSetUpdate;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.helper.TenantConfigHelper;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Resource handling target CRUD operations.
 */
@Slf4j
@RestController
public class MgmtTargetFilterQueryResource implements MgmtTargetFilterQueryRestApi {

    private final TargetFilterQueryManagement<? extends TargetFilterQuery> filterManagement;

    MgmtTargetFilterQueryResource(final TargetFilterQueryManagement<? extends TargetFilterQuery> filterManagement) {
        this.filterManagement = filterManagement;
    }

    @Override
    public ResponseEntity<MgmtTargetFilterQuery> getFilter(final Long filterId) {
        final TargetFilterQuery findTarget = findFilterWithExceptionIfNotFound(filterId);
        // to single response include poll status
        final MgmtTargetFilterQuery response = MgmtTargetFilterQueryMapper.toResponse(findTarget,
                TenantConfigHelper.isUserConfirmationFlowEnabled(), true);
        MgmtTargetFilterQueryMapper.addLinks(response);

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<PagedList<MgmtTargetFilterQuery>> getFilters(
            final String rsqlParam, final int pagingOffsetParam, final int pagingLimitParam, final String sortParam,
            final String representationModeParam) {
        final Pageable pageable = PagingUtility.toPageable(pagingOffsetParam, pagingLimitParam, sanitizeTargetFilterQuerySortParam(sortParam));
        final Page<? extends TargetFilterQuery> findTargetFiltersAll;
        if (rsqlParam != null) {
            findTargetFiltersAll = filterManagement.findByRsql(rsqlParam, pageable);
        } else {
            findTargetFiltersAll = filterManagement.findAll(pageable);
        }

        final boolean isRepresentationFull = parseRepresentationMode(representationModeParam) == MgmtRepresentationMode.FULL;

        final List<MgmtTargetFilterQuery> rest = MgmtTargetFilterQueryMapper.toResponse(
                findTargetFiltersAll.getContent(), TenantConfigHelper.isUserConfirmationFlowEnabled(), isRepresentationFull);
        return ResponseEntity.ok(new PagedList<>(rest, filterManagement.count()));
    }

    @Override
    public ResponseEntity<MgmtTargetFilterQuery> createFilter(final MgmtTargetFilterQueryRequestBody filter) {
        final TargetFilterQuery createdTarget = filterManagement.create(MgmtTargetFilterQueryMapper.fromRequest(filter));

        final MgmtTargetFilterQuery response = MgmtTargetFilterQueryMapper.toResponse(
                createdTarget, TenantConfigHelper.isUserConfirmationFlowEnabled(), false);
        MgmtTargetFilterQueryMapper.addLinks(response);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<MgmtTargetFilterQuery> updateFilter(final Long filterId, final MgmtTargetFilterQueryRequestBody targetFilterRest) {
        log.debug("updating target filter query {}", filterId);

        final TargetFilterQuery updateFilter = filterManagement
                .update(TargetFilterQueryManagement.Update.builder()
                        .id(filterId).name(targetFilterRest.getName()).query(targetFilterRest.getQuery())
                        .build());

        final MgmtTargetFilterQuery response = MgmtTargetFilterQueryMapper.toResponse(updateFilter,
                TenantConfigHelper.isUserConfirmationFlowEnabled(), false);
        MgmtTargetFilterQueryMapper.addLinks(response);

        return ResponseEntity.ok(response);
    }

    @Override
    @AuditLog(entity = "TargetFilter", type = AuditLog.Type.DELETE, description = "Delete Target Filter")
    public ResponseEntity<Void> deleteFilter(final Long filterId) {
        filterManagement.delete(filterId);
        log.debug("{} target filter query deleted, return status {}", filterId, HttpStatus.OK);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<MgmtDistributionSet> getAssignedDistributionSet(final Long filterId) {
        final TargetFilterQuery filter = findFilterWithExceptionIfNotFound(filterId);
        final DistributionSet autoAssignDistributionSet = filter.getAutoAssignDistributionSet();

        if (autoAssignDistributionSet == null) {
            return ResponseEntity.noContent().build();
        }

        final MgmtDistributionSet response = MgmtDistributionSetMapper.toResponse(autoAssignDistributionSet);
        MgmtDistributionSetMapper.addLinks(autoAssignDistributionSet, response);

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<MgmtTargetFilterQuery> postAssignedDistributionSet(
            final Long filterId, final MgmtDistributionSetAutoAssignment autoAssignRequest) {
        final boolean confirmationRequired = autoAssignRequest.getConfirmationRequired() == null
                ? TenantConfigHelper.isUserConfirmationFlowEnabled()
                : autoAssignRequest.getConfirmationRequired();

        final AutoAssignDistributionSetUpdate update = MgmtTargetFilterQueryMapper
                .fromRequest(filterId, autoAssignRequest).confirmationRequired(confirmationRequired);

        final TargetFilterQuery updateFilter = filterManagement.updateAutoAssignDS(update);

        final MgmtTargetFilterQuery response = MgmtTargetFilterQueryMapper.toResponse(updateFilter,
                TenantConfigHelper.isUserConfirmationFlowEnabled(), false);
        MgmtTargetFilterQueryMapper.addLinks(response);

        return ResponseEntity.ok(response);
    }

    @Override
    @AuditLog(entity = "TargetFilter", type = AuditLog.Type.DELETE, description = "Delete Target Filter Assigned Distribution Set")
    public ResponseEntity<Void> deleteAssignedDistributionSet(final Long filterId) {
        filterManagement.updateAutoAssignDS(new AutoAssignDistributionSetUpdate(filterId).ds(null));
        return ResponseEntity.noContent().build();
    }

    private static MgmtRepresentationMode parseRepresentationMode(final String representationModeParam) {
        return MgmtRepresentationMode.fromValue(representationModeParam).orElseGet(() -> {
            // no need for a 400, just apply a safe fallback
            log.warn("Received an invalid representation mode: {}", representationModeParam);
            return MgmtRepresentationMode.COMPACT;
        });
    }

    private TargetFilterQuery findFilterWithExceptionIfNotFound(final Long filterId) {
        return filterManagement.find(filterId)
                .orElseThrow(() -> new EntityNotFoundException(TargetFilterQuery.class, filterId));
    }
}