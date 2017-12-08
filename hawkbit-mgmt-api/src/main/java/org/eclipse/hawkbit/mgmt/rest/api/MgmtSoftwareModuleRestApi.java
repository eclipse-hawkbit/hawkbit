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

import org.eclipse.hawkbit.mgmt.json.model.PagedList;
import org.eclipse.hawkbit.mgmt.json.model.artifact.MgmtArtifact;
import org.eclipse.hawkbit.mgmt.json.model.softwaremodule.MgmtSoftwareModule;
import org.eclipse.hawkbit.mgmt.json.model.softwaremodule.MgmtSoftwareModuleMetadata;
import org.eclipse.hawkbit.mgmt.json.model.softwaremodule.MgmtSoftwareModuleMetadataBodyPut;
import org.eclipse.hawkbit.mgmt.json.model.softwaremodule.MgmtSoftwareModuleRequestBodyPost;
import org.eclipse.hawkbit.mgmt.json.model.softwaremodule.MgmtSoftwareModuleRequestBodyPut;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST Resource handling for SoftwareModule and related Artifact CRUD
 * operations.
 *
 */
@RequestMapping(MgmtRestConstants.SOFTWAREMODULE_V1_REQUEST_MAPPING)
public interface MgmtSoftwareModuleRestApi {

