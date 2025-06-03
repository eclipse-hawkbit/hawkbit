/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.rest.resource;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.validation.ValidationException;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.artifact.repository.urlhandler.ArtifactUrlHandler;
import org.eclipse.hawkbit.audit.AuditLog;
import org.eclipse.hawkbit.mgmt.json.model.PagedList;
import org.eclipse.hawkbit.mgmt.json.model.artifact.MgmtArtifact;
import org.eclipse.hawkbit.mgmt.json.model.softwaremodule.MgmtSoftwareModule;
import org.eclipse.hawkbit.mgmt.json.model.softwaremodule.MgmtSoftwareModuleMetadata;
import org.eclipse.hawkbit.mgmt.json.model.softwaremodule.MgmtSoftwareModuleMetadataBodyPut;
import org.eclipse.hawkbit.mgmt.json.model.softwaremodule.MgmtSoftwareModuleRequestBodyPost;
import org.eclipse.hawkbit.mgmt.json.model.softwaremodule.MgmtSoftwareModuleRequestBodyPut;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRepresentationMode;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtSoftwareModuleRestApi;
import org.eclipse.hawkbit.mgmt.rest.resource.util.PagingUtility;
import org.eclipse.hawkbit.repository.ArtifactManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.OffsetBasedPageRequest;
import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.repository.SoftwareModuleTypeManagement;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.model.Artifact;
import org.eclipse.hawkbit.repository.model.ArtifactUpload;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleMetadata;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.rest.json.model.ResponseList;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST Resource handling for {@link SoftwareModule} and related {@link Artifact} CRUD operations.
 */
@Slf4j
@RestController
public class MgmtSoftwareModuleResource implements MgmtSoftwareModuleRestApi {

    private final ArtifactManagement artifactManagement;
    private final SoftwareModuleManagement softwareModuleManagement;
    private final SoftwareModuleTypeManagement softwareModuleTypeManagement;
    private final ArtifactUrlHandler artifactUrlHandler;
    private final SystemManagement systemManagement;
    private final EntityFactory entityFactory;

    MgmtSoftwareModuleResource(
            final ArtifactManagement artifactManagement, final SoftwareModuleManagement softwareModuleManagement,
            final SoftwareModuleTypeManagement softwareModuleTypeManagement,
            final ArtifactUrlHandler artifactUrlHandler, final SystemManagement systemManagement,
            final EntityFactory entityFactory) {
        this.artifactManagement = artifactManagement;
        this.softwareModuleManagement = softwareModuleManagement;
        this.softwareModuleTypeManagement = softwareModuleTypeManagement;
        this.artifactUrlHandler = artifactUrlHandler;
        this.systemManagement = systemManagement;
        this.entityFactory = entityFactory;
    }

    @Override
    public ResponseEntity<MgmtArtifact> uploadArtifact(
            final Long softwareModuleId, final MultipartFile file, final String optionalFileName,
            final String md5Sum, final String sha1Sum, final String sha256Sum) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        String fileName = optionalFileName;

        if (fileName == null) {
            fileName = file.getOriginalFilename();
        }

