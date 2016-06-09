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
 *
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

        return new ResponseEntity<>(MgmtSoftwareModuleMapper.toResponse(result), HttpStatus.CREATED);

    }

    @Override
    @ResponseBody
    public ResponseEntity<List<MgmtArtifact>> getArtifacts(
            @PathVariable("softwareModuleId") final Long softwareModuleId) {
        final SoftwareModule module = findSoftwareModuleWithExceptionIfNotFound(softwareModuleId, null);

        return new ResponseEntity<>(MgmtSoftwareModuleMapper.artifactsToResponse(module.getArtifacts()), HttpStatus.OK);
    }

    @Override
    @ResponseBody
    public ResponseEntity<MgmtArtifact> getArtifact(@PathVariable("softwareModuleId") final Long softwareModuleId,
            @PathVariable("artifactId") final Long artifactId) {
        final SoftwareModule module = findSoftwareModuleWithExceptionIfNotFound(softwareModuleId, artifactId);

        return new ResponseEntity<>(MgmtSoftwareModuleMapper.toResponse(module.getLocalArtifact(artifactId).get()),
                HttpStatus.OK);
    }

    @Override
    @ResponseBody
    public ResponseEntity<Void> deleteArtifact(@PathVariable("softwareModuleId") final Long softwareModuleId,
            @PathVariable("artifactId") final Long artifactId) {
        findSoftwareModuleWithExceptionIfNotFound(softwareModuleId, artifactId);

        artifactManagement.deleteLocalArtifact(artifactId);

        return new ResponseEntity<>(HttpStatus.OK);

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
        return new ResponseEntity<>(new PagedList<>(rest, countModulesAll), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<MgmtSoftwareModule> getSoftwareModule(
            @PathVariable("softwareModuleId") final Long softwareModuleId) {
        final SoftwareModule findBaseSoftareModule = findSoftwareModuleWithExceptionIfNotFound(softwareModuleId, null);

        return new ResponseEntity<>(MgmtSoftwareModuleMapper.toResponse(findBaseSoftareModule), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<List<MgmtSoftwareModule>> createSoftwareModules(
            @RequestBody final List<MgmtSoftwareModuleRequestBodyPost> softwareModules) {
        LOG.debug("creating {} softwareModules", softwareModules.size());
        final Iterable<SoftwareModule> createdSoftwareModules = softwareManagement.createSoftwareModule(
                MgmtSoftwareModuleMapper.smFromRequest(entityFactory, softwareModules, softwareManagement));
        LOG.debug("{} softwareModules created, return status {}", softwareModules.size(), HttpStatus.CREATED);

        return new ResponseEntity<>(MgmtSoftwareModuleMapper.toResponseSoftwareModules(createdSoftwareModules),
                HttpStatus.CREATED);
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
        return new ResponseEntity<>(MgmtSoftwareModuleMapper.toResponse(updateSoftwareModule), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Void> deleteSoftwareModule(@PathVariable("softwareModuleId") final Long softwareModuleId) {
        final SoftwareModule module = findSoftwareModuleWithExceptionIfNotFound(softwareModuleId, null);

        softwareManagement.deleteSoftwareModule(module);

        return new ResponseEntity<>(HttpStatus.OK);
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

        return new ResponseEntity<>(
                new PagedList<>(MgmtSoftwareModuleMapper.toResponseSwMetadata(metaDataPage.getContent()),
                        metaDataPage.getTotalElements()),
                HttpStatus.OK);
    }

    @Override
    public ResponseEntity<MgmtMetadata> getMetadataValue(@PathVariable("softwareModuleId") final Long softwareModuleId,
            @PathVariable("metadataKey") final String metadataKey) {
        final SoftwareModule sw = findSoftwareModuleWithExceptionIfNotFound(softwareModuleId, null);
        final SoftwareModuleMetadata findOne = softwareManagement.findSoftwareModuleMetadata(sw, metadataKey);
        return ResponseEntity.<MgmtMetadata> ok(MgmtSoftwareModuleMapper.toResponseSwMetadata(findOne));
    }

    @Override
    public ResponseEntity<MgmtMetadata> updateMetadata(@PathVariable("softwareModuleId") final Long softwareModuleId,
            @PathVariable("metadataKey") final String metadataKey, @RequestBody final MgmtMetadata metadata) {
        final SoftwareModule sw = findSoftwareModuleWithExceptionIfNotFound(softwareModuleId, null);
        final SoftwareModuleMetadata updated = softwareManagement.updateSoftwareModuleMetadata(
                entityFactory.generateSoftwareModuleMetadata(sw, metadataKey, metadata.getValue()));
        return ResponseEntity.ok(MgmtSoftwareModuleMapper.toResponseSwMetadata(updated));
    }

    @Override
    public ResponseEntity<Void> deleteMetadata(@PathVariable("softwareModuleId") final Long softwareModuleId,
            @PathVariable("metadataKey") final String metadataKey) {
        final SoftwareModule sw = findSoftwareModuleWithExceptionIfNotFound(softwareModuleId, null);
        softwareManagement.deleteSoftwareModuleMetadata(sw, metadataKey);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<List<MgmtMetadata>> createMetadata(
            @PathVariable("softwareModuleId") final Long softwareModuleId,
            @RequestBody final List<MgmtMetadata> metadataRest) {
        final SoftwareModule sw = findSoftwareModuleWithExceptionIfNotFound(softwareModuleId, null);

        final List<SoftwareModuleMetadata> created = softwareManagement.createSoftwareModuleMetadata(
                MgmtSoftwareModuleMapper.fromRequestSwMetadata(entityFactory, sw, metadataRest));

        return new ResponseEntity<>(MgmtSoftwareModuleMapper.toResponseSwMetadata(created), HttpStatus.CREATED);

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
