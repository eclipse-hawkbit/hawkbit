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

import java.text.MessageFormat;
import java.util.AbstractMap.SimpleEntry;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.validation.ValidationException;

import lombok.extern.slf4j.Slf4j;
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
import org.eclipse.hawkbit.mgmt.rest.resource.util.PagingUtility;
import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.DistributionSetInvalidationManagement;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.DistributionSetTypeManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.OffsetBasedPageRequest;
import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.model.DeploymentRequest;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetAssignmentResult;
import org.eclipse.hawkbit.repository.model.DistributionSetInvalidation;
import org.eclipse.hawkbit.repository.model.DistributionSetMetadata;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.eclipse.hawkbit.utils.TenantConfigHelper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Resource handling for {@link DistributionSet} CRUD operations.
 */
@Slf4j
@RestController
public class MgmtDistributionSetResource implements MgmtDistributionSetRestApi {

    private final SoftwareModuleManagement softwareModuleManagement;
    private final TargetManagement targetManagement;
    private final TargetFilterQueryManagement targetFilterQueryManagement;
    private final DeploymentManagement deployManagement;
    private final SystemManagement systemManagement;
    private final EntityFactory entityFactory;
    private final DistributionSetManagement distributionSetManagement;
    private final DistributionSetTypeManagement distributionSetTypeManagement;
    private final SystemSecurityContext systemSecurityContext;
    private final DistributionSetInvalidationManagement distributionSetInvalidationManagement;
    private final TenantConfigHelper tenantConfigHelper;

    MgmtDistributionSetResource(
            final SoftwareModuleManagement softwareModuleManagement,
            final TargetManagement targetManagement, final TargetFilterQueryManagement targetFilterQueryManagement,
            final DeploymentManagement deployManagement, final SystemManagement systemManagement,
            final EntityFactory entityFactory, final DistributionSetManagement distributionSetManagement,
            final DistributionSetTypeManagement distributionSetTypeManagement, final SystemSecurityContext systemSecurityContext,
            final DistributionSetInvalidationManagement distributionSetInvalidationManagement,
            final TenantConfigurationManagement tenantConfigurationManagement) {
        this.softwareModuleManagement = softwareModuleManagement;
        this.targetManagement = targetManagement;
        this.targetFilterQueryManagement = targetFilterQueryManagement;
        this.deployManagement = deployManagement;
        this.systemManagement = systemManagement;
        this.entityFactory = entityFactory;
        this.distributionSetManagement = distributionSetManagement;
        this.distributionSetTypeManagement = distributionSetTypeManagement;
        this.systemSecurityContext = systemSecurityContext;
        this.distributionSetInvalidationManagement = distributionSetInvalidationManagement;
        this.tenantConfigHelper = TenantConfigHelper.usingContext(systemSecurityContext, tenantConfigurationManagement);
    }

    @Override
    public ResponseEntity<PagedList<MgmtDistributionSet>> getDistributionSets(
            final int pagingOffsetParam, final int pagingLimitParam, final String sortParam, final String rsqlParam) {
        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeDistributionSetSortParam(sortParam);

        final Pageable pageable = new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting);
        final Slice<DistributionSet> findDsPage;
        final long countModulesAll;
        if (rsqlParam != null) {
            findDsPage = distributionSetManagement.findByRsql(pageable, rsqlParam);
            countModulesAll = ((Page<DistributionSet>) findDsPage).getTotalElements();
        } else {
            findDsPage = distributionSetManagement.findAll(pageable);
            countModulesAll = distributionSetManagement.count();
        }

