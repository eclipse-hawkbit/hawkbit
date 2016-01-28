/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.rest.resource;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.hawkbit.artifact.repository.model.DbArtifact;
import org.eclipse.hawkbit.repository.ArtifactManagement;
import org.eclipse.hawkbit.repository.SoftwareManagement;
import org.eclipse.hawkbit.repository.SoftwareModuleFields;
import org.eclipse.hawkbit.repository.SoftwareModuleMetadataFields;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.model.Artifact;
import org.eclipse.hawkbit.repository.model.LocalArtifact;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleMetadata;
import org.eclipse.hawkbit.repository.model.SwMetadataCompositeKey;
import org.eclipse.hawkbit.repository.rsql.RSQLUtility;
import org.eclipse.hawkbit.rest.resource.helper.RestResourceConversionHelper;
import org.eclipse.hawkbit.rest.resource.model.MetadataRest;
import org.eclipse.hawkbit.rest.resource.model.MetadataRestPageList;
import org.eclipse.hawkbit.rest.resource.model.artifact.ArtifactRest;
import org.eclipse.hawkbit.rest.resource.model.artifact.ArtifactsRest;
import org.eclipse.hawkbit.rest.resource.model.softwaremodule.SoftwareModulePagedList;
import org.eclipse.hawkbit.rest.resource.model.softwaremodule.SoftwareModuleRequestBodyPost;
import org.eclipse.hawkbit.rest.resource.model.softwaremodule.SoftwareModuleRequestBodyPut;
import org.eclipse.hawkbit.rest.resource.model.softwaremodule.SoftwareModuleRest;
import org.eclipse.hawkbit.rest.resource.model.softwaremodule.SoftwareModulesRest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST Resource handling for {@link SoftwareModule} and related
 * {@link Artifact} CRUD operations.
 *
 *
 *
 *
 */
@RestController
@RequestMapping(RestConstants.SOFTWAREMODULE_V1_REQUEST_MAPPING)
public class SoftwareModuleResource {
    private static final Logger LOG = LoggerFactory.getLogger(SoftwareModuleResource.class);

    @Autowired
    private ArtifactManagement artifactManagement;

    @Autowired
    private SoftwareManagement softwareManagement;

