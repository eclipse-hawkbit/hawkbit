/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.rest.resource;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.DistributionSetAssignmentResult;
import org.eclipse.hawkbit.repository.DistributionSetFields;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.DistributionSetMetadataFields;
import org.eclipse.hawkbit.repository.TargetFields;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.SoftwareManagement;
import org.eclipse.hawkbit.repository.jpa.SystemManagement;
import org.eclipse.hawkbit.repository.jpa.TargetManagement;
import org.eclipse.hawkbit.repository.jpa.TargetWithActionType;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetMetadata;
import org.eclipse.hawkbit.repository.model.DsMetadataCompositeKey;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.rsql.RSQLUtility;
import org.eclipse.hawkbit.rest.resource.api.DistributionSetRestApi;
import org.eclipse.hawkbit.rest.resource.helper.RestResourceConversionHelper;
import org.eclipse.hawkbit.rest.resource.model.MetadataRest;
import org.eclipse.hawkbit.rest.resource.model.PagedList;
import org.eclipse.hawkbit.rest.resource.model.distributionset.DistributionSetRequestBodyPost;
import org.eclipse.hawkbit.rest.resource.model.distributionset.DistributionSetRequestBodyPut;
import org.eclipse.hawkbit.rest.resource.model.distributionset.DistributionSetRest;
import org.eclipse.hawkbit.rest.resource.model.distributionset.TargetAssignmentRequestBody;
import org.eclipse.hawkbit.rest.resource.model.distributionset.TargetAssignmentResponseBody;
import org.eclipse.hawkbit.rest.resource.model.softwaremodule.SoftwareModuleAssigmentRest;
import org.eclipse.hawkbit.rest.resource.model.softwaremodule.SoftwareModuleRest;
import org.eclipse.hawkbit.rest.resource.model.target.TargetRest;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Resource handling for {@link DistributionSet} CRUD operations.
 */
@RestController
public class DistributionSetResource implements DistributionSetRestApi {
    private static final Logger LOG = LoggerFactory.getLogger(DistributionSetResource.class);

    @Autowired
    private SoftwareManagement softwareManagement;

    @Autowired
    private TargetManagement targetManagement;

    @Autowired
    private DeploymentManagement deployManagament;

    @Autowired
    private SystemManagement systemManagement;

    @Autowired
    private TenantAware currentTenant;

    @Autowired
    private DistributionSetManagement distributionSetManagement;

    @Override
    public ResponseEntity<PagedList<DistributionSetRest>> getDistributionSets(final int pagingOffsetParam,
            final int pagingLimitParam, final String sortParam, final String rsqlParam) {

        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeDistributionSetSortParam(sortParam);

        final Pageable pageable = new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting);
        final Page<DistributionSet> findDsPage;
        if (rsqlParam != null) {
            findDsPage = this.distributionSetManagement.findDistributionSetsAll(
                    RSQLUtility.parse(rsqlParam, DistributionSetFields.class), pageable, false);
        } else {
            findDsPage = this.distributionSetManagement.findDistributionSetsAll(pageable, false, null);
        }

