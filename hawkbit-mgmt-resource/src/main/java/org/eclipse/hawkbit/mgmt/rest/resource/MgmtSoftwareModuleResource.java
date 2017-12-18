/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.rest.resource;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.eclipse.hawkbit.mgmt.json.model.PagedList;
import org.eclipse.hawkbit.mgmt.json.model.artifact.MgmtArtifact;
import org.eclipse.hawkbit.mgmt.json.model.softwaremodule.MgmtSoftwareModule;
import org.eclipse.hawkbit.mgmt.json.model.softwaremodule.MgmtSoftwareModuleMetadata;
import org.eclipse.hawkbit.mgmt.json.model.softwaremodule.MgmtSoftwareModuleMetadataBodyPut;
import org.eclipse.hawkbit.mgmt.json.model.softwaremodule.MgmtSoftwareModuleRequestBodyPost;
import org.eclipse.hawkbit.mgmt.json.model.softwaremodule.MgmtSoftwareModuleRequestBodyPut;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtSoftwareModuleRestApi;
import org.eclipse.hawkbit.repository.ArtifactManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.OffsetBasedPageRequest;
import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
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
    private SoftwareModuleManagement softwareModuleManagement;

    @Autowired
    private EntityFactory entityFactory;

    @Override
    public ResponseEntity<MgmtArtifact> uploadArtifact(@PathVariable("softwareModuleId") final Long softwareModuleId,
            @RequestParam("file") final MultipartFile file,
            @RequestParam(value = "filename", required = false) final String optionalFileName,
            @RequestParam(value = "md5sum", required = false) final String md5Sum,
            @RequestParam(value = "sha1sum", required = false) final String sha1Sum) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        String fileName = optionalFileName;

        if (fileName == null) {
            fileName = file.getOriginalFilename();
        }

        try {
            final Artifact result = artifactManagement.create(file.getInputStream(), softwareModuleId, fileName,
                    md5Sum == null ? null : md5Sum.toLowerCase(), sha1Sum == null ? null : sha1Sum.toLowerCase(), false,
                    file.getContentType());

            final MgmtArtifact reponse = MgmtSoftwareModuleMapper.toResponse(result);
            MgmtSoftwareModuleMapper.addLinks(result, reponse);

            return ResponseEntity.status(HttpStatus.CREATED).body(reponse);
        } catch (final IOException e) {
            LOG.error("Failed to store artifact", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<List<MgmtArtifact>> getArtifacts(
            @PathVariable("softwareModuleId") final Long softwareModuleId) {

        final SoftwareModule module = findSoftwareModuleWithExceptionIfNotFound(softwareModuleId, null);
        return ResponseEntity.ok(MgmtSoftwareModuleMapper.artifactsToResponse(module.getArtifacts()));
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

        final MgmtArtifact reponse = MgmtSoftwareModuleMapper.toResponse(module.getArtifact(artifactId).get());
        MgmtSoftwareModuleMapper.addLinks(module.getArtifact(artifactId).get(), reponse);

        return ResponseEntity.ok(reponse);
    }

    @Override
    @ResponseBody
    public ResponseEntity<Void> deleteArtifact(@PathVariable("softwareModuleId") final Long softwareModuleId,
            @PathVariable("artifactId") final Long artifactId) {

        findSoftwareModuleWithExceptionIfNotFound(softwareModuleId, artifactId);
        artifactManagement.delete(artifactId);

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
        long countModulesAll;
        if (rsqlParam != null) {
            findModulesAll = softwareModuleManagement.findByRsql(pageable, rsqlParam);
            countModulesAll = ((Page<SoftwareModule>) findModulesAll).getTotalElements();
        } else {
            findModulesAll = softwareModuleManagement.findAll(pageable);
            countModulesAll = softwareModuleManagement.count();
        }

        final List<MgmtSoftwareModule> rest = MgmtSoftwareModuleMapper.toResponse(findModulesAll.getContent());
        return ResponseEntity.ok(new PagedList<>(rest, countModulesAll));
    }

    @Override
    public ResponseEntity<MgmtSoftwareModule> getSoftwareModule(
            @PathVariable("softwareModuleId") final Long softwareModuleId) {

        final SoftwareModule module = findSoftwareModuleWithExceptionIfNotFound(softwareModuleId, null);

        final MgmtSoftwareModule response = MgmtSoftwareModuleMapper.toResponse(module);
        MgmtSoftwareModuleMapper.addLinks(module, response);

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<List<MgmtSoftwareModule>> createSoftwareModules(
            @RequestBody final List<MgmtSoftwareModuleRequestBodyPost> softwareModules) {

        LOG.debug("creating {} softwareModules", softwareModules.size());
        final Collection<SoftwareModule> createdSoftwareModules = softwareModuleManagement
                .create(MgmtSoftwareModuleMapper.smFromRequest(entityFactory, softwareModules));
        LOG.debug("{} softwareModules created, return status {}", softwareModules.size(), HttpStatus.CREATED);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(MgmtSoftwareModuleMapper.toResponse(createdSoftwareModules));
    }

    @Override
    public ResponseEntity<MgmtSoftwareModule> updateSoftwareModule(
            @PathVariable("softwareModuleId") final Long softwareModuleId,
            @RequestBody final MgmtSoftwareModuleRequestBodyPut restSoftwareModule) {
        final SoftwareModule module = softwareModuleManagement
                .update(entityFactory.softwareModule().update(softwareModuleId)
                        .description(restSoftwareModule.getDescription()).vendor(restSoftwareModule.getVendor()));

        final MgmtSoftwareModule response = MgmtSoftwareModuleMapper.toResponse(module);
        MgmtSoftwareModuleMapper.addLinks(module, response);

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<Void> deleteSoftwareModule(@PathVariable("softwareModuleId") final Long softwareModuleId) {

        final SoftwareModule module = findSoftwareModuleWithExceptionIfNotFound(softwareModuleId, null);
        softwareModuleManagement.delete(module.getId());

        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<PagedList<MgmtSoftwareModuleMetadata>> getMetadata(
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
            metaDataPage = softwareModuleManagement.findMetaDataByRsql(pageable, softwareModuleId, rsqlParam);
        } else {
            metaDataPage = softwareModuleManagement.findMetaDataBySoftwareModuleId(pageable, softwareModuleId);
        }

        return ResponseEntity
                .ok(new PagedList<>(MgmtSoftwareModuleMapper.toResponseSwMetadata(metaDataPage.getContent()),
                        metaDataPage.getTotalElements()));
    }

    @Override
    public ResponseEntity<MgmtSoftwareModuleMetadata> getMetadataValue(
            @PathVariable("softwareModuleId") final Long softwareModuleId,
            @PathVariable("metadataKey") final String metadataKey) {

        final SoftwareModuleMetadata findOne = softwareModuleManagement
                .getMetaDataBySoftwareModuleId(softwareModuleId, metadataKey).orElseThrow(
                        () -> new EntityNotFoundException(SoftwareModuleMetadata.class, softwareModuleId, metadataKey));

        return ResponseEntity.ok(MgmtSoftwareModuleMapper.toResponseSwMetadata(findOne));
    }

    @Override
    public ResponseEntity<MgmtSoftwareModuleMetadata> updateMetadata(
            @PathVariable("softwareModuleId") final Long softwareModuleId,
            @PathVariable("metadataKey") final String metadataKey,
            @RequestBody final MgmtSoftwareModuleMetadataBodyPut metadata) {
        final SoftwareModuleMetadata updated = softwareModuleManagement
                .updateMetaData(entityFactory.softwareModuleMetadata().update(softwareModuleId, metadataKey)
                        .value(metadata.getValue()).targetVisible(metadata.isTargetVisible()));

        return ResponseEntity.ok(MgmtSoftwareModuleMapper.toResponseSwMetadata(updated));
    }

    @Override
    public ResponseEntity<Void> deleteMetadata(@PathVariable("softwareModuleId") final Long softwareModuleId,
            @PathVariable("metadataKey") final String metadataKey) {
        softwareModuleManagement.deleteMetaData(softwareModuleId, metadataKey);

        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<List<MgmtSoftwareModuleMetadata>> createMetadata(
            @PathVariable("softwareModuleId") final Long softwareModuleId,
            @RequestBody final List<MgmtSoftwareModuleMetadata> metadataRest) {

        final List<SoftwareModuleMetadata> created = softwareModuleManagement.createMetaData(
                MgmtSoftwareModuleMapper.fromRequestSwMetadata(entityFactory, softwareModuleId, metadataRest));

        return ResponseEntity.status(HttpStatus.CREATED).body(MgmtSoftwareModuleMapper.toResponseSwMetadata(created));
    }

    private SoftwareModule findSoftwareModuleWithExceptionIfNotFound(final Long softwareModuleId,
            final Long artifactId) {

        final SoftwareModule module = softwareModuleManagement.get(softwareModuleId)
                .orElseThrow(() -> new EntityNotFoundException(SoftwareModule.class, softwareModuleId));

        if (artifactId != null && !module.getArtifact(artifactId).isPresent()) {
            throw new EntityNotFoundException(Artifact.class, artifactId);
        }

        return module;
    }
}