    /**
     * Handles POST request for artifact upload.
     *
     * @param softwareModuleId
     *            of the parent {@link SoftwareModule}
     * @param file
     *            that has to be uploaded
     * @param optionalFileName
     *            to override {@link MultipartFile#getOriginalFilename()}
     * @param md5Sum
     *            checksum for uploaded content check
     * @param sha1Sum
     *            checksum for uploaded content check
     *
     * @return {@link ResponseEntity} if status {@link HttpStatus#CREATED} if
     *         successful
     */
    @RequestMapping(method = RequestMethod.POST, value = "/{softwareModuleId}/artifacts", produces = {
            "application/hal+json", MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<ArtifactRest> uploadArtifact(@PathVariable final Long softwareModuleId,
            @RequestParam("file") final MultipartFile file,
            @RequestParam(value = "filename", required = false) final String optionalFileName,
            @RequestParam(value = "md5sum", required = false) final String md5Sum,
            @RequestParam(value = "sha1sum", required = false) final String sha1Sum) {

        Artifact result;
        if (!file.isEmpty()) {
            String fileName = optionalFileName;

            if (null == fileName) {
                fileName = file.getOriginalFilename();
            }

            try {
                result = artifactManagement.createLocalArtifact(file.getInputStream(), softwareModuleId, fileName,
                        md5Sum == null ? null : md5Sum.toLowerCase(), sha1Sum == null ? null : sha1Sum.toLowerCase(),
                        false, file.getContentType());
            } catch (final IOException e) {
                LOG.error("Failed to store artifact", e);
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } else {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        return new ResponseEntity<>(SoftwareModuleMapper.toResponse(result), HttpStatus.CREATED);

    }

    /**
     * Handles the GET request of retrieving all meta data of artifacts assigned
     * to a software module.
     *
     * @param softwareModuleId
     *            of the parent {@link SoftwareModule}
     *
     * @return {@link ResponseEntity} with status {@link HttpStatus#OK} if
     *         successful
     */
    @RequestMapping(method = RequestMethod.GET, value = "/{softwareModuleId}/artifacts", produces = {
            "application/hal+json", MediaType.APPLICATION_JSON_VALUE })
    @ResponseBody
    public ResponseEntity<ArtifactsRest> getArtifacts(@PathVariable final Long softwareModuleId) {
        final SoftwareModule module = findSoftwareModuleWithExceptionIfNotFound(softwareModuleId, null);

        return new ResponseEntity<>(SoftwareModuleMapper.artifactsToResponse(module.getArtifacts()), HttpStatus.OK);
    }

    /**
     * Handles the GET request for downloading an artifact.
     *
     * @param softwareModuleId
     *            of the parent {@link SoftwareModule}
     * @param artifactId
     *            of the related {@link LocalArtifact}
     * @param servletResponse
     *            of the servlet
     * @param request
     *            of the client
     *
     * @return {@link ResponseEntity} with status {@link HttpStatus#OK} if
     *         successful
     */
    @RequestMapping(method = RequestMethod.GET, value = "/{softwareModuleId}/artifacts/{artifactId}/download")
    @ResponseBody
    public ResponseEntity<Void> downloadArtifact(@PathVariable final Long softwareModuleId,
            @PathVariable final Long artifactId, final HttpServletResponse servletResponse,
            final HttpServletRequest request) {
        final SoftwareModule module = findSoftwareModuleWithExceptionIfNotFound(softwareModuleId, artifactId);

        if (null == module || !module.getLocalArtifact(artifactId).isPresent()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        final LocalArtifact artifact = module.getLocalArtifact(artifactId).get();
        final DbArtifact file = artifactManagement.loadLocalArtifactBinary(artifact);

        final String ifMatch = request.getHeader("If-Match");
        if (ifMatch != null && !RestResourceConversionHelper.matchesHttpHeader(ifMatch, artifact.getSha1Hash())) {
            return new ResponseEntity<>(HttpStatus.PRECONDITION_FAILED);
        }

        return RestResourceConversionHelper.writeFileResponse(artifact, servletResponse, request, file);

    }

    /**
     * Handles the GET request of retrieving a single Artifact meta data
     * request.
     *
     * @param softwareModuleId
     *            of the parent {@link SoftwareModule}
     * @param artifactId
     *            of the related {@link LocalArtifact}
     *
     * @return {@link ResponseEntity} with status {@link HttpStatus#OK} if
     *         successful
     */
    @RequestMapping(method = RequestMethod.GET, value = "/{softwareModuleId}/artifacts/{artifactId}", produces = {
            "application/hal+json", MediaType.APPLICATION_JSON_VALUE })
    @ResponseBody
    public ResponseEntity<ArtifactRest> getArtifact(@PathVariable final Long softwareModuleId,
            @PathVariable final Long artifactId) {
        final SoftwareModule module = findSoftwareModuleWithExceptionIfNotFound(softwareModuleId, artifactId);

        return new ResponseEntity<>(SoftwareModuleMapper.toResponse(module.getLocalArtifact(artifactId).get()),
                HttpStatus.OK);
    }

    /**
     * Handles the DELETE request for a single SoftwareModule within SP.
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
    public ResponseEntity<Void> deleteArtifact(@PathVariable final Long softwareModuleId,
            @PathVariable final Long artifactId) {
        findSoftwareModuleWithExceptionIfNotFound(softwareModuleId, artifactId);

        artifactManagement.deleteLocalArtifact(artifactId);

        return new ResponseEntity<>(HttpStatus.OK);

    }

    /**
     * Handles the GET request of retrieving all softwaremodules within SP.
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
    @RequestMapping(method = RequestMethod.GET, produces = { "application/hal+json", MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<SoftwareModulePagedList> getSoftwareModules(
            @RequestParam(value = RestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = RestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) final int pagingOffsetParam,
            @RequestParam(value = RestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = RestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) final int pagingLimitParam,
            @RequestParam(value = RestConstants.REQUEST_PARAMETER_SORTING, required = false) final String sortParam,
            @RequestParam(value = RestConstants.REQUEST_PARAMETER_SEARCH, required = false) final String rsqlParam) {

        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeSoftwareModuleSortParam(sortParam);

        final Pageable pageable = new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting);

        final Slice<SoftwareModule> findModulesAll;
        Long countModulesAll;
        if (rsqlParam != null) {
            findModulesAll = softwareManagement
                    .findSoftwareModulesByPredicate(RSQLUtility.parse(rsqlParam, SoftwareModuleFields.class), pageable);
            countModulesAll = ((Page<SoftwareModule>) findModulesAll).getTotalElements();
        } else {
            findModulesAll = softwareManagement.findSoftwareModulesAll(pageable);
            countModulesAll = softwareManagement.countSoftwareModulesAll();
        }

        final List<SoftwareModuleRest> rest = SoftwareModuleMapper.toResponse(findModulesAll.getContent());
        return new ResponseEntity<>(new SoftwareModulePagedList(rest, countModulesAll), HttpStatus.OK);
    }

    /**
     * Handles the GET request of retrieving a single software module within SP.
     *
     * @param softwareModuleId
     *            the ID of the module to retrieve
     *
     * @return a single softwareModule with status OK.
     * @throws EntityNotFoundException
     *             in case no with the given {@code softwareModuleId} exists.
     */
    @RequestMapping(method = RequestMethod.GET, value = "/{softwareModuleId}", produces = { "application/hal+json",
            MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<SoftwareModuleRest> getSoftwareModule(@PathVariable final Long softwareModuleId) {
        final SoftwareModule findBaseSoftareModule = findSoftwareModuleWithExceptionIfNotFound(softwareModuleId, null);

        return new ResponseEntity<>(SoftwareModuleMapper.toResponse(findBaseSoftareModule), HttpStatus.OK);
    }

    /**
     * Handles the POST request of creating new softwaremodules within SP. The
     * request body must always be a list of modules. The requests is delgating
     * to the {@link SoftwareManagement#createSoftwareModule(Iterable)}.
     *
     * @param softwareModules
     *            the modules to be created.
     * @return In case all modules could successful created the ResponseEntity
     *         with status code 201 - Created but without ResponseBody. In any
     *         failure the JsonResponseExceptionHandler is handling the
     *         response.
     */
    @RequestMapping(method = RequestMethod.POST, consumes = { "application/hal+json",
            MediaType.APPLICATION_JSON_VALUE }, produces = { "application/hal+json", MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<SoftwareModulesRest> createSoftwareModules(
            @RequestBody final List<SoftwareModuleRequestBodyPost> softwareModules) {
        LOG.debug("creating {} softwareModules", softwareModules.size());
        final Iterable<SoftwareModule> createdSoftwareModules = softwareManagement
                .createSoftwareModule(SoftwareModuleMapper.smFromRequest(softwareModules, softwareManagement));
        LOG.debug("{} softwareModules created, return status {}", softwareModules.size(), HttpStatus.CREATED);

        return new ResponseEntity<>(SoftwareModuleMapper.toResponseSoftwareModules(createdSoftwareModules),
                HttpStatus.CREATED);
    }

    /**
     * Handles the PUT request of updating a software module within SP.
     * {@link SoftwareManagement#createSoftwareModule(Iterable)}.
     *
     * @param softwareModuleId
     *            the ID of the software module in the URL
     * @param restSoftwareModule
     *            the modules to be updated.
     * @return status OK if update is successful
     */
    @RequestMapping(method = RequestMethod.PUT, value = "/{softwareModuleId}", consumes = { "application/hal+json",
            MediaType.APPLICATION_JSON_VALUE }, produces = { "application/hal+json", MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<SoftwareModuleRest> updateSoftwareModule(@PathVariable final Long softwareModuleId,
            @RequestBody final SoftwareModuleRequestBodyPut restSoftwareModule) {
        final SoftwareModule module = findSoftwareModuleWithExceptionIfNotFound(softwareModuleId, null);

        // only description and vendor can be modified
        if (restSoftwareModule.getDescription() != null) {
            module.setDescription(restSoftwareModule.getDescription());
        }
        if (restSoftwareModule.getVendor() != null) {
            module.setVendor(restSoftwareModule.getVendor());
        }

        final SoftwareModule updateSoftwareModule = softwareManagement.updateSoftwareModule(module);
        return new ResponseEntity<>(SoftwareModuleMapper.toResponse(updateSoftwareModule), HttpStatus.OK);
    }

    /**
     * Handles the DELETE request for a single softwaremodule within SP.
     *
     * @param softwareModuleId
     *            the ID of the module to retrieve
     * @return status OK if delete as sucessfull.
     *
     */
    @RequestMapping(method = RequestMethod.DELETE, value = "/{softwareModuleId}")
    public ResponseEntity<Void> deleteSoftwareModule(@PathVariable final Long softwareModuleId) {
        final SoftwareModule module = findSoftwareModuleWithExceptionIfNotFound(softwareModuleId, null);

        softwareManagement.deleteSoftwareModule(module);

        return new ResponseEntity<>(HttpStatus.OK);
    }

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
            MediaType.APPLICATION_JSON_VALUE, "application/hal+json" })
    public ResponseEntity<MetadataRestPageList> getMetadata(@PathVariable final Long softwareModuleId,
            @RequestParam(value = RestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = RestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) final int pagingOffsetParam,
            @RequestParam(value = RestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = RestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) final int pagingLimitParam,
            @RequestParam(value = RestConstants.REQUEST_PARAMETER_SORTING, required = false) final String sortParam,
            @RequestParam(value = RestConstants.REQUEST_PARAMETER_SEARCH, required = false) final String rsqlParam) {

        // check if software module exists otherwise throw exception immediately
        findSoftwareModuleWithExceptionIfNotFound(softwareModuleId, null);

        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeSoftwareModuleMetadataSortParam(sortParam);

        final Pageable pageable = new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting);
        final Page<SoftwareModuleMetadata> metaDataPage;

        if (rsqlParam != null) {
            metaDataPage = softwareManagement.findSoftwareModuleMetadataBySoftwareModuleId(softwareModuleId,
                    RSQLUtility.parse(rsqlParam, SoftwareModuleMetadataFields.class), pageable);
        } else {
            metaDataPage = softwareManagement.findSoftwareModuleMetadataBySoftwareModuleId(softwareModuleId, pageable);
        }

        return new ResponseEntity<>(
                new MetadataRestPageList(SoftwareModuleMapper.toResponseSwMetadata(metaDataPage.getContent()),
                        metaDataPage.getTotalElements()),
                HttpStatus.OK);
    }

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
            MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<MetadataRest> getMetadataValue(@PathVariable final Long softwareModuleId,
            @PathVariable final String metadataKey) {
        // check if distribution set exists otherwise throw exception
        // immediately
        final SoftwareModule sw = findSoftwareModuleWithExceptionIfNotFound(softwareModuleId, null);
        final SoftwareModuleMetadata findOne = softwareManagement.findOne(new SwMetadataCompositeKey(sw, metadataKey));
        return ResponseEntity.<MetadataRest> ok(SoftwareModuleMapper.toResponseSwMetadata(findOne));
    }

    /**
     * Updates a single meta data value of a software module.
     *
     * @param softwareModuleId
     *            the ID of the software module to update the meta data entry
     * @param metadataKey
     *            the key of the meta data to update the value
     * @return status OK if the update request is successful and the updated
     *         meta data result
     */
    @RequestMapping(method = RequestMethod.PUT, value = "/{softwareModuleId}/metadata/{metadataKey}", produces = {
            MediaType.APPLICATION_JSON_VALUE, "application/hal+json" })
    public ResponseEntity<MetadataRest> updateMetadata(@PathVariable final Long softwareModuleId,
            @PathVariable final String metadataKey, @RequestBody final MetadataRest metadata) {
        // check if software module exists otherwise throw exception immediately
        final SoftwareModule sw = findSoftwareModuleWithExceptionIfNotFound(softwareModuleId, null);
        final SoftwareModuleMetadata updated = softwareManagement
                .updateSoftwareModuleMetadata(new SoftwareModuleMetadata(metadataKey, sw, metadata.getValue()));
        return ResponseEntity.ok(SoftwareModuleMapper.toResponseSwMetadata(updated));
    }

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
    public ResponseEntity<Void> deleteMetadata(@PathVariable final Long softwareModuleId,
            @PathVariable final String metadataKey) {
        // check if software module exists otherwise throw exception immediately
        final SoftwareModule sw = findSoftwareModuleWithExceptionIfNotFound(softwareModuleId, null);
        softwareManagement.deleteSoftwareModuleMetadata(new SwMetadataCompositeKey(sw, metadataKey));
        return ResponseEntity.ok().build();
    }

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
            "application/hal+json" }, produces = { MediaType.APPLICATION_JSON_VALUE, "application/hal+json" })
    public ResponseEntity<List<MetadataRest>> createMetadata(@PathVariable final Long softwareModuleId,
            @RequestBody final List<MetadataRest> metadataRest) {
        // check if software module exists otherwise throw exception immediately
        final SoftwareModule sw = findSoftwareModuleWithExceptionIfNotFound(softwareModuleId, null);

        final List<SoftwareModuleMetadata> created = softwareManagement
                .createSoftwareModuleMetadata(SoftwareModuleMapper.fromRequestSwMetadata(sw, metadataRest));

        return new ResponseEntity<>(SoftwareModuleMapper.toResponseSwMetadata(created), HttpStatus.CREATED);

    }

    private SoftwareModule findSoftwareModuleWithExceptionIfNotFound(final Long softwareModuleId,
            final Long artifactId) {
        final SoftwareModule module = softwareManagement.findSoftwareModuleById(softwareModuleId);
        if (module == null) {
            throw new EntityNotFoundException("SoftwareModule with Id {" + softwareModuleId + "} does not exist");
        } else if (artifactId != null && !module.getLocalArtifact(artifactId).isPresent()) {
            throw new EntityNotFoundException("Artifact with Id {" + artifactId + "} does not exist");
        }
        return module;
    }
}
