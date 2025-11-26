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

import static org.eclipse.hawkbit.mgmt.rest.resource.util.PagingUtility.sanitizeDistributionSetSortParam;

import java.text.MessageFormat;
import java.util.AbstractMap.SimpleEntry;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.validation.ValidationException;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.audit.AuditLog;
import org.eclipse.hawkbit.mgmt.json.model.MgmtMetadata;
import org.eclipse.hawkbit.mgmt.json.model.MgmtMetadataBodyPut;
import org.eclipse.hawkbit.mgmt.json.model.PagedList;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtDistributionSet;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtDistributionSetRequestBodyPost;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtDistributionSetRequestBodyPut;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtDistributionSetStatistics;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtInvalidateDistributionSetRequestBody;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtTargetAssignmentRequestBody;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtTargetAssignmentResponseBody;
import org.eclipse.hawkbit.mgmt.json.model.softwaremodule.MgmtSoftwareModule;
import org.eclipse.hawkbit.mgmt.json.model.softwaremodule.MgmtSoftwareModuleAssignment;
import org.eclipse.hawkbit.mgmt.json.model.target.MgmtTarget;
import org.eclipse.hawkbit.mgmt.json.model.targetfilter.MgmtTargetFilterQuery;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtDistributionSetRestApi;
import org.eclipse.hawkbit.mgmt.rest.resource.mapper.MgmtDeploymentRequestMapper;
import org.eclipse.hawkbit.mgmt.rest.resource.mapper.MgmtDistributionSetMapper;
import org.eclipse.hawkbit.mgmt.rest.resource.mapper.MgmtRestModelMapper;
import org.eclipse.hawkbit.mgmt.rest.resource.mapper.MgmtSoftwareModuleMapper;
import org.eclipse.hawkbit.mgmt.rest.resource.mapper.MgmtTargetFilterQueryMapper;
import org.eclipse.hawkbit.mgmt.rest.resource.mapper.MgmtTargetMapper;
import org.eclipse.hawkbit.mgmt.rest.resource.util.LogUtility;
import org.eclipse.hawkbit.mgmt.rest.resource.util.PagingUtility;
import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.DistributionSetInvalidationManagement;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.DistributionSetTypeManagement;
import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.helper.TenantConfigHelper;
import org.eclipse.hawkbit.repository.model.DeploymentRequest;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetAssignmentResult;
import org.eclipse.hawkbit.repository.model.DistributionSetInvalidation;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Resource handling for {@link DistributionSet} CRUD operations.
 */
@Slf4j
@RestController
public class MgmtDistributionSetResource implements MgmtDistributionSetRestApi {

    private final SoftwareModuleManagement<? extends SoftwareModule> softwareModuleManagement;
    private final DistributionSetManagement<? extends DistributionSet> distributionSetManagement;
    private final DistributionSetTypeManagement<? extends DistributionSetType> distributionSetTypeManagement;
    private final DistributionSetInvalidationManagement distributionSetInvalidationManagement;
    private final TargetManagement<? extends Target> targetManagement;
    private final TargetFilterQueryManagement<? extends TargetFilterQuery> targetFilterQueryManagement;
    private final DeploymentManagement deployManagement;
    private final SystemManagement systemManagement;
    private final MgmtDistributionSetMapper mgmtDistributionSetMapper;

    @SuppressWarnings("java:S107")
    MgmtDistributionSetResource(
            final SoftwareModuleManagement<? extends SoftwareModule> softwareModuleManagement,
            final DistributionSetManagement<? extends DistributionSet> distributionSetManagement,
            final DistributionSetTypeManagement<? extends DistributionSetType> distributionSetTypeManagement,
            final DistributionSetInvalidationManagement distributionSetInvalidationManagement,
            final TargetManagement<? extends Target> targetManagement,
            final TargetFilterQueryManagement<? extends TargetFilterQuery> targetFilterQueryManagement,
            final DeploymentManagement deployManagement,
            final MgmtDistributionSetMapper mgmtDistributionSetMapper,
            final SystemManagement systemManagement) {
        this.softwareModuleManagement = softwareModuleManagement;
        this.distributionSetManagement = distributionSetManagement;
        this.distributionSetTypeManagement = distributionSetTypeManagement;
        this.distributionSetInvalidationManagement = distributionSetInvalidationManagement;
        this.targetManagement = targetManagement;
        this.targetFilterQueryManagement = targetFilterQueryManagement;
        this.deployManagement = deployManagement;
        this.mgmtDistributionSetMapper = mgmtDistributionSetMapper;
        this.systemManagement = systemManagement;
    }

