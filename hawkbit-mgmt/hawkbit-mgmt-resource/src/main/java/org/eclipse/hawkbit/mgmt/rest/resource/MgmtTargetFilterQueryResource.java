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
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.audit.AuditLog;
import org.eclipse.hawkbit.mgmt.json.model.PagedList;
import org.eclipse.hawkbit.mgmt.json.model.autoassignment.MgmtAutoAssignmentRestRequestBodyPost;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtDistributionSet;
import org.eclipse.hawkbit.mgmt.json.model.targetfilter.MgmtDistributionSetAutoAssignment;
import org.eclipse.hawkbit.mgmt.json.model.targetfilter.MgmtTargetFilterQuery;
import org.eclipse.hawkbit.mgmt.json.model.targetfilter.MgmtTargetFilterQueryRequestBody;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRepresentationMode;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtTargetFilterQueryRestApi;
import org.eclipse.hawkbit.mgmt.rest.resource.mapper.MgmtAutoAssignmentMapper;
import org.eclipse.hawkbit.mgmt.rest.resource.mapper.MgmtDistributionSetMapper;
import org.eclipse.hawkbit.mgmt.rest.resource.mapper.MgmtTargetFilterQueryMapper;
import org.eclipse.hawkbit.mgmt.rest.resource.util.PagingUtility;
import org.eclipse.hawkbit.repository.AutoAssignmentManagement;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.helper.TenantConfigHelper;
import org.eclipse.hawkbit.repository.model.AutoAssignment;
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
    private final AutoAssignmentManagement<? extends AutoAssignment> autoAssignmentManagement;
    private final DistributionSetManagement<? extends DistributionSet> distributionSetManagement;

    MgmtTargetFilterQueryResource(final TargetFilterQueryManagement<? extends TargetFilterQuery> filterManagement,
            final AutoAssignmentManagement<? extends AutoAssignment> autoAssignmentManagement,
            final DistributionSetManagement<? extends DistributionSet> distributionSetManagement) {
        this.filterManagement = filterManagement;
        this.autoAssignmentManagement = autoAssignmentManagement;
        this.distributionSetManagement = distributionSetManagement;
    }

    @Override
    public ResponseEntity<MgmtTargetFilterQuery> getFilter(final Long filterId) {
        final TargetFilterQuery findTarget = findFilterWithExceptionIfNotFound(filterId);
        // to single response include poll status
        final MgmtTargetFilterQuery response = MgmtTargetFilterQueryMapper.toResponse(findTarget,
                filterManagement.findLinkedAutoAssignment(filterId), TenantConfigHelper.isUserConfirmationFlowEnabled(), true);
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
        final boolean confirmationFlowEnabled = TenantConfigHelper.isUserConfirmationFlowEnabled();

        final List<MgmtTargetFilterQuery> rest = findTargetFiltersAll.getContent().stream()
                .map(filter -> MgmtTargetFilterQueryMapper.toResponse(
                        filter, filterManagement.findLinkedAutoAssignment(filter.getId()), confirmationFlowEnabled, isRepresentationFull))
                .toList();
        return ResponseEntity.ok(new PagedList<>(rest, filterManagement.count()));
    }

    @Override
    public ResponseEntity<MgmtTargetFilterQuery> createFilter(final MgmtTargetFilterQueryRequestBody filter) {
        final TargetFilterQuery createdTarget = filterManagement.create(MgmtTargetFilterQueryMapper.fromRequest(filter));

        final MgmtTargetFilterQuery response = MgmtTargetFilterQueryMapper.toResponse(
                createdTarget, Optional.empty(), TenantConfigHelper.isUserConfirmationFlowEnabled(), false);
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
                filterManagement.findLinkedAutoAssignment(filterId), TenantConfigHelper.isUserConfirmationFlowEnabled(), false);
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
        return filterManagement.findLinkedAutoAssignment(filterId)
                .map(AutoAssignment::getDistributionSet)
                .map(distributionSet -> {
                    final MgmtDistributionSet response = MgmtDistributionSetMapper.toResponse(distributionSet);
                    MgmtDistributionSetMapper.addLinks(distributionSet, response);
                    return ResponseEntity.ok(response);
                })
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @Override
    public ResponseEntity<MgmtTargetFilterQuery> postAssignedDistributionSet(
            final Long filterId, final MgmtDistributionSetAutoAssignment autoAssignRequest) {
        final TargetFilterQuery filter = filterManagement.get(filterId);

        final MgmtAutoAssignmentRestRequestBodyPost body = new MgmtAutoAssignmentRestRequestBodyPost();
        body.setName(filter.getName());
        body.setTargetFilterQuery(filter.getQuery());
        body.setDistributionSetId(autoAssignRequest.getId());
        body.setActionType(autoAssignRequest.getType());
        body.setWeight(autoAssignRequest.getWeight());
        body.setConfirmationRequired(autoAssignRequest.getConfirmationRequired());

        final AutoAssignmentManagement.Create create = MgmtAutoAssignmentMapper.fromRequest(body, distributionSetManagement.get(autoAssignRequest.getId()));
        final AutoAssignment created = filterManagement.createLinkedAutoAssignment(filterId, create);
        final MgmtTargetFilterQuery response = MgmtTargetFilterQueryMapper.toResponse(filterManagement.get(filterId), Optional.of(created),
                TenantConfigHelper.isUserConfirmationFlowEnabled(), false);
        MgmtTargetFilterQueryMapper.addLinks(response);

        return ResponseEntity.ok(response);
    }

    @Override
    @AuditLog(entity = "TargetFilter", type = AuditLog.Type.DELETE, description = "Delete Target Filter Assigned Distribution Set")
    public ResponseEntity<Void> deleteAssignedDistributionSet(final Long filterId) {
        filterManagement.deleteLinkedAutoAssignment(filterId);
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