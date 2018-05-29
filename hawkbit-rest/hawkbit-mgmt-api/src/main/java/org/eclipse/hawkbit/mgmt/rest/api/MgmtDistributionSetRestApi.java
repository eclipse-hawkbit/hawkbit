/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.rest.api;

import java.util.List;

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
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * REST Resource handling for DistributionSet CRUD operations.
 */
@RequestMapping(MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING)
public interface MgmtDistributionSetRestApi {

    /**
     * Handles the GET request of retrieving all DistributionSets .
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
    @RequestMapping(method = RequestMethod.GET, produces = { MediaTypes.HAL_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<PagedList<MgmtDistributionSet>> getDistributionSets(
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) int pagingOffsetParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) int pagingLimitParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false) String sortParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false) String rsqlParam);

    /**
     * Handles the GET request of retrieving a single DistributionSet .
     *
     * @param distributionSetId
     *            the ID of the set to retrieve
     *
     * @return a single DistributionSet with status OK.
     *
     */
    @RequestMapping(method = RequestMethod.GET, value = "/{distributionSetId}", produces = { MediaTypes.HAL_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtDistributionSet> getDistributionSet(@PathVariable("distributionSetId") Long distributionSetId);

    /**
     * Handles the POST request of creating new distribution sets . The request
     * body must always be a list of sets.
     *
     * @param sets
     *            the DistributionSets to be created.
     * @return In case all sets could successful created the ResponseEntity with
     *         status code 201 - Created but without ResponseBody. In any
     *         failure the JsonResponseExceptionHandler is handling the
     *         response.
     */
    @RequestMapping(method = RequestMethod.POST, consumes = { MediaTypes.HAL_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE }, produces = { MediaTypes.HAL_JSON_VALUE,
                    MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<List<MgmtDistributionSet>> createDistributionSets(List<MgmtDistributionSetRequestBodyPost> sets);

    /**
     * Handles the DELETE request for a single DistributionSet .
     *
     * @param distributionSetId
     *            the ID of the DistributionSet to delete
     * @return status OK if delete as successful.
     *
     */
    @RequestMapping(method = RequestMethod.DELETE, value = "/{distributionSetId}")
    ResponseEntity<Void> deleteDistributionSet(@PathVariable("distributionSetId") Long distributionSetId);

    /**
     * Handles the UPDATE request for a single DistributionSet .
     *
     * @param distributionSetId
     *            the ID of the DistributionSet to delete
     * @param toUpdate
     *            with the data that needs updating
     *
     * @return status OK if update as successful with updated content.
     *
     */
    @RequestMapping(method = RequestMethod.PUT, value = "/{distributionSetId}", consumes = { MediaTypes.HAL_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE }, produces = { MediaType.APPLICATION_JSON_VALUE,
                    MediaTypes.HAL_JSON_VALUE })
    ResponseEntity<MgmtDistributionSet> updateDistributionSet(@PathVariable("distributionSetId") Long distributionSetId,
            MgmtDistributionSetRequestBodyPut toUpdate);

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
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<PagedList<MgmtTarget>> getAssignedTargets(@PathVariable("distributionSetId") Long distributionSetId,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) int pagingOffsetParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) int pagingLimitParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false) String sortParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false) String rsqlParam);

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
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<PagedList<MgmtTarget>> getInstalledTargets(@PathVariable("distributionSetId") Long distributionSetId,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) int pagingOffsetParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) int pagingLimitParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false) String sortParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false) String rsqlParam);

    /**
     * Handles the GET request to retrieve target filter queries that have the
     * given distribution set as auto assign DS.
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
     *            the search name parameter in the request URL, syntax
     *            {@code q=myFilter}
     * @return status OK if get request is successful with the paged list of
     *         targets
     */
    @RequestMapping(method = RequestMethod.GET, value = "/{distributionSetId}/autoAssignTargetFilters", produces = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<PagedList<MgmtTargetFilterQuery>> getAutoAssignTargetFilterQueries(
            @PathVariable("distributionSetId") Long distributionSetId,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) int pagingOffsetParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) int pagingLimitParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false) String sortParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false) String rsqlParam);

    /**
     * Handles the POST request of assigning multiple targets to a single
     * distribution set.
     *
     * @param distributionSetId
     *            the ID of the distribution set within the URL path parameter
     * @param targetIds
     *            the IDs of the target which should get assigned to the
     *            distribution set given in the response body
     * @param offline
     *            to <code>true</code> if update was executed offline, i.e. not
     *            managed by hawkBit.
     * @return status OK if the assignment of the targets was successful and a
     *         complex return body which contains information about the assigned
     *         targets and the already assigned targets counters
     */
    @RequestMapping(method = RequestMethod.POST, value = "/{distributionSetId}/assignedTargets", consumes = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE }, produces = { MediaTypes.HAL_JSON_VALUE,
                    MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtTargetAssignmentResponseBody> createAssignedTarget(
            @PathVariable("distributionSetId") Long distributionSetId,
            final List<MgmtTargetAssignmentRequestBody> targetIds,
            @RequestParam(value = "offline", required = false) boolean offline);

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
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<PagedList<MgmtMetadata>> getMetadata(
            @PathVariable("distributionSetId") Long distributionSetId,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) int pagingOffsetParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) int pagingLimitParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false) String sortParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false) String rsqlParam);

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
    ResponseEntity<MgmtMetadata> getMetadataValue(@PathVariable("distributionSetId") Long distributionSetId,
            @PathVariable("metadataKey") String metadataKey);

    /**
     * Updates a single meta data value of a distribution set.
     *
     * @param distributionSetId
     *            the ID of the distribution set to update the meta data entry
     * @param metadataKey
     *            the key of the meta data to update the value
     * @param metadata
     *            update body
     * @return status OK if the update request is successful and the updated
     *         meta data result
     */
    @RequestMapping(method = RequestMethod.PUT, value = "/{distributionSetId}/metadata/{metadataKey}", produces = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtMetadata> updateMetadata(@PathVariable("distributionSetId") Long distributionSetId,
            @PathVariable("metadataKey") String metadataKey, MgmtMetadataBodyPut metadata);

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
    ResponseEntity<Void> deleteMetadata(@PathVariable("distributionSetId") Long distributionSetId,
            @PathVariable("metadataKey") String metadataKey);

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
            MediaTypes.HAL_JSON_VALUE }, produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<List<MgmtMetadata>> createMetadata(@PathVariable("distributionSetId") Long distributionSetId,
            List<MgmtMetadata> metadataRest);

    /**
     * Assigns a list of software modules to a distribution set.
     *
     * @param distributionSetId
     *            the ID of the distribution set to assign software modules for
     * @param softwareModuleIDs
     *            the list of software modules ids to assign
     * @return http status
     *
     */
    @RequestMapping(method = RequestMethod.POST, value = "/{distributionSetId}/assignedSM", consumes = {
            MediaType.APPLICATION_JSON_VALUE,
            MediaTypes.HAL_JSON_VALUE }, produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<Void> assignSoftwareModules(@PathVariable("distributionSetId") Long distributionSetId,
            List<MgmtSoftwareModuleAssigment> softwareModuleIDs);

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
     */
    @RequestMapping(method = RequestMethod.DELETE, value = "/{distributionSetId}/assignedSM/{softwareModuleId}")
    ResponseEntity<Void> deleteAssignSoftwareModules(@PathVariable("distributionSetId") Long distributionSetId,
            @PathVariable("softwareModuleId") Long softwareModuleId);

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
     */
    @RequestMapping(method = RequestMethod.GET, value = "/{distributionSetId}/assignedSM", produces = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<PagedList<MgmtSoftwareModule>> getAssignedSoftwareModules(
            @PathVariable("distributionSetId") Long distributionSetId,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) int pagingOffsetParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) int pagingLimitParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false) String sortParam);
}