    @Override
    public ResponseEntity<PagedList<MgmtDistributionSet>> getDistributionSets(
            final String rsqlParam, final int pagingOffsetParam, final int pagingLimitParam, final String sortParam) {
        if (rsqlParam != null && rsqlParam.toLowerCase().contains("complete")) {
            LogUtility.logDeprecated("Usage of MgmtDistributionSetResource.getActions with 'complete': 'complete' distribution set search field is limited and may be removed.");
        }
        final Pageable pageable = PagingUtility.toPageable(pagingOffsetParam, pagingLimitParam, sanitizeDistributionSetSortParam(sortParam));
        final Page<? extends DistributionSet> findDsPage;
        if (rsqlParam != null) {
            findDsPage = distributionSetManagement.findByRsql(rsqlParam, pageable);
        } else {
            findDsPage = distributionSetManagement.findAll(pageable);
        }

        final List<MgmtDistributionSet> rest = MgmtDistributionSetMapper.toResponseFromDsList(findDsPage.getContent());
        return ResponseEntity.ok(new PagedList<>(rest, findDsPage.getTotalElements()));
    }

    @Override
    public ResponseEntity<MgmtDistributionSet> getDistributionSet(final Long distributionSetId) {
        final DistributionSet foundDs = distributionSetManagement.get(distributionSetId);

        final MgmtDistributionSet response = MgmtDistributionSetMapper.toResponse(foundDs);
        MgmtDistributionSetMapper.addLinks(foundDs, response);

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<List<MgmtDistributionSet>> createDistributionSets(final List<MgmtDistributionSetRequestBodyPost> sets) {
        log.debug("creating {} distribution sets", sets.size());
        // set default Ds type if ds type is null
        final String defaultDsKey = systemManagement.getTenantMetadata().getDefaultDsType().getKey();
        sets.stream().filter(ds -> ds.getType() == null).forEach(ds -> ds.setType(defaultDsKey));

        // check if target ds types exist and are not deleted, also caches them
        final Map<String, DistributionSetType> dsTypeKeyToDsType = sets.stream()
                .map(MgmtDistributionSetRequestBodyPost::getType)
                .distinct()
                .collect(Collectors.toMap(
                        Function.identity(),
                        dsTypeKey ->
                                distributionSetTypeManagement.findByKey(dsTypeKey).map(dsType -> {
                                    if (dsType.isDeleted()) {
                                        throw new ValidationException(MessageFormat.format(
                                                "Cannot create Distribution Set from type with key {0}. Distribution Set Type already deleted!",
                                                dsTypeKey));
                                    }
                                    return dsType;
                                }).orElseThrow(() -> new EntityNotFoundException(DistributionSetType.class,  dsTypeKey))));

        final Collection<? extends DistributionSet> createdDSets =
                distributionSetManagement.create(mgmtDistributionSetMapper.fromRequest(sets, defaultDsKey, dsTypeKeyToDsType));

        log.debug("{} distribution sets created, return status {}", sets.size(), HttpStatus.CREATED);
        return new ResponseEntity<>(MgmtDistributionSetMapper.toResponseDistributionSets(createdDSets), HttpStatus.CREATED);
    }

    @Override
    @AuditLog(entity = "DistributionSet", type = AuditLog.Type.DELETE, description = "Delete Distribution Set")
    public ResponseEntity<Void> deleteDistributionSet(final Long distributionSetId) {
        distributionSetManagement.delete(distributionSetId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @AuditLog(entity = "DistributionSet", type = AuditLog.Type.UPDATE, description = "Update Distribution Set")
    public ResponseEntity<MgmtDistributionSet> updateDistributionSet(
            final Long distributionSetId,
            final MgmtDistributionSetRequestBodyPut toUpdate) {
        final DistributionSet updated = distributionSetManagement.update(DistributionSetManagement.Update.builder()
                .id(distributionSetId).name(toUpdate.getName()).description(toUpdate.getDescription())
                .version(toUpdate.getVersion()).locked(toUpdate.getLocked())
                .requiredMigrationStep(toUpdate.getRequiredMigrationStep())
                .build());

        final MgmtDistributionSet response = MgmtDistributionSetMapper.toResponse(updated);
        MgmtDistributionSetMapper.addLinks(updated, response);

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<PagedList<MgmtTarget>> getAssignedTargets(
            final Long distributionSetId, final String rsqlParam,
            final int pagingOffsetParam, final int pagingLimitParam, final String sortParam) {
        final Pageable pageable = PagingUtility.toPageable(pagingOffsetParam, pagingLimitParam, sanitizeDistributionSetSortParam(sortParam));
        final Page<Target> targetsAssignedDS;
        if (rsqlParam != null) {
            targetsAssignedDS = targetManagement.findByAssignedDistributionSetAndRsql(distributionSetId, rsqlParam, pageable);
        } else {
            targetsAssignedDS = targetManagement.findByAssignedDistributionSet(distributionSetId, pageable);
        }

        return ResponseEntity.ok(new PagedList<>(
                MgmtTargetMapper.toResponse(targetsAssignedDS.getContent()), targetsAssignedDS.getTotalElements()));
    }

    @Override
    public ResponseEntity<PagedList<MgmtTarget>> getInstalledTargets(
            final Long distributionSetId, final String rsqlParam,
            final int pagingOffsetParam, final int pagingLimitParam, final String sortParam) {
        // check if distribution set exists otherwise throw exception immediately
        distributionSetManagement.get(distributionSetId);
        final Pageable pageable = PagingUtility.toPageable(pagingOffsetParam, pagingLimitParam, sanitizeDistributionSetSortParam(sortParam));
        final Page<Target> targetsInstalledDS;
        if (rsqlParam != null) {
            targetsInstalledDS = this.targetManagement.findByInstalledDistributionSetAndRsql(distributionSetId, rsqlParam, pageable);
        } else {
            targetsInstalledDS = this.targetManagement.findByInstalledDistributionSet(distributionSetId, pageable);
        }

        return ResponseEntity.ok(new PagedList<>(
                MgmtTargetMapper.toResponse(targetsInstalledDS.getContent()), targetsInstalledDS.getTotalElements()));
    }

    @Override
    public ResponseEntity<PagedList<MgmtTargetFilterQuery>> getAutoAssignTargetFilterQueries(
            final Long distributionSetId, final String rsqlParam,
            final int pagingOffsetParam, final int pagingLimitParam, final String sortParam) {
        final Pageable pageable = PagingUtility.toPageable(pagingOffsetParam, pagingLimitParam, sanitizeDistributionSetSortParam(sortParam));
        final Page<TargetFilterQuery> targetFilterQueries = targetFilterQueryManagement
                .findByAutoAssignDSAndRsql(distributionSetId, rsqlParam, pageable);

        return ResponseEntity.ok(new PagedList<>(
                MgmtTargetFilterQueryMapper.toResponse(
                        targetFilterQueries.getContent(), TenantConfigHelper.isUserConfirmationFlowEnabled(), false),
                targetFilterQueries.getTotalElements()));
    }

    @Override
    public ResponseEntity<MgmtTargetAssignmentResponseBody> createAssignedTarget(
            final Long distributionSetId, final List<MgmtTargetAssignmentRequestBody> assignments, final Boolean offline) {
        if (offline != null && offline) {
            final List<Entry<String, Long>> offlineAssignments = assignments.stream()
                    .map(assignment -> new SimpleEntry<>(assignment.getId(), distributionSetId))
                    .collect(Collectors.toList());
            return ResponseEntity.ok(mgmtDistributionSetMapper
                    .toResponse(deployManagement.offlineAssignedDistributionSets(offlineAssignments)));
        }

        final List<DeploymentRequest> deploymentRequests = assignments.stream().map(dsAssignment -> {
            final boolean isConfirmationRequired = dsAssignment.getConfirmationRequired() == null
                    ? TenantConfigHelper.isUserConfirmationFlowEnabled()
                    : dsAssignment.getConfirmationRequired();
            return MgmtDeploymentRequestMapper.createAssignmentRequestBuilder(dsAssignment, distributionSetId)
                    .confirmationRequired(isConfirmationRequired).build();
        }).toList();

        final List<DistributionSetAssignmentResult> assignmentResults = deployManagement.assignDistributionSets(deploymentRequests, null);
        return ResponseEntity.ok(mgmtDistributionSetMapper.toResponse(assignmentResults));
    }

    @Override
    public ResponseEntity<Void> createMetadata(final Long distributionSetId, final List<MgmtMetadata> metadataRest) {
        distributionSetManagement.createMetadata(distributionSetId, MgmtDistributionSetMapper.fromRequestDsMetadata(metadataRest));
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Override
    public ResponseEntity<PagedList<MgmtMetadata>> getMetadata(final Long distributionSetId) {
        final Map<String, String> metadata = distributionSetManagement.getMetadata(distributionSetId);
        return ResponseEntity.ok(new PagedList<>(MgmtDistributionSetMapper.toResponseDsMetadata(metadata), metadata.size()));
    }

    @Override
    public ResponseEntity<MgmtMetadata> getMetadataValue(final Long distributionSetId, final String metadataKey) {
        final String metadataValue = distributionSetManagement.getMetadata(distributionSetId).get(metadataKey);
        if (metadataValue == null) {
            throw new EntityNotFoundException("Target metadata", distributionSetId + ":" + metadataKey);
        }
        return ResponseEntity.ok(MgmtDistributionSetMapper.toResponseDsMetadata(metadataKey, metadataValue));
    }

    @Override
    public ResponseEntity<Void> updateMetadata(final Long distributionSetId, final String metadataKey, final MgmtMetadataBodyPut metadata) {
        distributionSetManagement.createMetadata(distributionSetId, metadataKey, metadata.getValue());
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteMetadata(final Long distributionSetId, final String metadataKey) {
        distributionSetManagement.deleteMetadata(distributionSetId, metadataKey);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> assignSoftwareModules(final Long distributionSetId,
            final List<MgmtSoftwareModuleAssignment> softwareModuleIDs) {
        distributionSetManagement.assignSoftwareModules(
                distributionSetId,
                softwareModuleIDs.stream()
                        .map(MgmtSoftwareModuleAssignment::getId)
                        .toList());
        return ResponseEntity.noContent().build();
    }

    @Override
    @AuditLog(entity = "DistributionSet", type = AuditLog.Type.DELETE, description = "Delete Assigned Distribution Set")
    public ResponseEntity<Void> deleteAssignSoftwareModules(final Long distributionSetId, final Long softwareModuleId) {
        distributionSetManagement.unassignSoftwareModule(distributionSetId, softwareModuleId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<PagedList<MgmtSoftwareModule>> getAssignedSoftwareModules(
            final Long distributionSetId,
            final int pagingOffsetParam, final int pagingLimitParam, final String sortParam) {
        final Pageable pageable = PagingUtility.toPageable(pagingOffsetParam, pagingLimitParam, sanitizeDistributionSetSortParam(sortParam));
        final Page<? extends SoftwareModule> softwareModules = softwareModuleManagement.findByAssignedTo(distributionSetId, pageable);
        return ResponseEntity.ok(
                new PagedList<>(MgmtSoftwareModuleMapper.toResponse(softwareModules.getContent()), softwareModules.getTotalElements()));
    }

    @Override
    public ResponseEntity<MgmtDistributionSetStatistics> getRolloutsCountByStatusForDistributionSet(final Long distributionSetId) {
        final MgmtDistributionSetStatistics.Builder statistics = new MgmtDistributionSetStatistics.Builder(false);
        distributionSetManagement.countRolloutsByStatusForDistributionSet(distributionSetId).forEach(statistic ->
                statistics.addTotalRolloutPerStatus(String.valueOf(statistic.getName()), Long.parseLong(statistic.getData().toString())));
        return ResponseEntity.ok(statistics.build());
    }

    @Override
    public ResponseEntity<MgmtDistributionSetStatistics> getActionsCountByStatusForDistributionSet(final Long distributionSetId) {
        MgmtDistributionSetStatistics.Builder statistics = new MgmtDistributionSetStatistics.Builder(false);
        distributionSetManagement.countActionsByStatusForDistributionSet(distributionSetId).forEach(statistic ->
                statistics.addTotalActionPerStatus(String.valueOf(statistic.getName()), Long.parseLong(statistic.getData().toString())));
        return ResponseEntity.ok(statistics.build());
    }

    @Override
    public ResponseEntity<MgmtDistributionSetStatistics> getAutoAssignmentsCountForDistributionSet(final Long distributionSetId) {
        final MgmtDistributionSetStatistics.Builder statistics = new MgmtDistributionSetStatistics.Builder(false);
        statistics.addTotalAutoAssignments(distributionSetManagement.countAutoAssignmentsForDistributionSet(distributionSetId));
        return ResponseEntity.ok(statistics.build());
    }

    @Override
    public ResponseEntity<MgmtDistributionSetStatistics> getStatisticsForDistributionSet(final Long distributionSetId) {
        final MgmtDistributionSetStatistics.Builder statistics = new MgmtDistributionSetStatistics.Builder(true);
        distributionSetManagement.countRolloutsByStatusForDistributionSet(distributionSetId).forEach(statistic ->
                statistics.addTotalRolloutPerStatus(String.valueOf(statistic.getName()), Long.parseLong(statistic.getData().toString())));
        distributionSetManagement.countActionsByStatusForDistributionSet(distributionSetId).forEach(statistic ->
                statistics.addTotalActionPerStatus(String.valueOf(statistic.getName()), Long.parseLong(statistic.getData().toString())));
        statistics.addTotalAutoAssignments(distributionSetManagement.countAutoAssignmentsForDistributionSet(distributionSetId));
        return ResponseEntity.ok(statistics.build());
    }

    @Override
    @AuditLog(entity = "DistributionSet", type = AuditLog.Type.DELETE, description = "Invalidate Distribution Set")
    public ResponseEntity<Void> invalidateDistributionSet(
            final Long distributionSetId, final MgmtInvalidateDistributionSetRequestBody invalidateRequestBody) {
        distributionSetInvalidationManagement.invalidateDistributionSet(new DistributionSetInvalidation(
                List.of(distributionSetId),
                MgmtRestModelMapper.convertCancelationType(invalidateRequestBody.getActionCancelationType())));
        return ResponseEntity.noContent().build();
    }
}