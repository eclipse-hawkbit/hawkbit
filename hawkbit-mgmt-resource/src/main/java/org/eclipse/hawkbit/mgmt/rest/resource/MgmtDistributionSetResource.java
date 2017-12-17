/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.rest.resource;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.mgmt.json.model.MgmtMetadata;
import org.eclipse.hawkbit.mgmt.json.model.MgmtMetadataBodyPut;
import org.eclipse.hawkbit.mgmt.json.model.PagedList;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtDistributionSet;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtDistributionSetRequestBodyPost;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtDistributionSetRequestBodyPut;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtTargetAssignmentRequestBody;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtTargetAssignmentResponseBody;
import org.eclipse.hawkbit.mgmt.json.model.softwaremodule.MgmtSoftwareModule;
import org.eclipse.hawkbit.mgmt.json.model.softwaremodule.MgmtSoftwareModuleAssigment;
import org.eclipse.hawkbit.mgmt.json.model.target.MgmtTarget;
import org.eclipse.hawkbit.mgmt.json.model.targetfilter.MgmtTargetFilterQuery;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtDistributionSetRestApi;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants;
import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.OffsetBasedPageRequest;
import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetMetadata;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.eclipse.hawkbit.repository.model.TargetWithActionType;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Resource handling for {@link DistributionSet} CRUD operations.
 */
@RestController
public class MgmtDistributionSetResource implements MgmtDistributionSetRestApi {
    private static final Logger LOG = LoggerFactory.getLogger(MgmtDistributionSetResource.class);

    @Autowired
    private SoftwareModuleManagement softwareModuleManagement;

    @Autowired
    private TargetManagement targetManagement;

    @Autowired
    private TargetFilterQueryManagement targetFilterQueryManagement;

    @Autowired
    private DeploymentManagement deployManagament;

    @Autowired
    private SystemManagement systemManagement;

    @Autowired
    private EntityFactory entityFactory;

    @Autowired
    private DistributionSetManagement distributionSetManagement;

    @Autowired
    private SystemSecurityContext systemSecurityContext;

