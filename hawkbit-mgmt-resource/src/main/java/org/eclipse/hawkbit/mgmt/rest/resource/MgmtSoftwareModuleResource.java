/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.rest.resource;

import static org.eclipse.hawkbit.mgmt.rest.resource.MgmtSoftwareModuleMapper.artifactsToResponse;
import static org.eclipse.hawkbit.mgmt.rest.resource.MgmtSoftwareModuleMapper.toResponse;
import static org.eclipse.hawkbit.mgmt.rest.resource.MgmtSoftwareModuleMapper.toResponseSwMetadata;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.eclipse.hawkbit.mgmt.json.model.MgmtMetadata;
import org.eclipse.hawkbit.mgmt.json.model.PagedList;
import org.eclipse.hawkbit.mgmt.json.model.artifact.MgmtArtifact;
import org.eclipse.hawkbit.mgmt.json.model.softwaremodule.MgmtSoftwareModule;
import org.eclipse.hawkbit.mgmt.json.model.softwaremodule.MgmtSoftwareModuleRequestBodyPost;
import org.eclipse.hawkbit.mgmt.json.model.softwaremodule.MgmtSoftwareModuleRequestBodyPut;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtSoftwareModuleRestApi;
import org.eclipse.hawkbit.repository.ArtifactManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.OffsetBasedPageRequest;
import org.eclipse.hawkbit.repository.SoftwareManagement;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.model.Artifact;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleMetadata;
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
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST Resource handling for {@link SoftwareModule} and related
 * {@link Artifact} CRUD operations.
 */
@RestController
public class MgmtSoftwareModuleResource implements MgmtSoftwareModuleRestApi {

    private static final Logger LOG = LoggerFactory.getLogger(MgmtSoftwareModuleResource.class);

    @Autowired
    private ArtifactManagement artifactManagement;

    @Autowired
    private SoftwareManagement softwareManagement;

    @Autowired
    private EntityFactory entityFactory;

    @Override
    public ResponseEntity<MgmtArtifact> uploadArtifact(@PathVariable("softwareModuleId") final Long softwareModuleId,
            @RequestParam("file") final MultipartFile file,
            @RequestParam(value = "filename", required = false) final String optionalFileName,
            @RequestParam(value = "md5sum", required = false) final String md5Sum,
            @RequestParam(value = "sha1sum", required = false) final String sha1Sum) {

        if (file.isEmpty()) {
            return new ResponseEntity<>(BAD_REQUEST);
        }
        String fileName = optionalFileName;

        if (fileName == null) {
            fileName = file.getOriginalFilename();
        }

        try {
            final Artifact result = artifactManagement.createArtifact(file.getInputStream(), softwareModuleId, fileName,
                    md5Sum == null ? null : md5Sum.toLowerCase(), sha1Sum == null ? null : sha1Sum.toLowerCase(), false,
                    file.getContentType());
            return ResponseEntity.status(CREATED).body(toResponse(result));
        } catch (final IOException e) {
            LOG.error("Failed to store artifact", e);
            return new ResponseEntity<>(INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    @ResponseBody
    public ResponseEntity<List<MgmtArtifact>> getArtifacts(
            @PathVariable("softwareModuleId") final Long softwareModuleId) {

        final SoftwareModule module = findSoftwareModuleWithExceptionIfNotFound(softwareModuleId, null);
        return ResponseEntity.ok(artifactsToResponse(module.getArtifacts()));
    }

    @Override
    @ResponseBody
    // Exception squid:S3655 - Optional access is checked in
    // findSoftwareModuleWithExceptionIfNotFound
    // subroutine
    @SuppressWarnings("squid:S3655")
    public ResponseEntity<MgmtArtifact> getArtifact(@PathVariable("softwareModuleId") final Long softwareModuleId,
            @PathVariable("artifactId") final Long artifactId) {

        final SoftwareModule module = findSoftwareModuleWithExceptionIfNotFound(softwareModuleId, artifactId);

        return ResponseEntity.ok(toResponse(module.getArtifact(artifactId).get()));
    }

    @Override
    @ResponseBody
    public ResponseEntity<Void> deleteArtifact(@PathVariable("softwareModuleId") final Long softwareModuleId,
            @PathVariable("artifactId") final Long artifactId) {

        findSoftwareModuleWithExceptionIfNotFound(softwareModuleId, artifactId);
        artifactManagement.deleteArtifact(artifactId);

        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<PagedList<MgmtSoftwareModule>> getSoftwareModules(
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) final int pagingOffsetParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) final int pagingLimitParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false) final String sortParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false) final String rsqlParam) {

        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeSoftwareModuleSortParam(sortParam);

        final Pageable pageable = new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting);

        final Slice<SoftwareModule> findModulesAll;
        Long countModulesAll;
        if (rsqlParam != null) {
            findModulesAll = softwareManagement.findSoftwareModulesByPredicate(rsqlParam, pageable);
            countModulesAll = ((Page<SoftwareModule>) findModulesAll).getTotalElements();
        } else {
            findModulesAll = softwareManagement.findSoftwareModulesAll(pageable);
            countModulesAll = softwareManagement.countSoftwareModulesAll();
        }