        try (final InputStream in = file.getInputStream()) {
            final Artifact result = artifactManagement.create(new ArtifactUpload(in, softwareModuleId, fileName,
                    md5Sum == null ? null : md5Sum.toLowerCase(), sha1Sum == null ? null : sha1Sum.toLowerCase(),
                    sha256Sum == null ? null : sha256Sum.toLowerCase(), false, file.getContentType(), file.getSize()));

            final MgmtArtifact reponse = MgmtSoftwareModuleMapper.toResponse(result);
            MgmtSoftwareModuleMapper.addLinks(result, reponse);

            return ResponseEntity.status(HttpStatus.CREATED).body(reponse);
        } catch (final IOException e) {
            log.error("Failed to store artifact", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<List<MgmtArtifact>> getArtifacts(
            final Long softwareModuleId, final String representationModeParam, final Boolean useArtifactUrlHandler) {
        final SoftwareModule module = findSoftwareModuleWithExceptionIfNotFound(softwareModuleId, null);

        final boolean isFullMode = parseRepresentationMode(representationModeParam) == MgmtRepresentationMode.FULL;

        final List<MgmtArtifact> response = module.getArtifacts().stream().map(artifact -> {
            final MgmtArtifact mgmtArtifact = MgmtSoftwareModuleMapper.toResponse(artifact);
            if (isFullMode && !module.isDeleted() && Boolean.TRUE.equals(useArtifactUrlHandler)) {
                MgmtSoftwareModuleMapper.addLinks(artifact, mgmtArtifact, artifactUrlHandler, systemManagement);
            } else if (isFullMode && !module.isDeleted()) {
                MgmtSoftwareModuleMapper.addLinks(artifact, mgmtArtifact);
            }
            return mgmtArtifact;
        }).toList();
        return ResponseEntity.ok(new ResponseList<>(response));
    }

    // Exception squid:S3655 - Optional access is checked in findSoftwareModuleWithExceptionIfNotFound subroutine
    @SuppressWarnings("squid:S3655")
    @Override
    public ResponseEntity<MgmtArtifact> getArtifact(final Long softwareModuleId, final Long artifactId, final Boolean useArtifactUrlHandler) {
        final SoftwareModule module = findSoftwareModuleWithExceptionIfNotFound(softwareModuleId, artifactId);

        final MgmtArtifact response = MgmtSoftwareModuleMapper.toResponse(module.getArtifact(artifactId).get());
        if (!module.isDeleted()) {
            if (Boolean.TRUE.equals(useArtifactUrlHandler)) {
                MgmtSoftwareModuleMapper.addLinks(module.getArtifact(artifactId).get(), response, artifactUrlHandler,
                        systemManagement);
            } else {
                MgmtSoftwareModuleMapper.addLinks(module.getArtifact(artifactId).get(), response);
            }
        }

        return ResponseEntity.ok(response);
    }

    @Override
    @AuditLog(entity = "SoftwareModule", type = AuditLog.Type.DELETE, description = "Delete Software Module Artifact")
    public ResponseEntity<Void> deleteArtifact(final Long softwareModuleId, final Long artifactId) {
        findSoftwareModuleWithExceptionIfNotFound(softwareModuleId, artifactId);
        artifactManagement.delete(artifactId);

        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<PagedList<MgmtSoftwareModule>> getSoftwareModules(
            final int pagingOffsetParam, final int pagingLimitParam, final String sortParam, final String rsqlParam) {
        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeSoftwareModuleSortParam(sortParam);

        final Pageable pageable = new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting);

        final Slice<SoftwareModule> findModulesAll;
        final long countModulesAll;
        if (rsqlParam != null) {
            findModulesAll = softwareModuleManagement.findByRsql(rsqlParam, pageable);
            countModulesAll = ((Page<SoftwareModule>) findModulesAll).getTotalElements();
        } else {
            findModulesAll = softwareModuleManagement.findAll(pageable);
            countModulesAll = softwareModuleManagement.count();
        }

        final List<MgmtSoftwareModule> rest = MgmtSoftwareModuleMapper.toResponse(findModulesAll.getContent());
        return ResponseEntity.ok(new PagedList<>(rest, countModulesAll));
    }

    @Override
    public ResponseEntity<MgmtSoftwareModule> getSoftwareModule(final Long softwareModuleId) {
        final SoftwareModule module = findSoftwareModuleWithExceptionIfNotFound(softwareModuleId, null);

        final MgmtSoftwareModule response = MgmtSoftwareModuleMapper.toResponse(module);
        MgmtSoftwareModuleMapper.addLinks(module, response);

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<List<MgmtSoftwareModule>> createSoftwareModules(final List<MgmtSoftwareModuleRequestBodyPost> softwareModules) {
        log.debug("creating {} softwareModules", softwareModules.size());

        for (final MgmtSoftwareModuleRequestBodyPost sm : softwareModules) {
            final Optional<SoftwareModuleType> opt = softwareModuleTypeManagement.findByKey(sm.getType());
            opt.ifPresent(smType -> {
                if (smType.isDeleted()) {
                    final String text = "Cannot create Software Module from type with key {0}. Software Module Type already deleted!";
                    final String message = MessageFormat.format(text, smType.getKey());
                    throw new ValidationException(message);
                }
            });
        }
        final Collection<SoftwareModule> createdSoftwareModules = softwareModuleManagement
                .create(MgmtSoftwareModuleMapper.smFromRequest(entityFactory, softwareModules));
        log.debug("{} softwareModules created, return status {}", softwareModules.size(), HttpStatus.CREATED);

        return ResponseEntity.status(HttpStatus.CREATED).body(MgmtSoftwareModuleMapper.toResponse(createdSoftwareModules));
    }

    @Override
    public ResponseEntity<MgmtSoftwareModule> updateSoftwareModule(
            final Long softwareModuleId, final MgmtSoftwareModuleRequestBodyPut restSoftwareModule) {
        final SoftwareModule module = softwareModuleManagement
                .update(entityFactory.softwareModule().update(softwareModuleId)
                        .description(restSoftwareModule.getDescription())
                        .vendor(restSoftwareModule.getVendor())
                        .locked(restSoftwareModule.getLocked()));

        final MgmtSoftwareModule response = MgmtSoftwareModuleMapper.toResponse(module);
        MgmtSoftwareModuleMapper.addLinks(module, response);

        return ResponseEntity.ok(response);
    }

    @Override
    @AuditLog(entity = "SoftwareModule", type = AuditLog.Type.DELETE, description = "Delete Software Module")
    public ResponseEntity<Void> deleteSoftwareModule(final Long softwareModuleId) {
        final SoftwareModule module = findSoftwareModuleWithExceptionIfNotFound(softwareModuleId, null);
        softwareModuleManagement.delete(module.getId());

        return ResponseEntity.ok().build();
    }

    @Override
    public void createMetadata(final Long softwareModuleId, final List<MgmtSoftwareModuleMetadata> metadataRest) {
        softwareModuleManagement.createMetadata(MgmtSoftwareModuleMapper.fromRequestSwMetadata(entityFactory, softwareModuleId, metadataRest));
    }

    @Override
    public ResponseEntity<PagedList<MgmtSoftwareModuleMetadata>> getMetadata(final Long softwareModuleId) {
        // check if software module exists otherwise throw exception immediately
        findSoftwareModuleWithExceptionIfNotFound(softwareModuleId, null);

        final List<SoftwareModuleMetadata> metadata = softwareModuleManagement.getMetadata(softwareModuleId);
        return ResponseEntity.ok(new PagedList<>(MgmtSoftwareModuleMapper.toResponseSwMetadata(metadata), metadata.size()));
    }

    @Override
    public ResponseEntity<MgmtSoftwareModuleMetadata> getMetadataValue(final Long softwareModuleId, final String metadataKey) {
        final SoftwareModuleMetadata findOne = softwareModuleManagement.getMetadata(softwareModuleId).stream()
                .filter(entry -> entry.getKey().equals(metadataKey))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("SoftwareModule metadata", softwareModuleId + ":" + metadataKey));

        return ResponseEntity.ok(MgmtSoftwareModuleMapper.toResponseSwMetadata(findOne));
    }

    @Override
    public void updateMetadata(final Long softwareModuleId, final String metadataKey, final MgmtSoftwareModuleMetadataBodyPut metadata) {
        softwareModuleManagement.updateMetadata(
                entityFactory.softwareModuleMetadata()
                        .update(softwareModuleId, metadataKey)
                        .value(metadata.getValue())
                        .targetVisible(metadata.getTargetVisible()));
    }

    @Override
    @AuditLog(entity = "SoftwareModule", type = AuditLog.Type.DELETE, description = "Delete Software Module Metadata")
    public void deleteMetadata(final Long softwareModuleId, final String metadataKey) {
        softwareModuleManagement.deleteMetadata(softwareModuleId, metadataKey);
    }

    private static MgmtRepresentationMode parseRepresentationMode(final String representationModeParam) {
        return MgmtRepresentationMode.fromValue(representationModeParam).orElseGet(() -> {
            // no need for a 400, just apply a safe fallback
            log.warn("Received an invalid representation mode: {}", representationModeParam);
            return MgmtRepresentationMode.COMPACT;
        });
    }

    private SoftwareModule findSoftwareModuleWithExceptionIfNotFound(final Long softwareModuleId, final Long artifactId) {
        final SoftwareModule module = softwareModuleManagement.get(softwareModuleId)
                .orElseThrow(() -> new EntityNotFoundException(SoftwareModule.class, softwareModuleId));

        if (artifactId != null && module.getArtifact(artifactId).isEmpty()) {
            throw new EntityNotFoundException(Artifact.class, artifactId);
        }

        return module;
    }
}