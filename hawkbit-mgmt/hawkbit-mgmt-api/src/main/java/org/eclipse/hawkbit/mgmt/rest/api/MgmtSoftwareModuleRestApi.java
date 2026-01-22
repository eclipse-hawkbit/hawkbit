/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.rest.api;

import static org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT;
import static org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET;
import static org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT;
import static org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET;
import static org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants.SOFTWARE_MODULE_ORDER;
import static org.eclipse.hawkbit.rest.ApiResponsesConstants.DeleteResponses;
import static org.eclipse.hawkbit.rest.ApiResponsesConstants.GONE_410;
import static org.eclipse.hawkbit.rest.ApiResponsesConstants.GetIfExistResponses;
import static org.eclipse.hawkbit.rest.ApiResponsesConstants.GetResponses;
import static org.eclipse.hawkbit.rest.ApiResponsesConstants.INTERNAL_SERVER_ERROR_500;
import static org.eclipse.hawkbit.rest.ApiResponsesConstants.LOCKED_423;
import static org.eclipse.hawkbit.rest.ApiResponsesConstants.NOT_FOUND_404;
import static org.eclipse.hawkbit.rest.ApiResponsesConstants.PostCreateResponses;
import static org.eclipse.hawkbit.rest.ApiResponsesConstants.PutResponses;
import static org.springframework.hateoas.MediaTypes.HAL_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.eclipse.hawkbit.mgmt.json.model.PagedList;
import org.eclipse.hawkbit.mgmt.json.model.artifact.MgmtArtifact;
import org.eclipse.hawkbit.mgmt.json.model.softwaremodule.MgmtSoftwareModule;
import org.eclipse.hawkbit.mgmt.json.model.softwaremodule.MgmtSoftwareModuleMetadata;
import org.eclipse.hawkbit.mgmt.json.model.softwaremodule.MgmtSoftwareModuleMetadataBodyPut;
import org.eclipse.hawkbit.mgmt.json.model.softwaremodule.MgmtSoftwareModuleRequestBodyPost;
import org.eclipse.hawkbit.mgmt.json.model.softwaremodule.MgmtSoftwareModuleRequestBodyPut;
import org.eclipse.hawkbit.rest.OpenApi;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST API for SoftwareModule and related Artifact CRUD operations.
 */
// no request mapping specified here to avoid CVE-2021-22044 in Feign client
@Tag(name = "Software Modules", description = "REST API for SoftwareModule and related Artifact CRUD operations.",
        extensions = @Extension(name = OpenApi.X_HAWKBIT, properties = @ExtensionProperty(name = "order", value = SOFTWARE_MODULE_ORDER)))
public interface MgmtSoftwareModuleRestApi {

    String SOFTWAREMODULES_V1 = MgmtRestConstants.REST_V1 + "/softwaremodules";
    String REQUEST_PARAMETER_USE_ARTIFACT_URL_HANDLER = "useartifacturlhandler";