        final List<DistributionSetRest> rest = DistributionSetMapper.toResponseFromDsList(findDsPage.getContent());
        return new ResponseEntity<>(new PagedList<>(rest, findDsPage.getTotalElements()), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<DistributionSetRest> getDistributionSet(final Long distributionSetId) {
        final DistributionSet foundDs = findDistributionSetWithExceptionIfNotFound(distributionSetId);

        return new ResponseEntity<>(DistributionSetMapper.toResponse(foundDs), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<List<DistributionSetRest>> createDistributionSets(
            final List<DistributionSetRequestBodyPost> sets) {

        LOG.debug("creating {} distribution sets", sets.size());
        // set default Ds type if ds type is null
        sets.stream().filter(ds -> ds.getType() == null).forEach(ds -> ds.setType(this.systemManagement
                .getTenantMetadata(this.currentTenant.getCurrentTenant()).getDefaultDsType().getKey()));

        final Iterable<DistributionSet> createdDSets = this.distributionSetManagement.createDistributionSets(
                DistributionSetMapper.dsFromRequest(sets, this.softwareManagement, this.distributionSetManagement));

        LOG.debug("{} distribution sets created, return status {}", sets.size(), HttpStatus.CREATED);
        return new ResponseEntity<>(DistributionSetMapper.toResponseDistributionSets(createdDSets), HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<Void> deleteDistributionSet(final Long distributionSetId) {
        final DistributionSet set = findDistributionSetWithExceptionIfNotFound(distributionSetId);

        this.distributionSetManagement.deleteDistributionSet(set);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public ResponseEntity<DistributionSetRest> updateDistributionSet(final Long distributionSetId,
            final DistributionSetRequestBodyPut toUpdate) {
        final DistributionSet set = findDistributionSetWithExceptionIfNotFound(distributionSetId);

        if (toUpdate.getDescription() != null) {
            set.setDescription(toUpdate.getDescription());
        }

        if (toUpdate.getName() != null) {
            set.setName(toUpdate.getName());
        }

        if (toUpdate.getVersion() != null) {
            set.setVersion(toUpdate.getVersion());
        }
        return new ResponseEntity<>(
                DistributionSetMapper.toResponse(this.distributionSetManagement.updateDistributionSet(set)),
                HttpStatus.OK);
    }

    @Override
    public ResponseEntity<PagedList<TargetRest>> getAssignedTargets(final Long distributionSetId,
            final int pagingOffsetParam, final int pagingLimitParam, final String sortParam, final String rsqlParam) {

        // check if distribution set exists otherwise throw exception
        // immediately
        findDistributionSetWithExceptionIfNotFound(distributionSetId);

        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeTargetSortParam(sortParam);

        final Pageable pageable = new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting);
        final Page<Target> targetsAssignedDS;
        if (rsqlParam != null) {
            targetsAssignedDS = this.targetManagement.findTargetByAssignedDistributionSet(distributionSetId,
                    RSQLUtility.parse(rsqlParam, TargetFields.class), pageable);
        } else {
            targetsAssignedDS = this.targetManagement.findTargetByAssignedDistributionSet(distributionSetId, pageable);
        }

        return new ResponseEntity<>(new PagedList<>(TargetMapper.toResponse(targetsAssignedDS.getContent()),
                targetsAssignedDS.getTotalElements()), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<PagedList<TargetRest>> getInstalledTargets(final Long distributionSetId,
            final int pagingOffsetParam, final int pagingLimitParam, final String sortParam, final String rsqlParam) {
        // check if distribution set exists otherwise throw exception
        // immediately
        findDistributionSetWithExceptionIfNotFound(distributionSetId);

        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeTargetSortParam(sortParam);

        final Pageable pageable = new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting);
        final Page<Target> targetsInstalledDS;
        if (rsqlParam != null) {
            targetsInstalledDS = this.targetManagement.findTargetByInstalledDistributionSet(distributionSetId,
                    RSQLUtility.parse(rsqlParam, TargetFields.class), pageable);
        } else {
            targetsInstalledDS = this.targetManagement.findTargetByInstalledDistributionSet(distributionSetId,
                    pageable);
        }

        return new ResponseEntity<>(new PagedList<TargetRest>(TargetMapper.toResponse(targetsInstalledDS.getContent()),
                targetsInstalledDS.getTotalElements()), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<TargetAssignmentResponseBody> createAssignedTarget(final Long distributionSetId,
            final List<TargetAssignmentRequestBody> targetIds) {

        final DistributionSetAssignmentResult assignDistributionSet = this.deployManagament.assignDistributionSet(
                distributionSetId,
                targetIds.stream()
                        .map(t -> new TargetWithActionType(t.getId(),
                                RestResourceConversionHelper.convertActionType(t.getType()), t.getForcetime()))
                        .collect(Collectors.toList()));

        return new ResponseEntity<>(DistributionSetMapper.toResponse(assignDistributionSet), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<PagedList<MetadataRest>> getMetadata(final Long distributionSetId,
            final int pagingOffsetParam, final int pagingLimitParam, final String sortParam, final String rsqlParam) {

        // check if distribution set exists otherwise throw exception
        // immediately
        findDistributionSetWithExceptionIfNotFound(distributionSetId);

        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeDistributionSetMetadataSortParam(sortParam);

        final Pageable pageable = new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting);
        final Page<DistributionSetMetadata> metaDataPage;

        if (rsqlParam != null) {
            metaDataPage = this.distributionSetManagement.findDistributionSetMetadataByDistributionSetId(
                    distributionSetId, RSQLUtility.parse(rsqlParam, DistributionSetMetadataFields.class), pageable);
        } else {
            metaDataPage = this.distributionSetManagement
                    .findDistributionSetMetadataByDistributionSetId(distributionSetId, pageable);
        }

        return new ResponseEntity<>(
                new PagedList<>(DistributionSetMapper.toResponseDsMetadata(metaDataPage.getContent()),
                        metaDataPage.getTotalElements()),
                HttpStatus.OK);

    }

    @Override
    public ResponseEntity<MetadataRest> getMetadataValue(final Long distributionSetId, final String metadataKey) {
        // check if distribution set exists otherwise throw exception
        // immediately
        final DistributionSet ds = findDistributionSetWithExceptionIfNotFound(distributionSetId);
        final DistributionSetMetadata findOne = this.distributionSetManagement
                .findOne(new DsMetadataCompositeKey(ds, metadataKey));
        return ResponseEntity.<MetadataRest> ok(DistributionSetMapper.toResponseDsMetadata(findOne));
    }

    @Override
    public ResponseEntity<MetadataRest> updateMetadata(final Long distributionSetId, final String metadataKey,
            final MetadataRest metadata) {
        // check if distribution set exists otherwise throw exception
        // immediately
        final DistributionSet ds = findDistributionSetWithExceptionIfNotFound(distributionSetId);
        final DistributionSetMetadata updated = this.distributionSetManagement
                .updateDistributionSetMetadata(new DistributionSetMetadata(metadataKey, ds, metadata.getValue()));
        return ResponseEntity.ok(DistributionSetMapper.toResponseDsMetadata(updated));
    }

    @Override
    public ResponseEntity<Void> deleteMetadata(final Long distributionSetId, final String metadataKey) {
        // check if distribution set exists otherwise throw exception
        // immediately
        final DistributionSet ds = findDistributionSetWithExceptionIfNotFound(distributionSetId);
        this.distributionSetManagement.deleteDistributionSetMetadata(new DsMetadataCompositeKey(ds, metadataKey));
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<List<MetadataRest>> createMetadata(final Long distributionSetId,
            final List<MetadataRest> metadataRest) {
        // check if distribution set exists otherwise throw exception
        // immediately
        final DistributionSet ds = findDistributionSetWithExceptionIfNotFound(distributionSetId);

        final List<DistributionSetMetadata> created = this.distributionSetManagement
                .createDistributionSetMetadata(DistributionSetMapper.fromRequestDsMetadata(ds, metadataRest));
        return new ResponseEntity<>(DistributionSetMapper.toResponseDsMetadata(created), HttpStatus.CREATED);

    }

    @Override
    public ResponseEntity<Void> assignSoftwareModules(final Long distributionSetId,
            final List<SoftwareModuleAssigmentRest> softwareModuleIDs) {
        // check if distribution set exists otherwise throw exception
        // immediately
        final DistributionSet ds = findDistributionSetWithExceptionIfNotFound(distributionSetId);

        final Set<SoftwareModule> softwareModuleToBeAssigned = new HashSet<>();
        for (final SoftwareModuleAssigmentRest sm : softwareModuleIDs) {
            final SoftwareModule softwareModule = this.softwareManagement.findSoftwareModuleById(sm.getId());
            if (softwareModule != null) {
                softwareModuleToBeAssigned.add(softwareModule);
            } else {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        }
        // Add Softwaremodules to DisSet only if all of them were found
        this.distributionSetManagement.assignSoftwareModules(ds, softwareModuleToBeAssigned);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Void> deleteAssignSoftwareModules(final Long distributionSetId, final Long softwareModuleId) {
        // check if distribution set and software module exist otherwise throw
        // exception immediately
        final DistributionSet ds = findDistributionSetWithExceptionIfNotFound(distributionSetId);
        final SoftwareModule sm = findSoftwareModuleWithExceptionIfNotFound(softwareModuleId);
        this.distributionSetManagement.unassignSoftwareModule(ds, sm);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<PagedList<SoftwareModuleRest>> getAssignedSoftwareModules(final Long distributionSetId,
            final int pagingOffsetParam, final int pagingLimitParam, final String sortParam) {
        // check if distribution set exists otherwise throw exception
        // immediately
        final DistributionSet foundDs = findDistributionSetWithExceptionIfNotFound(distributionSetId);
        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeSoftwareModuleSortParam(sortParam);
        final Pageable pageable = new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting);
        final Page<SoftwareModule> softwaremodules = this.softwareManagement.findSoftwareModuleByAssignedTo(pageable,
                foundDs);
        return new ResponseEntity<>(new PagedList<>(SoftwareModuleMapper.toResponse(softwaremodules.getContent()),
                softwaremodules.getTotalElements()), HttpStatus.OK);
    }

    private DistributionSet findDistributionSetWithExceptionIfNotFound(final Long distributionSetId) {
        final DistributionSet set = this.distributionSetManagement.findDistributionSetById(distributionSetId);
        if (set == null) {
            throw new EntityNotFoundException("DistributionSet with Id {" + distributionSetId + "} does not exist");
        }

        return set;
    }

    private SoftwareModule findSoftwareModuleWithExceptionIfNotFound(final Long softwareModuleId) {
        final SoftwareModule sm = this.softwareManagement.findSoftwareModuleById(softwareModuleId);
        if (sm == null) {
            throw new EntityNotFoundException("SoftwareModule with Id {" + softwareModuleId + "} does not exist");
        }
        return sm;
    }
}