    /**
     * Handles POST request for artifact upload.
     *
     * @param softwareModuleId
     *            of the parent SoftwareModule
     * @param file
     *            that has to be uploaded
     * @param optionalFileName
     *            to override {@link MultipartFile#getOriginalFilename()}
     * @param md5Sum
     *            checksum for uploaded content check
     * @param sha1Sum
     *            checksum for uploaded content check
     * 
     * @return In case all sets could successful be created the ResponseEntity
     *         with status code 201 - Created but without ResponseBody. In any
     *         failure the JsonResponseExceptionHandler is handling the
     *         response.
     */
    @RequestMapping(method = RequestMethod.POST, value = "/{softwareModuleId}/artifacts", produces = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtArtifact> uploadArtifact(@PathVariable("softwareModuleId") final Long softwareModuleId,
            @RequestParam("file") final MultipartFile file,
            @RequestParam(value = "filename", required = false) final String optionalFileName,
            @RequestParam(value = "md5sum", required = false) final String md5Sum,
            @RequestParam(value = "sha1sum", required = false) final String sha1Sum);

    /**
     * Handles the GET request of retrieving all meta data of artifacts assigned
     * to a software module.
     *
     * @param softwareModuleId
     *            of the parent SoftwareModule
     *
     * @return a list of all artifacts for a defined or default page request
     *         with status OK. The response is always paged. In any failure the
     *         JsonResponseExceptionHandler is handling the response.
     */
    @RequestMapping(method = RequestMethod.GET, value = "/{softwareModuleId}/artifacts", produces = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<List<MgmtArtifact>> getArtifacts(@PathVariable("softwareModuleId") final Long softwareModuleId);

    /**
     * Handles the GET request of retrieving a single Artifact meta data
     * request.
     *
     * @param softwareModuleId
     *            of the parent SoftwareModule
     * @param artifactId
     *            of the related LocalArtifact
     *
     * @return responseEntity with status ok if successful
     */
    @RequestMapping(method = RequestMethod.GET, value = "/{softwareModuleId}/artifacts/{artifactId}", produces = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    @ResponseBody
    ResponseEntity<MgmtArtifact> getArtifact(@PathVariable("softwareModuleId") final Long softwareModuleId,
            @PathVariable("artifactId") final Long artifactId);

    /**
     * Handles the DELETE request for a single SoftwareModule.
     *
     * @param softwareModuleId
     *            the ID of the module that has the artifact
     * @param artifactId
     *            of the artifact to be deleted
     *
     * @return status OK if delete as successful.
     */
    @RequestMapping(method = RequestMethod.DELETE, value = "/{softwareModuleId}/artifacts/{artifactId}")
    @ResponseBody
    ResponseEntity<Void> deleteArtifact(@PathVariable("softwareModuleId") final Long softwareModuleId,
            @PathVariable("artifactId") final Long artifactId);

    /**
     * Handles the GET request of retrieving all softwaremodules.
     *
     * @param pagingOffsetParam
     *            the offset of list of modules for pagination, might not be
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
     *
     * @return a list of all modules for a defined or default page request with
     *         status OK. The response is always paged. In any failure the
     *         JsonResponseExceptionHandler is handling the response.
     */
    @RequestMapping(method = RequestMethod.GET, produces = { MediaTypes.HAL_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<PagedList<MgmtSoftwareModule>> getSoftwareModules(
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) final int pagingOffsetParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) final int pagingLimitParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false) final String sortParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false) final String rsqlParam);

    /**
     * Handles the GET request of retrieving a single software module.
     *
     * @param softwareModuleId
     *            the ID of the module to retrieve
     *
     * @return a single softwareModule with status OK.
     */
    @RequestMapping(method = RequestMethod.GET, value = "/{softwareModuleId}", produces = { MediaTypes.HAL_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtSoftwareModule> getSoftwareModule(@PathVariable("softwareModuleId") final Long softwareModuleId);

    /**
     * Handles the POST request of creating new softwaremodules. The request
     * body must always be a list of modules.
     *
     * @param softwareModules
     *            the modules to be created.
     * @return In case all modules could successful created the ResponseEntity
     *         with status code 201 - Created but without ResponseBody. In any
     *         failure the JsonResponseExceptionHandler is handling the
     *         response.
     */
    @RequestMapping(method = RequestMethod.POST, consumes = { MediaTypes.HAL_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE }, produces = { MediaTypes.HAL_JSON_VALUE,
                    MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<List<MgmtSoftwareModule>> createSoftwareModules(
            final List<MgmtSoftwareModuleRequestBodyPost> softwareModules);

    /**
     * Handles the PUT request of updating a software module.
     *
     * @param softwareModuleId
     *            the ID of the software module in the URL
     * @param restSoftwareModule
     *            the modules to be updated.
     * @return status OK if update was successful
     */
    @RequestMapping(method = RequestMethod.PUT, value = "/{softwareModuleId}", consumes = { MediaTypes.HAL_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE }, produces = { MediaTypes.HAL_JSON_VALUE,
                    MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtSoftwareModule> updateSoftwareModule(
            @PathVariable("softwareModuleId") final Long softwareModuleId,
            final MgmtSoftwareModuleRequestBodyPut restSoftwareModule);

    /**
     * Handles the DELETE request for a single software module.
     *
     * @param softwareModuleId
     *            the ID of the module to retrieve
     * @return status OK if delete was successful.
     *
     */
    @RequestMapping(method = RequestMethod.DELETE, value = "/{softwareModuleId}")
    ResponseEntity<Void> deleteSoftwareModule(@PathVariable("softwareModuleId") final Long softwareModuleId);

    /**
     * Gets a paged list of meta data for a software module.
     *
     * @param softwareModuleId
     *            the ID of the software module for the meta data
     * @param pagingOffsetParam
     *            the offset of list of meta data for pagination, might not be
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
    @RequestMapping(method = RequestMethod.GET, value = "/{softwareModuleId}/metadata", produces = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<PagedList<MgmtSoftwareModuleMetadata>> getMetadata(
            @PathVariable("softwareModuleId") final Long softwareModuleId,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) final int pagingOffsetParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) final int pagingLimitParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false) final String sortParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false) final String rsqlParam);

    /**
     * Gets a single meta data value for a specific key of a software module.
     *
     * @param softwareModuleId
     *            the ID of the software module to get the meta data from
     * @param metadataKey
     *            the key of the meta data entry to retrieve the value from
     * @return status OK if get request is successful with the value of the meta
     *         data
     */
    @RequestMapping(method = RequestMethod.GET, value = "/{softwareModuleId}/metadata/{metadataKey}", produces = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtSoftwareModuleMetadata> getMetadataValue(
            @PathVariable("softwareModuleId") final Long softwareModuleId,
            @PathVariable("metadataKey") final String metadataKey);

    /**
     * Updates a single meta data value of a software module.
     *
     * @param softwareModuleId
     *            the ID of the software module to update the meta data entry
     * @param metadataKey
     *            the key of the meta data to update the value
     * @param metadata
     *            body to update
     * @return status OK if the update request is successful and the updated
     *         meta data result
     */
    @RequestMapping(method = RequestMethod.PUT, value = "/{softwareModuleId}/metadata/{metadataKey}", produces = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtSoftwareModuleMetadata> updateMetadata(
            @PathVariable("softwareModuleId") final Long softwareModuleId,
            @PathVariable("metadataKey") final String metadataKey, final MgmtSoftwareModuleMetadataBodyPut metadata);

    /**
     * Deletes a single meta data entry from the software module.
     *
     * @param softwareModuleId
     *            the ID of the software module to delete the meta data entry
     * @param metadataKey
     *            the key of the meta data to delete
     * @return status OK if the delete request is successful
     */
    @RequestMapping(method = RequestMethod.DELETE, value = "/{softwareModuleId}/metadata/{metadataKey}")
    ResponseEntity<Void> deleteMetadata(@PathVariable("softwareModuleId") final Long softwareModuleId,
            @PathVariable("metadataKey") final String metadataKey);

    /**
     * Creates a list of meta data for a specific software module.
     *
     * @param softwareModuleId
     *            the ID of the distribution set to create meta data for
     * @param metadataRest
     *            the list of meta data entries to create
     * @return status created if post request is successful with the value of
     *         the created meta data
     */
    @RequestMapping(method = RequestMethod.POST, value = "/{softwareModuleId}/metadata", consumes = {
            MediaType.APPLICATION_JSON_VALUE,
            MediaTypes.HAL_JSON_VALUE }, produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<List<MgmtSoftwareModuleMetadata>> createMetadata(
            @PathVariable("softwareModuleId") final Long softwareModuleId,
            final List<MgmtSoftwareModuleMetadata> metadataRest);

}
