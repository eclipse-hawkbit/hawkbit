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

import org.eclipse.hawkbit.repository.ArtifactManagement;
import org.eclipse.hawkbit.repository.SoftwareManagement;
import org.eclipse.hawkbit.repository.SoftwareModuleFields;
import org.eclipse.hawkbit.repository.SoftwareModuleMetadataFields;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.model.Artifact;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleMetadata;
import org.eclipse.hawkbit.repository.model.SwMetadataCompositeKey;
import org.eclipse.hawkbit.repository.rsql.RSQLUtility;
import org.eclipse.hawkbit.rest.resource.api.SoftwareModuleRestAPI;
import org.eclipse.hawkbit.rest.resource.model.MetadataRest;
import org.eclipse.hawkbit.rest.resource.model.MetadataRestPageList;
import org.eclipse.hawkbit.rest.resource.model.PagedList;
import org.eclipse.hawkbit.rest.resource.model.artifact.ArtifactRest;
import org.eclipse.hawkbit.rest.resource.model.softwaremodule.SoftwareModuleRequestBodyPost;
import org.eclipse.hawkbit.rest.resource.model.softwaremodule.SoftwareModuleRequestBodyPut;
import org.eclipse.hawkbit.rest.resource.model.softwaremodule.SoftwareModuleRest;
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
import org.springframework.web.multipart.MultipartFile;

/**
 * REST Resource handling for {@link SoftwareModule} and related
 * {@link Artifact} CRUD operations.
 *
 */
@RestController
public class SoftwareModuleResource implements SoftwareModuleRestAPI {
    private static final Logger LOG = LoggerFactory.getLogger(SoftwareModuleResource.class);

    @Autowired
    private ArtifactManagement artifactManagement;

    @Autowired
    private SoftwareManagement softwareManagement;

    @Override
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

    @Override
    public ResponseEntity<List<ArtifactRest>> getArtifacts(@PathVariable final Long softwareModuleId) {
        final SoftwareModule module = findSoftwareModuleWithExceptionIfNotFound(softwareModuleId, null);

        return new ResponseEntity<>(SoftwareModuleMapper.artifactsToResponse(module.getArtifacts()), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<ArtifactRest> getArtifact(@PathVariable final Long softwareModuleId,
            @PathVariable final Long artifactId) {
        final SoftwareModule module = findSoftwareModuleWithExceptionIfNotFound(softwareModuleId, artifactId);

        return new ResponseEntity<>(SoftwareModuleMapper.toResponse(module.getLocalArtifact(artifactId).get()),
                HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Void> deleteArtifact(@PathVariable final Long softwareModuleId,
            @PathVariable final Long artifactId) {
        findSoftwareModuleWithExceptionIfNotFound(softwareModuleId, artifactId);

        artifactManagement.deleteLocalArtifact(artifactId);

        return new ResponseEntity<>(HttpStatus.OK);

    }

    @Override
    public ResponseEntity<PagedList<SoftwareModuleRest>> getSoftwareModules(
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
        return new ResponseEntity<>(new PagedList<>(rest, countModulesAll), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<SoftwareModuleRest> getSoftwareModule(@PathVariable final Long softwareModuleId) {
        final SoftwareModule findBaseSoftareModule = findSoftwareModuleWithExceptionIfNotFound(softwareModuleId, null);

        return new ResponseEntity<>(SoftwareModuleMapper.toResponse(findBaseSoftareModule), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<List<SoftwareModuleRest>> createSoftwareModules(
            @RequestBody final List<SoftwareModuleRequestBodyPost> softwareModules) {
        LOG.debug("creating {} softwareModules", softwareModules.size());
        final Iterable<SoftwareModule> createdSoftwareModules = softwareManagement
                .createSoftwareModule(SoftwareModuleMapper.smFromRequest(softwareModules, softwareManagement));
        LOG.debug("{} softwareModules created, return status {}", softwareModules.size(), HttpStatus.CREATED);

        return new ResponseEntity<>(SoftwareModuleMapper.toResponseSoftwareModules(createdSoftwareModules),
                HttpStatus.CREATED);
    }

    @Override
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

    @Override
    public ResponseEntity<Void> deleteSoftwareModule(@PathVariable final Long softwareModuleId) {
        final SoftwareModule module = findSoftwareModuleWithExceptionIfNotFound(softwareModuleId, null);

        softwareManagement.deleteSoftwareModule(module);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
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

    @Override
    public ResponseEntity<MetadataRest> getMetadataValue(@PathVariable final Long softwareModuleId,
            @PathVariable final String metadataKey) {
        // check if distribution set exists otherwise throw exception
        // immediately
        final SoftwareModule sw = findSoftwareModuleWithExceptionIfNotFound(softwareModuleId, null);
        final SoftwareModuleMetadata findOne = softwareManagement.findSoftwareModuleMetadata(new SwMetadataCompositeKey(sw, metadataKey));
        return ResponseEntity.<MetadataRest> ok(SoftwareModuleMapper.toResponseSwMetadata(findOne));
    }

    @Override
    public ResponseEntity<MetadataRest> updateMetadata(@PathVariable final Long softwareModuleId,
            @PathVariable final String metadataKey, @RequestBody final MetadataRest metadata) {
        // check if software module exists otherwise throw exception immediately
        final SoftwareModule sw = findSoftwareModuleWithExceptionIfNotFound(softwareModuleId, null);
        final SoftwareModuleMetadata updated = softwareManagement
                .updateSoftwareModuleMetadata(new SoftwareModuleMetadata(metadataKey, sw, metadata.getValue()));
        return ResponseEntity.ok(SoftwareModuleMapper.toResponseSwMetadata(updated));
    }

    @Override
    public ResponseEntity<Void> deleteMetadata(@PathVariable final Long softwareModuleId,
            @PathVariable final String metadataKey) {
        // check if software module exists otherwise throw exception immediately
        final SoftwareModule sw = findSoftwareModuleWithExceptionIfNotFound(softwareModuleId, null);
        softwareManagement.deleteSoftwareModuleMetadata(new SwMetadataCompositeKey(sw, metadataKey));
        return ResponseEntity.ok().build();
    }

    @Override
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