        final List<MgmtSoftwareModule> rest = MgmtSoftwareModuleMapper.toResponse(findModulesAll.getContent());
        return ResponseEntity.ok(new PagedList<>(rest, countModulesAll));
    }

    @Override
    public ResponseEntity<MgmtSoftwareModule> getSoftwareModule(
            @PathVariable("softwareModuleId") final Long softwareModuleId) {

        final SoftwareModule findBaseSoftareModule = findSoftwareModuleWithExceptionIfNotFound(softwareModuleId, null);
        return ResponseEntity.ok(toResponse(findBaseSoftareModule));
    }

    @Override
    public ResponseEntity<List<MgmtSoftwareModule>> createSoftwareModules(
            @RequestBody final List<MgmtSoftwareModuleRequestBodyPost> softwareModules) {

        LOG.debug("creating {} softwareModules", softwareModules.size());
        final Collection<SoftwareModule> createdSoftwareModules = softwareManagement.createSoftwareModule(
                MgmtSoftwareModuleMapper.smFromRequest(entityFactory, softwareModules, softwareManagement));
        LOG.debug("{} softwareModules created, return status {}", softwareModules.size(), HttpStatus.CREATED);

        return ResponseEntity.status(CREATED).body(toResponse(createdSoftwareModules));
    }

    @Override
    public ResponseEntity<MgmtSoftwareModule> updateSoftwareModule(
            @PathVariable("softwareModuleId") final Long softwareModuleId,
            @RequestBody final MgmtSoftwareModuleRequestBodyPut restSoftwareModule) {

        final SoftwareModule module = findSoftwareModuleWithExceptionIfNotFound(softwareModuleId, null);

        // only description and vendor can be modified
        if (restSoftwareModule.getDescription() != null) {
            module.setDescription(restSoftwareModule.getDescription());
        }
        if (restSoftwareModule.getVendor() != null) {
            module.setVendor(restSoftwareModule.getVendor());
        }

        final SoftwareModule updateSoftwareModule = softwareManagement.updateSoftwareModule(module);
        return ResponseEntity.ok(toResponse(updateSoftwareModule));
    }

    @Override
    public ResponseEntity<Void> deleteSoftwareModule(@PathVariable("softwareModuleId") final Long softwareModuleId) {

        final SoftwareModule module = findSoftwareModuleWithExceptionIfNotFound(softwareModuleId, null);
        softwareManagement.deleteSoftwareModule(module.getId());

        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<PagedList<MgmtMetadata>> getMetadata(
            @PathVariable("softwareModuleId") final Long softwareModuleId,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) final int pagingOffsetParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) final int pagingLimitParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false) final String sortParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false) final String rsqlParam) {

        // check if software module exists otherwise throw exception immediately
        findSoftwareModuleWithExceptionIfNotFound(softwareModuleId, null);

        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeSoftwareModuleMetadataSortParam(sortParam);

        final Pageable pageable = new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting);
        final Page<SoftwareModuleMetadata> metaDataPage;

        if (rsqlParam != null) {
            metaDataPage = softwareManagement.findSoftwareModuleMetadataBySoftwareModuleId(softwareModuleId, rsqlParam,
                    pageable);
        } else {
            metaDataPage = softwareManagement.findSoftwareModuleMetadataBySoftwareModuleId(softwareModuleId, pageable);
        }

        return ResponseEntity
                .ok(new PagedList<>(toResponseSwMetadata(metaDataPage.getContent()), metaDataPage.getTotalElements()));
    }

    @Override
    public ResponseEntity<MgmtMetadata> getMetadataValue(@PathVariable("softwareModuleId") final Long softwareModuleId,
            @PathVariable("metadataKey") final String metadataKey) {

        final SoftwareModule sw = findSoftwareModuleWithExceptionIfNotFound(softwareModuleId, null);
        final SoftwareModuleMetadata findOne = softwareManagement.findSoftwareModuleMetadata(sw.getId(), metadataKey);

        return ResponseEntity.ok(toResponseSwMetadata(findOne));
    }

    @Override
    public ResponseEntity<MgmtMetadata> updateMetadata(@PathVariable("softwareModuleId") final Long softwareModuleId,
            @PathVariable("metadataKey") final String metadataKey, @RequestBody final MgmtMetadata metadata) {

        final SoftwareModule sw = findSoftwareModuleWithExceptionIfNotFound(softwareModuleId, null);
        final SoftwareModuleMetadata updated = softwareManagement.updateSoftwareModuleMetadata(
                entityFactory.generateSoftwareModuleMetadata(sw, metadataKey, metadata.getValue()));

        return ResponseEntity.ok(toResponseSwMetadata(updated));
    }

    @Override
    public ResponseEntity<Void> deleteMetadata(@PathVariable("softwareModuleId") final Long softwareModuleId,
            @PathVariable("metadataKey") final String metadataKey) {

        final SoftwareModule sw = findSoftwareModuleWithExceptionIfNotFound(softwareModuleId, null);
        softwareManagement.deleteSoftwareModuleMetadata(sw.getId(), metadataKey);

        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<List<MgmtMetadata>> createMetadata(
            @PathVariable("softwareModuleId") final Long softwareModuleId,
            @RequestBody final List<MgmtMetadata> metadataRest) {

        final SoftwareModule sw = findSoftwareModuleWithExceptionIfNotFound(softwareModuleId, null);
        final List<SoftwareModuleMetadata> created = softwareManagement.createSoftwareModuleMetadata(
                MgmtSoftwareModuleMapper.fromRequestSwMetadata(entityFactory, sw, metadataRest));

        return ResponseEntity.status(CREATED).body(toResponseSwMetadata(created));
    }

    private SoftwareModule findSoftwareModuleWithExceptionIfNotFound(final Long softwareModuleId,
            final Long artifactId) {

        final SoftwareModule module = softwareManagement.findSoftwareModuleById(softwareModuleId);
        if (module == null) {
            throw new EntityNotFoundException("SoftwareModule with Id {" + softwareModuleId + "} does not exist");
        }
        if (artifactId != null && !module.getArtifact(artifactId).isPresent()) {
            throw new EntityNotFoundException("Artifact with Id {" + artifactId + "} does not exist");
        }

        return module;
    }
}