        final List<MgmtDistributionSet> rest = MgmtDistributionSetMapper.toResponseFromDsList(findDsPage.getContent());
        return ResponseEntity.ok(new PagedList<>(rest, countModulesAll));
    }

    @Override
    public ResponseEntity<MgmtDistributionSet> getDistributionSet(
            final Long distributionSetId) {
        final DistributionSet foundDs = distributionSetManagement.getOrElseThrowException(distributionSetId);

        final MgmtDistributionSet response = MgmtDistributionSetMapper.toResponse(foundDs);
        MgmtDistributionSetMapper.addLinks(foundDs, response);

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<List<MgmtDistributionSet>> createDistributionSets(
            final List<MgmtDistributionSetRequestBodyPost> sets) {
        log.debug("creating {} distribution sets", sets.size());
        // set default Ds type if ds type is null
        final String defaultDsKey = systemSecurityContext
                .runAsSystem(systemManagement.getTenantMetadata().getDefaultDsType()::getKey);
        sets.stream().filter(ds -> ds.getType() == null).forEach(ds -> ds.setType(defaultDsKey));

        //check if there is already deleted DS Type
        for (MgmtDistributionSetRequestBodyPost ds : sets) {
            final Optional<DistributionSetType> opt = distributionSetTypeManagement.getByKey(ds.getType());
            opt.ifPresent(dsType -> {
                if (dsType.isDeleted()) {
                    final String text = "Cannot create Distribution Set from type with key {0}. Distribution Set Type already deleted!";
                    final String message = MessageFormat.format(text, dsType.getKey());
                    throw new ValidationException(message);
                }
            });
        }

        final Collection<DistributionSet> createdDSets = distributionSetManagement
                .create(MgmtDistributionSetMapper.dsFromRequest(sets, entityFactory));

        log.debug("{} distribution sets created, return status {}", sets.size(), HttpStatus.CREATED);
        return new ResponseEntity<>(MgmtDistributionSetMapper.toResponseDistributionSets(createdDSets), HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<Void> deleteDistributionSet(final Long distributionSetId) {
        distributionSetManagement.delete(distributionSetId);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<MgmtDistributionSet> updateDistributionSet(
            final Long distributionSetId,
            final MgmtDistributionSetRequestBodyPut toUpdate) {
        final DistributionSet updated = distributionSetManagement.update(entityFactory.distributionSet()
                .update(distributionSetId).name(toUpdate.getName()).description(toUpdate.getDescription())
                .version(toUpdate.getVersion()).locked(toUpdate.getLocked())
                .requiredMigrationStep(toUpdate.getRequiredMigrationStep()));

        final MgmtDistributionSet response = MgmtDistributionSetMapper.toResponse(updated);
        MgmtDistributionSetMapper.addLinks(updated, response);

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<PagedList<MgmtTarget>> getAssignedTargets(
            final Long distributionSetId,
            final int pagingOffsetParam, final int pagingLimitParam, final String sortParam, final String rsqlParam) {
        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeTargetSortParam(sortParam);

        final Pageable pageable = new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting);
        final Page<Target> targetsAssignedDS;
        if (rsqlParam != null) {
            targetsAssignedDS = this.targetManagement.findByAssignedDistributionSetAndRsql(pageable, distributionSetId,
                    rsqlParam);
        } else {
            targetsAssignedDS = this.targetManagement.findByAssignedDistributionSet(pageable, distributionSetId);
        }

        return ResponseEntity
                .ok(new PagedList<>(MgmtTargetMapper.toResponse(targetsAssignedDS.getContent(), tenantConfigHelper),
                        targetsAssignedDS.getTotalElements()));
    }

    @Override
    public ResponseEntity<PagedList<MgmtTarget>> getInstalledTargets(
            final Long distributionSetId,
            final int pagingOffsetParam, final int pagingLimitParam, final String sortParam, final String rsqlParam) {
        // check if distribution set exists otherwise throw exception
        // immediately
        distributionSetManagement.getOrElseThrowException(distributionSetId);

        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeTargetSortParam(sortParam);

        final Pageable pageable = new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting);
        final Page<Target> targetsInstalledDS;
        if (rsqlParam != null) {
            targetsInstalledDS = this.targetManagement.findByInstalledDistributionSetAndRsql(pageable, distributionSetId, rsqlParam);
        } else {
            targetsInstalledDS = this.targetManagement.findByInstalledDistributionSet(pageable, distributionSetId);
        }

        return ResponseEntity
                .ok(new PagedList<>(MgmtTargetMapper.toResponse(targetsInstalledDS.getContent(), tenantConfigHelper),
                        targetsInstalledDS.getTotalElements()));
    }

    @Override
    public ResponseEntity<PagedList<MgmtTargetFilterQuery>> getAutoAssignTargetFilterQueries(
            final Long distributionSetId,
            final int pagingOffsetParam, final int pagingLimitParam, final String sortParam, final String rsqlParam) {
        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeTargetFilterQuerySortParam(sortParam);

        final Pageable pageable = new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting);
        final Page<TargetFilterQuery> targetFilterQueries = targetFilterQueryManagement
                .findByAutoAssignDSAndRsql(pageable, distributionSetId, rsqlParam);

        return ResponseEntity
                .ok(new PagedList<>(MgmtTargetFilterQueryMapper.toResponse(targetFilterQueries.getContent(),
                        tenantConfigHelper.isConfirmationFlowEnabled(), false), targetFilterQueries.getTotalElements()));
    }

    @Override
    public ResponseEntity<MgmtTargetAssignmentResponseBody> createAssignedTarget(
            final Long distributionSetId, final List<MgmtTargetAssignmentRequestBody> assignments, final Boolean offline) {
        if (offline != null && offline) {
            final List<Entry<String, Long>> offlineAssignments = assignments.stream()
                    .map(assignment -> new SimpleEntry<>(assignment.getId(), distributionSetId))
                    .collect(Collectors.toList());
            return ResponseEntity.ok(MgmtDistributionSetMapper
                    .toResponse(deployManagement.offlineAssignedDistributionSets(offlineAssignments)));
        }

        final List<DeploymentRequest> deploymentRequests = assignments.stream().map(dsAssignment -> {
            final boolean isConfirmationRequired = dsAssignment.getConfirmationRequired() == null
                    ? tenantConfigHelper.isConfirmationFlowEnabled()
                    : dsAssignment.getConfirmationRequired();
            return MgmtDeploymentRequestMapper.createAssignmentRequestBuilder(dsAssignment, distributionSetId)
                    .setConfirmationRequired(isConfirmationRequired).build();
        }).collect(Collectors.toList());

        final List<DistributionSetAssignmentResult> assignmentResults = deployManagement
                .assignDistributionSets(deploymentRequests);
        return ResponseEntity.ok(MgmtDistributionSetMapper.toResponse(assignmentResults));
    }

    @Override
    public ResponseEntity<PagedList<MgmtMetadata>> getMetadata(
            final Long distributionSetId,
            final int pagingOffsetParam, final int pagingLimitParam, final String sortParam, final String rsqlParam) {
        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeDistributionSetMetadataSortParam(sortParam);

        final Pageable pageable = new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting);
        final Page<DistributionSetMetadata> metaDataPage;

        if (rsqlParam != null) {
            metaDataPage = distributionSetManagement.findMetaDataByDistributionSetIdAndRsql(pageable, distributionSetId,
                    rsqlParam);
        } else {
            metaDataPage = distributionSetManagement.findMetaDataByDistributionSetId(pageable, distributionSetId);
        }

        return ResponseEntity
                .ok(new PagedList<>(MgmtDistributionSetMapper.toResponseDsMetadata(metaDataPage.getContent()), metaDataPage.getTotalElements()));
    }

    @Override
    public ResponseEntity<MgmtMetadata> getMetadataValue(final Long distributionSetId, final String metadataKey) {
        // check if distribution set exists otherwise throw exception immediately
        final DistributionSetMetadata findOne = distributionSetManagement
                .getMetaDataByDistributionSetId(distributionSetId, metadataKey)
                .orElseThrow(() -> new EntityNotFoundException(DistributionSetMetadata.class, distributionSetId, metadataKey));
        return ResponseEntity.ok(MgmtDistributionSetMapper.toResponseDsMetadata(findOne));
    }

    @Override
    public ResponseEntity<MgmtMetadata> updateMetadata(
            final Long distributionSetId, final String metadataKey, final MgmtMetadataBodyPut metadata) {
        // check if distribution set exists otherwise throw exception immediately
        final DistributionSetMetadata updated = distributionSetManagement.updateMetaData(distributionSetId,
                entityFactory.generateDsMetadata(metadataKey, metadata.getValue()));
        return ResponseEntity.ok(MgmtDistributionSetMapper.toResponseDsMetadata(updated));
    }

    @Override
    public ResponseEntity<Void> deleteMetadata(final Long distributionSetId, final String metadataKey) {
        // check if distribution set exists otherwise throw exception immediately
        distributionSetManagement.deleteMetaData(distributionSetId, metadataKey);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<List<MgmtMetadata>> createMetadata(final Long distributionSetId, final List<MgmtMetadata> metadataRest) {
        // check if distribution set exists otherwise throw exception immediately
        final List<DistributionSetMetadata> created = distributionSetManagement.createMetaData(distributionSetId,
                MgmtDistributionSetMapper.fromRequestDsMetadata(metadataRest, entityFactory));
        return new ResponseEntity<>(MgmtDistributionSetMapper.toResponseDsMetadata(created), HttpStatus.CREATED);

    }

    @Override
    public ResponseEntity<Void> assignSoftwareModules(
            final Long distributionSetId, final List<MgmtSoftwareModuleAssignment> softwareModuleIDs) {
        distributionSetManagement.assignSoftwareModules(
                distributionSetId,
                softwareModuleIDs.stream()
                        .map(MgmtSoftwareModuleAssignment::getId)
                        .collect(Collectors.toList()));
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> deleteAssignSoftwareModules(final Long distributionSetId, final Long softwareModuleId) {
        distributionSetManagement.unassignSoftwareModule(distributionSetId, softwareModuleId);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<PagedList<MgmtSoftwareModule>> getAssignedSoftwareModules(
            final Long distributionSetId,
            final int pagingOffsetParam, final int pagingLimitParam, final String sortParam) {
        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeSoftwareModuleSortParam(sortParam);
        final Pageable pageable = new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting);
        final Page<SoftwareModule> softwaremodules = softwareModuleManagement.findByAssignedTo(pageable, distributionSetId);
        return ResponseEntity.ok(new PagedList<>(MgmtSoftwareModuleMapper.toResponse(
                softwaremodules.getContent()), softwaremodules.getTotalElements()));
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
    public ResponseEntity<Void> invalidateDistributionSet(
            final Long distributionSetId, final MgmtInvalidateDistributionSetRequestBody invalidateRequestBody) {
        distributionSetInvalidationManagement
                .invalidateDistributionSet(
                        new DistributionSetInvalidation(Collections.singletonList(distributionSetId),
                                MgmtRestModelMapper.convertCancelationType(invalidateRequestBody.getActionCancelationType()),
                                invalidateRequestBody.isCancelRollouts()));
        return ResponseEntity.ok().build();
    }
}