    @Override
    public ResponseEntity<PagedList<MgmtDistributionSet>> getDistributionSets(
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) final int pagingOffsetParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) final int pagingLimitParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false) final String sortParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false) final String rsqlParam) {

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
            @PathVariable("distributionSetId") final Long distributionSetId) {
        final DistributionSet foundDs = findDistributionSetWithExceptionIfNotFound(distributionSetId);

        final MgmtDistributionSet response = MgmtDistributionSetMapper.toResponse(foundDs);
        MgmtDistributionSetMapper.addLinks(foundDs, response);

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<List<MgmtDistributionSet>> createDistributionSets(
            @RequestBody final List<MgmtDistributionSetRequestBodyPost> sets) {

        LOG.debug("creating {} distribution sets", sets.size());
        // set default Ds type if ds type is null
        final String defaultDsKey = systemSecurityContext
                .runAsSystem(systemManagement.getTenantMetadata().getDefaultDsType()::getKey);
        sets.stream().filter(ds -> ds.getType() == null).forEach(ds -> ds.setType(defaultDsKey));

        final Collection<DistributionSet> createdDSets = distributionSetManagement
                .create(MgmtDistributionSetMapper.dsFromRequest(sets, entityFactory));

        LOG.debug("{} distribution sets created, return status {}", sets.size(), HttpStatus.CREATED);
        return new ResponseEntity<>(MgmtDistributionSetMapper.toResponseDistributionSets(createdDSets),
                HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<Void> deleteDistributionSet(@PathVariable("distributionSetId") final Long distributionSetId) {
        distributionSetManagement.delete(distributionSetId);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<MgmtDistributionSet> updateDistributionSet(
            @PathVariable("distributionSetId") final Long distributionSetId,
            @RequestBody final MgmtDistributionSetRequestBodyPut toUpdate) {

        final DistributionSet updated = distributionSetManagement.update(entityFactory.distributionSet()
                .update(distributionSetId).name(toUpdate.getName()).description(toUpdate.getDescription())
                .version(toUpdate.getVersion()).requiredMigrationStep(toUpdate.isRequiredMigrationStep()));

        final MgmtDistributionSet response = MgmtDistributionSetMapper.toResponse(updated);
        MgmtDistributionSetMapper.addLinks(updated, response);

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<PagedList<MgmtTarget>> getAssignedTargets(
            @PathVariable("distributionSetId") final Long distributionSetId,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) final int pagingOffsetParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) final int pagingLimitParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false) final String sortParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false) final String rsqlParam) {

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

        return ResponseEntity.ok(new PagedList<>(MgmtTargetMapper.toResponse(targetsAssignedDS.getContent()),
                targetsAssignedDS.getTotalElements()));
    }

    @Override
    public ResponseEntity<PagedList<MgmtTarget>> getInstalledTargets(
            @PathVariable("distributionSetId") final Long distributionSetId,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) final int pagingOffsetParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) final int pagingLimitParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false) final String sortParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false) final String rsqlParam) {
        // check if distribution set exists otherwise throw exception
        // immediately
        findDistributionSetWithExceptionIfNotFound(distributionSetId);

        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeTargetSortParam(sortParam);

        final Pageable pageable = new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting);
        final Page<Target> targetsInstalledDS;
        if (rsqlParam != null) {
            targetsInstalledDS = this.targetManagement.findByInstalledDistributionSetAndRsql(pageable,
                    distributionSetId, rsqlParam);
        } else {
            targetsInstalledDS = this.targetManagement.findByInstalledDistributionSet(pageable, distributionSetId);
        }

        return ResponseEntity.ok(new PagedList<>(MgmtTargetMapper.toResponse(targetsInstalledDS.getContent()),
                targetsInstalledDS.getTotalElements()));
    }

    @Override
    public ResponseEntity<PagedList<MgmtTargetFilterQuery>> getAutoAssignTargetFilterQueries(
            @PathVariable("distributionSetId") final Long distributionSetId,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) final int pagingOffsetParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) final int pagingLimitParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false) final String sortParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false) final String rsqlParam) {
        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeTargetFilterQuerySortParam(sortParam);

        final Pageable pageable = new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting);
        final Page<TargetFilterQuery> targetFilterQueries = targetFilterQueryManagement
                .findByAutoAssignDSAndRsql(pageable, distributionSetId, rsqlParam);

        return ResponseEntity
                .ok(new PagedList<>(MgmtTargetFilterQueryMapper.toResponse(targetFilterQueries.getContent()),
                        targetFilterQueries.getTotalElements()));
    }

    @Override
    public ResponseEntity<MgmtTargetAssignmentResponseBody> createAssignedTarget(
            @PathVariable("distributionSetId") final Long distributionSetId,
            @RequestBody final List<MgmtTargetAssignmentRequestBody> assignments,
            @RequestParam(value = "offline", required = false) final boolean offline) {

        if (offline) {
            return ResponseEntity.ok(MgmtDistributionSetMapper
                    .toResponse(this.deployManagament.offlineAssignedDistributionSet(distributionSetId, assignments
                            .stream().map(MgmtTargetAssignmentRequestBody::getId).collect(Collectors.toList()))));
        }

        return ResponseEntity.ok(MgmtDistributionSetMapper.toResponse(this.deployManagament.assignDistributionSet(
                distributionSetId,
                assignments.stream()
                        .map(assignment -> new TargetWithActionType(assignment.getId(),
                                MgmtRestModelMapper.convertActionType(assignment.getType()), assignment.getForcetime()))
                        .collect(Collectors.toList()))));

    }

    @Override
    public ResponseEntity<PagedList<MgmtMetadata>> getMetadata(
            @PathVariable("distributionSetId") final Long distributionSetId,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) final int pagingOffsetParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) final int pagingLimitParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false) final String sortParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false) final String rsqlParam) {

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
                .ok(new PagedList<>(MgmtDistributionSetMapper.toResponseDsMetadata(metaDataPage.getContent()),
                        metaDataPage.getTotalElements()));

    }

    @Override
    public ResponseEntity<MgmtMetadata> getMetadataValue(
            @PathVariable("distributionSetId") final Long distributionSetId,
            @PathVariable("metadataKey") final String metadataKey) {
        // check if distribution set exists otherwise throw exception
        // immediately
        final DistributionSetMetadata findOne = distributionSetManagement
                .getMetaDataByDistributionSetId(distributionSetId, metadataKey)
                .orElseThrow(() -> new EntityNotFoundException(DistributionSetMetadata.class, distributionSetId,
                        metadataKey));
        return ResponseEntity.ok(MgmtDistributionSetMapper.toResponseDsMetadata(findOne));
    }

    @Override
    public ResponseEntity<MgmtMetadata> updateMetadata(
            @PathVariable("distributionSetId") final Long distributionSetId,
            @PathVariable("metadataKey") final String metadataKey, @RequestBody final MgmtMetadataBodyPut metadata) {
        // check if distribution set exists otherwise throw exception
        // immediately
        final DistributionSetMetadata updated = distributionSetManagement.updateMetaData(distributionSetId,
                entityFactory.generateMetadata(metadataKey, metadata.getValue()));
        return ResponseEntity.ok(MgmtDistributionSetMapper.toResponseDsMetadata(updated));
    }

    @Override
    public ResponseEntity<Void> deleteMetadata(@PathVariable("distributionSetId") final Long distributionSetId,
            @PathVariable("metadataKey") final String metadataKey) {
        // check if distribution set exists otherwise throw exception
        // immediately
        distributionSetManagement.deleteMetaData(distributionSetId, metadataKey);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<List<MgmtMetadata>> createMetadata(
            @PathVariable("distributionSetId") final Long distributionSetId,
            @RequestBody final List<MgmtMetadata> metadataRest) {
        // check if distribution set exists otherwise throw exception
        // immediately
        final List<DistributionSetMetadata> created = distributionSetManagement.createMetaData(distributionSetId,
                MgmtDistributionSetMapper.fromRequestDsMetadata(metadataRest, entityFactory));
        return new ResponseEntity<>(MgmtDistributionSetMapper.toResponseDsMetadata(created), HttpStatus.CREATED);

    }

    @Override
    public ResponseEntity<Void> assignSoftwareModules(@PathVariable("distributionSetId") final Long distributionSetId,
            @RequestBody final List<MgmtSoftwareModuleAssigment> softwareModuleIDs) {

        distributionSetManagement.assignSoftwareModules(distributionSetId,
                softwareModuleIDs.stream().map(MgmtSoftwareModuleAssigment::getId).collect(Collectors.toList()));
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> deleteAssignSoftwareModules(
            @PathVariable("distributionSetId") final Long distributionSetId,
            @PathVariable("softwareModuleId") final Long softwareModuleId) {
        distributionSetManagement.unassignSoftwareModule(distributionSetId, softwareModuleId);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<PagedList<MgmtSoftwareModule>> getAssignedSoftwareModules(
            @PathVariable("distributionSetId") final Long distributionSetId,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) final int pagingOffsetParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) final int pagingLimitParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false) final String sortParam) {

        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeSoftwareModuleSortParam(sortParam);
        final Pageable pageable = new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting);
        final Page<SoftwareModule> softwaremodules = softwareModuleManagement.findByAssignedTo(pageable,
                distributionSetId);
        return ResponseEntity.ok(new PagedList<>(MgmtSoftwareModuleMapper.toResponse(softwaremodules.getContent()),
                softwaremodules.getTotalElements()));
    }

    private DistributionSet findDistributionSetWithExceptionIfNotFound(final Long distributionSetId) {
        return distributionSetManagement.get(distributionSetId)
                .orElseThrow(() -> new EntityNotFoundException(DistributionSet.class, distributionSetId));
    }
}
