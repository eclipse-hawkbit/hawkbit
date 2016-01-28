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
import org.eclipse.hawkbit.repository.SoftwareManagement;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.TargetFields;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.TargetWithActionType;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetMetadata;
import org.eclipse.hawkbit.repository.model.DsMetadataCompositeKey;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.rsql.RSQLUtility;
import org.eclipse.hawkbit.rest.resource.helper.RestResourceConversionHelper;
import org.eclipse.hawkbit.rest.resource.model.MetadataRest;
import org.eclipse.hawkbit.rest.resource.model.MetadataRestPageList;
import org.eclipse.hawkbit.rest.resource.model.distributionset.DistributionSetPagedList;
import org.eclipse.hawkbit.rest.resource.model.distributionset.DistributionSetRequestBodyPost;
import org.eclipse.hawkbit.rest.resource.model.distributionset.DistributionSetRequestBodyPut;
import org.eclipse.hawkbit.rest.resource.model.distributionset.DistributionSetRest;
import org.eclipse.hawkbit.rest.resource.model.distributionset.DistributionSetsRest;
import org.eclipse.hawkbit.rest.resource.model.distributionset.TargetAssignmentRequestBody;
import org.eclipse.hawkbit.rest.resource.model.distributionset.TargetAssignmentResponseBody;
import org.eclipse.hawkbit.rest.resource.model.softwaremodule.SoftwareModuleAssigmentRest;
import org.eclipse.hawkbit.rest.resource.model.softwaremodule.SoftwareModulePagedList;
import org.eclipse.hawkbit.rest.resource.model.target.TargetPagedList;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Resource handling for {@link DistributionSet} CRUD operations.
 *
 *
 *
 *
 */
@RestController
@RequestMapping(RestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING)
public class DistributionSetResource {
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