    /**
     * Handles POST request for artifact upload.
     *
     * @param softwareModuleId of the parent SoftwareModule
     * @param file that has to be uploaded
     * @param optionalFileName to override {@link MultipartFile#getOriginalFilename()}
     * @param md5Sum checksum for uploaded content check
     * @param sha1Sum checksum for uploaded content check
     * @param sha256Sum checksum for uploaded content check
     * @return In case all sets could be successfully be created the ResponseEntity with status code 201 - Created but without ResponseBody. In any
     *         failure the JsonResponseExceptionHandler is handling the response.
     */
    @Operation(summary = "Upload artifact", description = "Handles POST request for artifact upload. Required Permission: CREATE_REPOSITORY")
    @PostCreateResponses
    @ApiResponses(value = {
            @ApiResponse(responseCode = NOT_FOUND_404, description = "Software Module not found",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = GONE_410, description = "Artifact binary no longer exists",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = LOCKED_423, description = "Software module is locked",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = INTERNAL_SERVER_ERROR_500, description = "Upload / store to storage or encryption failed",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @PostMapping(value = SOFTWAREMODULES_V1 + "/{softwareModuleId}/artifacts",
            consumes = MULTIPART_FORM_DATA_VALUE, produces = { HAL_JSON_VALUE, APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtArtifact> uploadArtifact(
            @PathVariable("softwareModuleId") Long softwareModuleId,
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "filename", required = false) String optionalFileName,
            @RequestParam(value = "md5sum", required = false) String md5Sum,
            @RequestParam(value = "sha1sum", required = false) String sha1Sum,
            @RequestParam(value = "sha256sum", required = false) String sha256Sum);

    /**
     * Handles the GET request of retrieving all metadata of artifacts assigned to a software module.
     *
     * @param softwareModuleId of the parent SoftwareModule
     * @return a list of all artifacts for a defined or default page request with status OK. The response is always paged. In any failure the
     *         JsonResponseExceptionHandler is handling the response.
     */
    @Operation(summary = "Return all metadata of artifacts assigned to a software module",
            description = "Handles the GET request of retrieving all metadata of artifacts assigned to a " +
                    "software module. Required Permission: READ_REPOSITORY")
    @GetResponses
    @ApiResponses(value = {
            @ApiResponse(responseCode = GONE_410, description = "Artifact binary no longer exists",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @GetMapping(value = SOFTWAREMODULES_V1 + "/{softwareModuleId}/artifacts", produces = { HAL_JSON_VALUE, APPLICATION_JSON_VALUE })
    ResponseEntity<List<MgmtArtifact>> getArtifacts(
            @PathVariable("softwareModuleId") Long softwareModuleId,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_REPRESENTATION_MODE, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_REPRESENTATION_MODE_DEFAULT) String representationModeParam,
            @RequestParam(value = REQUEST_PARAMETER_USE_ARTIFACT_URL_HANDLER, required = false) Boolean useArtifactUrlHandler);

    /**
     * Handles the GET request of retrieving a single Artifact metadata request.
     *
     * @param softwareModuleId of the parent SoftwareModule
     * @param artifactId of the related artifact
     * @return responseEntity with status ok if successful
     */
    @Operation(summary = "Return single Artifact metadata",
            description = "Handles the GET request of retrieving a single Artifact metadata request. Required Permission: READ_REPOSITORY")
    @GetResponses
    @ApiResponses(value = {
            @ApiResponse(responseCode = GONE_410, description = "Artifact binary no longer exists",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @GetMapping(value = SOFTWAREMODULES_V1 + "/{softwareModuleId}/artifacts/{artifactId}",
            produces = { HAL_JSON_VALUE, APPLICATION_JSON_VALUE })
    @ResponseBody
    ResponseEntity<MgmtArtifact> getArtifact(
            @PathVariable("softwareModuleId") Long softwareModuleId,
            @PathVariable("artifactId") Long artifactId,
            @RequestParam(value = REQUEST_PARAMETER_USE_ARTIFACT_URL_HANDLER, required = false) Boolean useArtifactUrlHandler);

    /**
     * Handles the DELETE request for a single SoftwareModule.
     *
     * @param softwareModuleId the ID of the module that has the artifact
     * @param artifactId of the artifact to be deleted
     * @return status OK if delete as successful.
     */
    @Operation(summary = "Delete artifact by Id",
            description = "Handles the DELETE request for a single Artifact assigned to a SoftwareModule. Required Permission: DELETE_REPOSITORY")
    @DeleteResponses
    @ApiResponses(value = {
            @ApiResponse(responseCode = LOCKED_423, description = "Software module is locked",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = INTERNAL_SERVER_ERROR_500, description = "Artifact delete failed with internal server error",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @DeleteMapping(value = SOFTWAREMODULES_V1 + "/{softwareModuleId}/artifacts/{artifactId}")
    @ResponseBody
    ResponseEntity<Void> deleteArtifact(
            @PathVariable("softwareModuleId") Long softwareModuleId,
            @PathVariable("artifactId") Long artifactId);

    /**
     * Handles the GET request of retrieving all software modules.
     *
     * @param rsqlParam the search parameter in the request URL, syntax {@code q=name==abc}
     * @param pagingOffsetParam the offset of list of modules for pagination, might not be present in the rest request then default value will
     *         be applied
     * @param pagingLimitParam the limit of the paged request, might not be present in the rest request then default value will be applied
     * @param sortParam the sorting parameter in the request URL, syntax {@code field:direction, field:direction}
     * @return a list of all modules for a defined or default page request with status OK. The response is always paged. In any failure the
     *         JsonResponseExceptionHandler is handling the response.
     */
    @Operation(summary = "Return all Software modules",
            description = "Handles the GET request of retrieving all softwaremodules. Required Permission: READ_REPOSITORY")
    @GetIfExistResponses
    @GetMapping(value = SOFTWAREMODULES_V1, produces = { HAL_JSON_VALUE, APPLICATION_JSON_VALUE })
    ResponseEntity<PagedList<MgmtSoftwareModule>> getSoftwareModules(
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false)
            @Schema(description = "Query fields based on the Feed Item Query Language (FIQL). See Entity Definitions for available fields.")
            String rsqlParam,
            @RequestParam(value = REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET)
            @Schema(description = "The paging offset (default is 0)")
            int pagingOffsetParam,
            @RequestParam(value = REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT)
            @Schema(description = "The maximum number of entries in a page (default is 50)")
            int pagingLimitParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false)
            @Schema(description = "The query parameter sort allows to define the sort order for the result of a query. " +
                    "A sort criteria consists of the name of a field and the sort direction (ASC for ascending and DESC descending)." +
                    "The sequence of the sort criteria (multiple can be used) defines the sort order of the entities in the result.")
            String sortParam);

    /**
     * Handles the GET request of retrieving a single software module.
     *
     * @param softwareModuleId the ID of the module to retrieve
     * @return a single softwareModule with status OK.
     */
    @Operation(summary = "Return Software Module by id", description = "Handles the GET request of retrieving a single softwaremodule. Required Permission: READ_REPOSITORY")
    @GetResponses
    @GetMapping(value = SOFTWAREMODULES_V1 + "/{softwareModuleId}", produces = { HAL_JSON_VALUE, APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtSoftwareModule> getSoftwareModule(@PathVariable("softwareModuleId") Long softwareModuleId);

    /**
     * Handles the POST request of creating new software modules. The request body must always be a list of modules.
     *
     * @param softwareModules the modules to be created.
     * @return In case all modules could successful created the ResponseEntity with status code 201 - Created but without ResponseBody. In any
     *         failure the JsonResponseExceptionHandler is handling the response.
     */
    @Operation(summary = "Create Software Module(s)",
            description = "Handles the POST request of creating new software modules. The request body must always be a list of modules. Required Permission: CREATE_REPOSITORY")
    @PostCreateResponses
    @ApiResponses(value = {
            @ApiResponse(responseCode = INTERNAL_SERVER_ERROR_500, description = "Artifact encryption failed",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @PostMapping(value = SOFTWAREMODULES_V1,
            consumes = { HAL_JSON_VALUE, APPLICATION_JSON_VALUE }, produces = { HAL_JSON_VALUE, APPLICATION_JSON_VALUE })
    ResponseEntity<List<MgmtSoftwareModule>> createSoftwareModules(@RequestBody List<MgmtSoftwareModuleRequestBodyPost> softwareModules);

    /**
     * Handles the PUT request of updating a software module.
     *
     * @param softwareModuleId the ID of the software module in the URL
     * @param restSoftwareModule the modules to be updated.
     * @return status OK if update was successful
     */
    @Operation(summary = "Update Software Module",
            description = "Handles the PUT request for a single softwaremodule within Hawkbit. Required Permission: UPDATE_REPOSITORY")
    @PutResponses
    @PutMapping(value = SOFTWAREMODULES_V1 + "/{softwareModuleId}",
            consumes = { HAL_JSON_VALUE, APPLICATION_JSON_VALUE }, produces = { HAL_JSON_VALUE, APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtSoftwareModule> updateSoftwareModule(
            @PathVariable("softwareModuleId") Long softwareModuleId,
            @RequestBody MgmtSoftwareModuleRequestBodyPut restSoftwareModule);

    /**
     * Handles the DELETE request for a single software module.
     *
     * @param softwareModuleId the ID of the module to retrieve
     * @return status OK if delete was successful.
     */
    @Operation(summary = "Delete Software Module by Id",
            description = "Handles the DELETE request for a single softwaremodule within Hawkbit. Required Permission: DELETE_REPOSITORY")
    @DeleteResponses
    @DeleteMapping(value = SOFTWAREMODULES_V1 + "/{softwareModuleId}")
    ResponseEntity<Void> deleteSoftwareModule(@PathVariable("softwareModuleId") Long softwareModuleId);

    /**
     * Creates a list of metadata for a specific software module.
     *
     * @param softwareModuleId the ID of the distribution set to create metadata for
     * @param metadataRest the list of metadata entries to create
     * @return status created if post request is successful with the value of the created metadata
     */
    @Operation(summary = "Creates a list of metadata for a specific Software Module",
            description = "Create a list of metadata entries Required Permission: UPDATE_REPOSITORY")
    @PostCreateResponses
    @ApiResponses(value = {
            @ApiResponse(responseCode = NOT_FOUND_404, description = "Software Module not found",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @PostMapping(value = SOFTWAREMODULES_V1 + "/{softwareModuleId}/metadata", consumes = { APPLICATION_JSON_VALUE, HAL_JSON_VALUE })
    ResponseEntity<Void> createMetadata(@PathVariable("softwareModuleId") Long softwareModuleId,
            @RequestBody List<MgmtSoftwareModuleMetadata> metadataRest);

    /**
     * Gets a paged list of metadata for a software module.
     *
     * @param softwareModuleId the ID of the software module for the metadata
     * @return status OK with the paged list of metadata, if the request is successful
     */
    @Operation(summary = "Return metadata for a Software Module", description = "Get a paged list of metadata for a software module. Required Permission: READ_REPOSITORY")
    @GetResponses
    @GetMapping(value = SOFTWAREMODULES_V1 + "/{softwareModuleId}/metadata", produces = { HAL_JSON_VALUE, APPLICATION_JSON_VALUE })
    ResponseEntity<PagedList<MgmtSoftwareModuleMetadata>> getMetadata(@PathVariable("softwareModuleId") Long softwareModuleId);

    /**
     * Gets a single metadata value for a specific key of a software module.
     *
     * @param softwareModuleId the ID of the software module to get the metadata from
     * @param metadataKey the key of the metadata entry to retrieve the value from
     * @return status OK if get request is successful with the value of the metadata
     */
    @Operation(summary = "Return single metadata value for a specific key of a Software Module",
            description = "Get a single metadata value for a metadata key. Required Permission: READ_REPOSITORY")
    @GetResponses
    @GetMapping(value = SOFTWAREMODULES_V1 + "/{softwareModuleId}/metadata/{metadataKey}",
            produces = { HAL_JSON_VALUE, APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtSoftwareModuleMetadata> getMetadataValue(
            @PathVariable("softwareModuleId") Long softwareModuleId,
            @PathVariable("metadataKey") String metadataKey);

    /**
     * Updates a single metadata value of a software module.
     *
     * @param softwareModuleId the ID of the software module to update the metadata entry
     * @param metadataKey the key of the metadata to update the value
     * @param metadata body to update
     */
    @Operation(summary = "Update a single metadata value of a Software Module",
            description = "Update a single metadata value for specific key. Required Permission: UPDATE_REPOSITORY")
    @PutResponses
    @PutMapping(value = SOFTWAREMODULES_V1 + "/{softwareModuleId}/metadata/{metadataKey}")
    ResponseEntity<Void> updateMetadata(
            @PathVariable("softwareModuleId") Long softwareModuleId,
            @PathVariable("metadataKey") String metadataKey,
            @RequestBody MgmtSoftwareModuleMetadataBodyPut metadata);

    /**
     * Deletes a single metadata entry from the software module.
     *
     * @param softwareModuleId the ID of the software module to delete the metadata entry
     * @param metadataKey the key of the metadata to delete
     */
    @Operation(summary = "Delete single metadata entry from the software module",
            description = "Delete a single metadata. Required Permission: UPDATE_REPOSITORY")
    @DeleteResponses
    @DeleteMapping(value = SOFTWAREMODULES_V1 + "/{softwareModuleId}/metadata/{metadataKey}")
    ResponseEntity<Void> deleteMetadata(@PathVariable("softwareModuleId") Long softwareModuleId,
            @PathVariable("metadataKey") String metadataKey);
}