    /**
     * Handles the GET request of retrieving all {@link DistributionSet}s within
     * SP.
     *
     * @param pagingOffsetParam
     *            the offset of list of sets for pagination, might not be
     *            present in the rest request then default value will be applied
     * @param pagingLimitParam
     *            the limit of the paged request, might not be present in the
     *            rest request then default value will be applied
     * @param sortParam
     *            the sorting parameter in the request URL, syntax
     *            {@code field:direction, field:direction}
     * @param rsqlParam
     *            the search parameter in the request URL, syntax
     *            {@code q=name==abc}
     * @return a list of all set for a defined or default page request with
     *         status OK. The response is always paged. In any failure the
     *         JsonResponseExceptionHandler is handling the response.
     */
    @RequestMapping(method = RequestMethod.GET, produces = { "application/hal+json", MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<DistributionSetPagedList> getDistributionSets(
            @RequestParam(value = RestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = RestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) final int pagingOffsetParam,
            @RequestParam(value = RestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = RestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) final int pagingLimitParam,
            @RequestParam(value = RestConstants.REQUEST_PARAMETER_SORTING, required = false) final String sortParam,
            @RequestParam(value = RestConstants.REQUEST_PARAMETER_SEARCH, required = false) final String rsqlParam) {

        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeDistributionSetSortParam(sortParam);

        final Pageable pageable = new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting);
        final Page<DistributionSet> findDsPage;
        if (rsqlParam != null) {
            findDsPage = distributionSetManagement.findDistributionSetsAll(
                    RSQLUtility.parse(rsqlParam, DistributionSetFields.class), pageable, false);
        } else {
            findDsPage = distributionSetManagement.findDistributionSetsAll(pageable, false, null);
        }

        final List<DistributionSetRest> rest = DistributionSetMapper.toResponseFromDsList(findDsPage.getContent());
        return new ResponseEntity<>(new DistributionSetPagedList(rest, findDsPage.getTotalElements()), HttpStatus.OK);
    }

    /**
     * Handles the GET request of retrieving a single {@link DistributionSet}
     * within SP.
     *
     * @param distributionSetId
     *            the ID of the set to retrieve
     *
     * @return a single {@link DistributionSet} with status OK.
     *
     * @throws EntityNotFoundException
     *             in case no {@link DistributionSet} with the given ID exists.
     */
    @RequestMapping(method = RequestMethod.GET, value = "/{distributionSetId}", produces = { "application/hal+json",
            MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<DistributionSetRest> getDistributionSet(@PathVariable final Long distributionSetId) {
        final DistributionSet foundDs = findDistributionSetWithExceptionIfNotFound(distributionSetId);

        return new ResponseEntity<>(DistributionSetMapper.toResponse(foundDs), HttpStatus.OK);
    }

    /**
     * Handles the POST request of creating new distribution sets within SP. The
     * request body must always be a list of sets. The requests is delegating to
     * the {@link SoftwareManagement#createDistributionSet(DistributionSet))}.
     *
     * @param sets
     *            the {@link DistributionSet}s to be created.
     * @return In case all sets could successful created the ResponseEntity with
     *         status code 201 - Created but without ResponseBody. In any
     *         failure the JsonResponseExceptionHandler is handling the
     *         response.
     */
    @RequestMapping(method = RequestMethod.POST, consumes = { MediaType.APPLICATION_JSON_VALUE,
            "application/hal+json" }, produces = { "application/hal+json", MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<DistributionSetsRest> createDistributionSets(
            @RequestBody final List<DistributionSetRequestBodyPost> sets) {

        LOG.debug("creating {} distribution sets", sets.size());
        // set default Ds type if ds type is null
        sets.stream().filter(ds -> ds.getType() == null).forEach(ds -> ds.setType(
                systemManagement.getTenantMetadata(currentTenant.getCurrentTenant()).getDefaultDsType().getKey()));

        final Iterable<DistributionSet> createdDSets = distributionSetManagement.createDistributionSets(
                DistributionSetMapper.dsFromRequest(sets, softwareManagement, distributionSetManagement));

        LOG.debug("{} distribution sets created, return status {}", sets.size(), HttpStatus.CREATED);
        return new ResponseEntity<>(DistributionSetMapper.toResponseDistributionSets(createdDSets), HttpStatus.CREATED);
    }

    /**
     * Handles the DELETE request for a single {@link DistributionSet} within
     * SP.
     *
     * @param distributionSetId
     *            the ID of the {@link DistributionSet} to delete
     * @return status OK if delete as successful.
     *
     */
    @RequestMapping(method = RequestMethod.DELETE, value = "/{distributionSetId}")
    public ResponseEntity<Void> deleteDistributionSet(@PathVariable final Long distributionSetId) {
        final DistributionSet set = findDistributionSetWithExceptionIfNotFound(distributionSetId);

        distributionSetManagement.deleteDistributionSet(set);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Handles the UPDATE request for a single {@link DistributionSet} within
     * SP.
     *
     * @param distributionSetId
     *            the ID of the {@link DistributionSet} to delete
     * @param toUpdate
     *            with the data that needs updating
     *
     * @return status OK if update as successful with updated content.
     *
     */
    @RequestMapping(method = RequestMethod.PUT, value = "/{distributionSetId}", consumes = { "application/hal+json",
            MediaType.APPLICATION_JSON_VALUE }, produces = { MediaType.APPLICATION_JSON_VALUE, "application/hal+json" })
    public ResponseEntity<DistributionSetRest> updateDistributionSet(@PathVariable final Long distributionSetId,
            @RequestBody final DistributionSetRequestBodyPut toUpdate) {
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
                DistributionSetMapper.toResponse(distributionSetManagement.updateDistributionSet(set)), HttpStatus.OK);
    }

    /**
     * Handles the GET request of retrieving assigned targets to a specific
     * distribution set.
     *
     * @param distributionSetId
     *            the ID of the distribution set to retrieve the assigned
     *            targets
     * @param pagingOffsetParam
     *            the offset of list of targets for pagination, might not be
     *            present in the rest request then default value will be applied
     * @param pagingLimitParam
     *            the limit of the paged request, might not be present in the
     *            rest request then default value will be applied
     * @param sortParam
     *            the sorting parameter in the request URL, syntax
     *            {@code field:direction, field:direction}
     * @param rsqlParam
     *            the search parameter in the request URL, syntax
     *            {@code q=name==abc}
     * @return status OK if get request is successful with the paged list of
     *         targets
     */
    @RequestMapping(method = RequestMethod.GET, value = "/{distributionSetId}/assignedTargets", produces = {
            MediaType.APPLICATION_JSON_VALUE, "application/hal+json" })
    public ResponseEntity<TargetPagedList> getAssignedTargets(@PathVariable final Long distributionSetId,
            @RequestParam(value = RestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = RestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) final int pagingOffsetParam,
            @RequestParam(value = RestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = RestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) final int pagingLimitParam,
            @RequestParam(value = RestConstants.REQUEST_PARAMETER_SORTING, required = false) final String sortParam,
            @RequestParam(value = RestConstants.REQUEST_PARAMETER_SEARCH, required = false) final String rsqlParam) {

        // check if distribution set exists otherwise throw exception
        // immediately
        findDistributionSetWithExceptionIfNotFound(distributionSetId);

        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeTargetSortParam(sortParam);

        final Pageable pageable = new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting);
        final Page<Target> targetsAssignedDS;
        if (rsqlParam != null) {
            targetsAssignedDS = targetManagement.findTargetByAssignedDistributionSet(distributionSetId,
                    RSQLUtility.parse(rsqlParam, TargetFields.class), pageable);
        } else {
            targetsAssignedDS = targetManagement.findTargetByAssignedDistributionSet(distributionSetId, pageable);
        }

        return new ResponseEntity<>(new TargetPagedList(TargetMapper.toResponse(targetsAssignedDS.getContent()),
                targetsAssignedDS.getTotalElements()), HttpStatus.OK);
    }

    /**
     * Handles the GET request of retrieving installed targets to a specific
     * distribution set.
     *
     * @param distributionSetId
     *            the ID of the distribution set to retrieve the assigned
     *            targets
     * @param pagingOffsetParam
     *            the offset of list of targets for pagination, might not be
     *            present in the rest request then default value will be applied
     * @param pagingLimitParam
     *            the limit of the paged request, might not be present in the
     *            rest request then default value will be applied
     * @param sortParam
     *            the sorting parameter in the request URL, syntax
     *            {@code field:direction, field:direction}
     * @param rsqlParam
     *            the search parameter in the request URL, syntax
     *            {@code q=name==abc}
     * @return status OK if get request is successful with the paged list of
     *         targets
     */
    @RequestMapping(method = RequestMethod.GET, value = "/{distributionSetId}/installedTargets", produces = {
            MediaType.APPLICATION_JSON_VALUE, "application/hal+json" })
    public ResponseEntity<TargetPagedList> getInstalledTargets(@PathVariable final Long distributionSetId,
            @RequestParam(value = RestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = RestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) final int pagingOffsetParam,
            @RequestParam(value = RestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = RestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) final int pagingLimitParam,
            @RequestParam(value = RestConstants.REQUEST_PARAMETER_SORTING, required = false) final String sortParam,
            @RequestParam(value = RestConstants.REQUEST_PARAMETER_SEARCH, required = false) final String rsqlParam) {
        // check if distribution set exists otherwise throw exception
        // immediately
        findDistributionSetWithExceptionIfNotFound(distributionSetId);

        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeTargetSortParam(sortParam);

        final Pageable pageable = new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting);
        final Page<Target> targetsInstalledDS;
        if (rsqlParam != null) {
            targetsInstalledDS = targetManagement.findTargetByInstalledDistributionSet(distributionSetId,
                    RSQLUtility.parse(rsqlParam, TargetFields.class), pageable);
        } else {
            targetsInstalledDS = targetManagement.findTargetByInstalledDistributionSet(distributionSetId, pageable);
        }

        return new ResponseEntity<>(new TargetPagedList(TargetMapper.toResponse(targetsInstalledDS.getContent()),
                targetsInstalledDS.getTotalElements()), HttpStatus.OK);
    }

    /**
     * Handles the POST request of assigning multiple targets to a single
     * distribution set.
     *
     * @param distributionSetId
     *            the ID of the distribution set within the URL path parameter
     * @param targetIds
     *            the IDs of the target which should get assigned to the
     *            distribution set given in the response body
     * @return status OK if the assignment of the targets was successful and a
     *         complex return body which contains information about the assigned
     *         targets and the already assigned targets counters
     */
    @RequestMapping(method = RequestMethod.POST, value = "/{distributionSetId}/assignedTargets", consumes = {
            "application/hal+json",
            MediaType.APPLICATION_JSON_VALUE }, produces = { MediaType.APPLICATION_JSON_VALUE, "application/hal+json" })
    public ResponseEntity<TargetAssignmentResponseBody> createAssignedTarget(@PathVariable final Long distributionSetId,
            @RequestBody final List<TargetAssignmentRequestBody> targetIds) {

        final DistributionSetAssignmentResult assignDistributionSet = deployManagament.assignDistributionSet(
                distributionSetId,
                targetIds.stream()
                        .map(t -> new TargetWithActionType(t.getId(),
                                RestResourceConversionHelper.convertActionType(t.getType()), t.getForcetime()))
                        .collect(Collectors.toList()));

        return new ResponseEntity<>(DistributionSetMapper.toResponse(assignDistributionSet), HttpStatus.OK);
    }

    /**
     * Gets a paged list of meta data for a distribution set.
     *
     * @param distributionSetId
     *            the ID of the distribution set for the meta data
     * @param pagingOffsetParam
     *            the offset of list of targets for pagination, might not be
     *            present in the rest request then default value will be applied
     * @param pagingLimitParam
     *            the limit of the paged request, might not be present in the
     *            rest request then default value will be applied
     * @param sortParam
     *            the sorting parameter in the request URL, syntax
     *            {@code field:direction, field:direction}
     * @param rsqlParam
     *            the search parameter in the request URL, syntax
     *            {@code q=key==abc}
     * @return status OK if get request is successful with the paged list of
     *         meta data
     */
    @RequestMapping(method = RequestMethod.GET, value = "/{distributionSetId}/metadata", produces = {
            MediaType.APPLICATION_JSON_VALUE, "application/hal+json" })
    public ResponseEntity<MetadataRestPageList> getMetadata(@PathVariable final Long distributionSetId,
            @RequestParam(value = RestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = RestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) final int pagingOffsetParam,
            @RequestParam(value = RestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = RestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) final int pagingLimitParam,
            @RequestParam(value = RestConstants.REQUEST_PARAMETER_SORTING, required = false) final String sortParam,
            @RequestParam(value = RestConstants.REQUEST_PARAMETER_SEARCH, required = false) final String rsqlParam) {

        // check if distribution set exists otherwise throw exception
        // immediately
        findDistributionSetWithExceptionIfNotFound(distributionSetId);

        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeDistributionSetMetadataSortParam(sortParam);

        final Pageable pageable = new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting);
        final Page<DistributionSetMetadata> metaDataPage;

        if (rsqlParam != null) {
            metaDataPage = distributionSetManagement.findDistributionSetMetadataByDistributionSetId(distributionSetId,
                    RSQLUtility.parse(rsqlParam, DistributionSetMetadataFields.class), pageable);
        } else {
            metaDataPage = distributionSetManagement.findDistributionSetMetadataByDistributionSetId(distributionSetId,
                    pageable);
        }

        return new ResponseEntity<>(
                new MetadataRestPageList(DistributionSetMapper.toResponseDsMetadata(metaDataPage.getContent()),
                        metaDataPage.getTotalElements()),
                HttpStatus.OK);

    }

    /**
     * Gets a single meta data value for a specific key of a distribution set.
     *
     * @param distributionSetId
     *            the ID of the distribution set to get the meta data from
     * @param metadataKey
     *            the key of the meta data entry to retrieve the value from
     * @return status OK if get request is successful with the value of the meta
     *         data
     */
    @RequestMapping(method = RequestMethod.GET, value = "/{distributionSetId}/metadata/{metadataKey}", produces = {
            MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<MetadataRest> getMetadataValue(@PathVariable final Long distributionSetId,
            @PathVariable final String metadataKey) {
        // check if distribution set exists otherwise throw exception
        // immediately
        final DistributionSet ds = findDistributionSetWithExceptionIfNotFound(distributionSetId);
        final DistributionSetMetadata findOne = distributionSetManagement
                .findOne(new DsMetadataCompositeKey(ds, metadataKey));
        return ResponseEntity.<MetadataRest> ok(DistributionSetMapper.toResponseDsMetadata(findOne));
    }

    /**
     * Updates a single meta data value of a distribution set.
     *
     * @param distributionSetId
     *            the ID of the distribution set to update the meta data entry
     * @param metadataKey
     *            the key of the meta data to update the value
     * @return status OK if the update request is successful and the updated
     *         meta data result
     */
    @RequestMapping(method = RequestMethod.PUT, value = "/{distributionSetId}/metadata/{metadataKey}", produces = {
            MediaType.APPLICATION_JSON_VALUE, "application/hal+json" })
    public ResponseEntity<MetadataRest> updateMetadata(@PathVariable final Long distributionSetId,
            @PathVariable final String metadataKey, @RequestBody final MetadataRest metadata) {
        // check if distribution set exists otherwise throw exception
        // immediately
        final DistributionSet ds = findDistributionSetWithExceptionIfNotFound(distributionSetId);
        final DistributionSetMetadata updated = distributionSetManagement
                .updateDistributionSetMetadata(new DistributionSetMetadata(metadataKey, ds, metadata.getValue()));
        return ResponseEntity.ok(DistributionSetMapper.toResponseDsMetadata(updated));
    }

    /**
     * Deletes a single meta data entry from the distribution set.
     *
     * @param distributionSetId
     *            the ID of the distribution set to delete the meta data entry
     * @param metadataKey
     *            the key of the meta data to delete
     * @return status OK if the delete request is successful
     */
    @RequestMapping(method = RequestMethod.DELETE, value = "/{distributionSetId}/metadata/{metadataKey}")
    public ResponseEntity<Void> deleteMetadata(@PathVariable final Long distributionSetId,
            @PathVariable final String metadataKey) {
        // check if distribution set exists otherwise throw exception
        // immediately
        final DistributionSet ds = findDistributionSetWithExceptionIfNotFound(distributionSetId);
        distributionSetManagement.deleteDistributionSetMetadata(new DsMetadataCompositeKey(ds, metadataKey));
        return ResponseEntity.ok().build();
    }

    /**
     * Creates a list of meta data for a specific distribution set.
     *
     * @param distributionSetId
     *            the ID of the distribution set to create meta data for
     * @param metadataRest
     *            the list of meta data entries to create
     * @return status created if post request is successful with the value of
     *         the created meta data
     */
    @RequestMapping(method = RequestMethod.POST, value = "/{distributionSetId}/metadata", consumes = {
            MediaType.APPLICATION_JSON_VALUE,
            "application/hal+json" }, produces = { MediaType.APPLICATION_JSON_VALUE, "application/hal+json" })
    public ResponseEntity<List<MetadataRest>> createMetadata(@PathVariable final Long distributionSetId,
            @RequestBody final List<MetadataRest> metadataRest) {
        // check if distribution set exists otherwise throw exception
        // immediately
        final DistributionSet ds = findDistributionSetWithExceptionIfNotFound(distributionSetId);

        final List<DistributionSetMetadata> created = distributionSetManagement
                .createDistributionSetMetadata(DistributionSetMapper.fromRequestDsMetadata(ds, metadataRest));
        return new ResponseEntity<>(DistributionSetMapper.toResponseDsMetadata(created), HttpStatus.CREATED);

    }

    /**
     * Assigns a list of software modules to a distribution set.
     *
     * @param distributionSetId
     *            the ID of the distribution set to assign software modules for
     * @param softwareModuleIDs
     *            the list of software modules ids to assign
     * @return {@link HttpStatus#OK}
     *
     * @throws EntityNotFoundException
     *             in case no distribution set with the given
     *             {@code distributionSetId} exists.
     */
    @RequestMapping(method = RequestMethod.POST, value = "/{distributionSetId}/assignedSM", consumes = {
            MediaType.APPLICATION_JSON_VALUE,
            "application/hal+json" }, produces = { "application/hal+json", MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<Void> assignSoftwareModules(@PathVariable final Long distributionSetId,
            @RequestBody final List<SoftwareModuleAssigmentRest> softwareModuleIDs) {
        // check if distribution set exists otherwise throw exception
        // immediately
        final DistributionSet ds = findDistributionSetWithExceptionIfNotFound(distributionSetId);

        final Set<SoftwareModule> softwareModuleToBeAssigned = new HashSet<>();
        for (final SoftwareModuleAssigmentRest sm : softwareModuleIDs) {
            final SoftwareModule softwareModule = softwareManagement.findSoftwareModuleById(sm.getId());
            if (softwareModule != null) {
                softwareModuleToBeAssigned.add(softwareModule);
            } else {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        }
        // Add Softwaremodules to DisSet only if all of them were found
        distributionSetManagement.assignSoftwareModules(ds, softwareModuleToBeAssigned);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Deletes the assignment of the software module form the distribution set.
     *
     * @param distributionSetId
     *            the ID of the distribution set to reject the software module
     *            for
     * @param softwareModuleId
     *            the software module id to get rejected form the distribution
     *            set
     * @return status OK if rejection was successful.
     * @throws EntityNotFoundException
     *             in case no distribution set with the given
     *             {@code distributionSetId} exists.
     */
    @RequestMapping(method = RequestMethod.DELETE, value = "/{distributionSetId}/assignedSM/{softwareModuleId}")
    public ResponseEntity<Void> deleteAssignSoftwareModules(@PathVariable final Long distributionSetId,
            @PathVariable final Long softwareModuleId) {
        // check if distribution set and software module exist otherwise throw
        // exception immediately
        final DistributionSet ds = findDistributionSetWithExceptionIfNotFound(distributionSetId);
        final SoftwareModule sm = findSoftwareModuleWithExceptionIfNotFound(softwareModuleId);
        distributionSetManagement.unassignSoftwareModule(ds, sm);
        return ResponseEntity.ok().build();
    }

    /**
     * Handles the GET request for retrieving the assigned software modules of a
     * specific distribution set.
     *
     * @param distributionSetId
     *            the ID of the distribution to retrieve
     * @param pagingOffsetParam
     *            the offset of list of sets for pagination, might not be
     *            present in the rest request then default value will be applied
     * @param pagingLimitParam
     *            the limit of the paged request, might not be present in the
     *            rest request then default value will be applied
     * @param sortParam
     *            the sorting parameter in the request URL, syntax
     *            {@code field:direction, field:direction}
     * @return a list of the assigned software modules of a distribution set
     *         with status OK, if none is assigned than {@code null}
     * @throws EntityNotFoundException
     *             in case no distribution set with the given
     *             {@code distributionSetId} exists.
     */
    @RequestMapping(method = RequestMethod.GET, value = "/{distributionSetId}/assignedSM", produces = {
            "application/hal+json", MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<SoftwareModulePagedList> getAssignedSoftwareModules(
            @PathVariable final Long distributionSetId,
            @RequestParam(value = RestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = RestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) final int pagingOffsetParam,
            @RequestParam(value = RestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = RestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) final int pagingLimitParam,
            @RequestParam(value = RestConstants.REQUEST_PARAMETER_SORTING, required = false) final String sortParam) {
        // check if distribution set exists otherwise throw exception
        // immediately
        final DistributionSet foundDs = findDistributionSetWithExceptionIfNotFound(distributionSetId);
        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeSoftwareModuleSortParam(sortParam);
        final Pageable pageable = new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting);
        final Page<SoftwareModule> softwaremodules = softwareManagement.findSoftwareModuleByAssignedTo(pageable,
                foundDs);
        return new ResponseEntity<>(
                new SoftwareModulePagedList(SoftwareModuleMapper.toResponse(softwaremodules.getContent()),
                        softwaremodules.getTotalElements()),
                HttpStatus.OK);
    }

    private DistributionSet findDistributionSetWithExceptionIfNotFound(final Long distributionSetId) {
        final DistributionSet set = distributionSetManagement.findDistributionSetById(distributionSetId);
        if (set == null) {
            throw new EntityNotFoundException("DistributionSet with Id {" + distributionSetId + "} does not exist");
        }

        return set;
    }

    private SoftwareModule findSoftwareModuleWithExceptionIfNotFound(final Long softwareModuleId) {
        final SoftwareModule sm = softwareManagement.findSoftwareModuleById(softwareModuleId);
        if (sm == null) {
            throw new EntityNotFoundException("SoftwareModule with Id {" + softwareModuleId + "} does not exist");
        }
        return sm;
    }
}
