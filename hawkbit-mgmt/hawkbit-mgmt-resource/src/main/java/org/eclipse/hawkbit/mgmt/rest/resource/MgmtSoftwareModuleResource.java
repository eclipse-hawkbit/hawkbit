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

import static org.eclipse.hawkbit.mgmt.rest.resource.util.PagingUtility.sanitizeSoftwareModuleSortParam;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.validation.ValidationException;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.artifact.model.ArtifactHashes;
import org.eclipse.hawkbit.artifact.urlresolver.ArtifactUrlResolver;
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
import org.eclipse.hawkbit.mgmt.rest.resource.mapper.MgmtSoftwareModuleMapper;
import org.eclipse.hawkbit.mgmt.rest.resource.util.PagingUtility;
import org.eclipse.hawkbit.repository.ArtifactManagement;
import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.repository.SoftwareModuleTypeManagement;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.model.Artifact;
import org.eclipse.hawkbit.repository.model.ArtifactUpload;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModule.MetadataValue;
import org.eclipse.hawkbit.repository.model.SoftwareModule.MetadataValueCreate;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.rest.json.model.ResponseList;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
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
    private final SoftwareModuleManagement<? extends SoftwareModule> softwareModuleManagement;
    private final SoftwareModuleTypeManagement<? extends SoftwareModuleType> softwareModuleTypeManagement;
    private final ArtifactUrlResolver artifactUrlHandler;
    private final MgmtSoftwareModuleMapper mgmtSoftwareModuleMapper;
    private final SystemManagement systemManagement;

    MgmtSoftwareModuleResource(
            final ArtifactManagement artifactManagement,
            final SoftwareModuleManagement<? extends SoftwareModule> softwareModuleManagement,
            final SoftwareModuleTypeManagement<? extends SoftwareModuleType> softwareModuleTypeManagement,
            final ArtifactUrlResolver artifactUrlHandler,
            final MgmtSoftwareModuleMapper mgmtSoftwareModuleMapper,
            final SystemManagement systemManagement) {
        this.artifactManagement = artifactManagement;
        this.softwareModuleManagement = softwareModuleManagement;
        this.softwareModuleTypeManagement = softwareModuleTypeManagement;
        this.artifactUrlHandler = artifactUrlHandler;
        this.mgmtSoftwareModuleMapper = mgmtSoftwareModuleMapper;
        this.systemManagement = systemManagement;
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
            final Artifact result = artifactManagement.create(new ArtifactUpload(
                    in, file.getContentType(), file.getSize(),
                    new ArtifactHashes(
                            sha1Sum == null ? null : sha1Sum.toLowerCase(),
                            md5Sum == null ? null : md5Sum.toLowerCase(),
                            sha256Sum == null ? null : sha256Sum.toLowerCase()),
                    softwareModuleId, fileName, false));

            final MgmtArtifact response = MgmtSoftwareModuleMapper.toResponse(result);
            MgmtSoftwareModuleMapper.addLinks(result, response);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
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

        final MgmtArtifact response = MgmtSoftwareModuleMapper.toResponse(module.getArtifact(artifactId).orElseThrow());
        if (!module.isDeleted()) {
            if (Boolean.TRUE.equals(useArtifactUrlHandler)) {
                MgmtSoftwareModuleMapper.addLinks(module.getArtifact(artifactId).orElseThrow(), response, artifactUrlHandler, systemManagement);
            } else {
                MgmtSoftwareModuleMapper.addLinks(module.getArtifact(artifactId).orElseThrow(), response);
            }
        }

        return ResponseEntity.ok(response);
    }

    @Override
    @AuditLog(entity = "SoftwareModule", type = AuditLog.Type.DELETE, description = "Delete Software Module Artifact")
    public ResponseEntity<Void> deleteArtifact(final Long softwareModuleId, final Long artifactId) {
        findSoftwareModuleWithExceptionIfNotFound(softwareModuleId, artifactId);
        artifactManagement.delete(artifactId);

        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<PagedList<MgmtSoftwareModule>> getSoftwareModules(
            final String rsqlParam, final int pagingOffsetParam, final int pagingLimitParam, final String sortParam) {
        final Pageable pageable = PagingUtility.toPageable(pagingOffsetParam, pagingLimitParam, sanitizeSoftwareModuleSortParam(sortParam));
        final Slice<? extends SoftwareModule> findModulesAll;
        final long countModulesAll;
        if (rsqlParam != null) {
            findModulesAll = softwareModuleManagement.findByRsql(rsqlParam, pageable);
            countModulesAll = ((Page<?>) findModulesAll).getTotalElements();
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
            final Optional<? extends SoftwareModuleType> opt = softwareModuleTypeManagement.findByKey(sm.getType());
            opt.ifPresent(smType -> {
                if (smType.isDeleted()) {
                    final String text = "Cannot create Software Module from type with key {0}. Software Module Type already deleted!";
                    final String message = MessageFormat.format(text, smType.getKey());
                    throw new ValidationException(message);
                }
            });
        }
        final Collection<? extends SoftwareModule> createdSoftwareModules = softwareModuleManagement
                .create(mgmtSoftwareModuleMapper.smFromRequest(softwareModules));
        log.debug("{} softwareModules created, return status {}", softwareModules.size(), HttpStatus.CREATED);

        return ResponseEntity.status(HttpStatus.CREATED).body(MgmtSoftwareModuleMapper.toResponse(createdSoftwareModules));
    }

    @Override
    public ResponseEntity<MgmtSoftwareModule> updateSoftwareModule(
            final Long softwareModuleId, final MgmtSoftwareModuleRequestBodyPut restSoftwareModule) {
        final SoftwareModule module = softwareModuleManagement
                .update(SoftwareModuleManagement.Update.builder()
                        .id(softwareModuleId)
                        .description(restSoftwareModule.getDescription())
                        .vendor(restSoftwareModule.getVendor())
                        .locked(restSoftwareModule.getLocked())
                        .build());

        final MgmtSoftwareModule response = MgmtSoftwareModuleMapper.toResponse(module);
        MgmtSoftwareModuleMapper.addLinks(module, response);

        return ResponseEntity.ok(response);
    }

    @Override
    @AuditLog(entity = "SoftwareModule", type = AuditLog.Type.DELETE, description = "Delete Software Module")
    public ResponseEntity<Void> deleteSoftwareModule(final Long softwareModuleId) {
        final SoftwareModule module = findSoftwareModuleWithExceptionIfNotFound(softwareModuleId, null);
        softwareModuleManagement.delete(module.getId());

        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> createMetadata(final Long softwareModuleId, final List<MgmtSoftwareModuleMetadata> metadataRest) {
        softwareModuleManagement.createMetadata(softwareModuleId, MgmtSoftwareModuleMapper.fromRequestSwMetadata(metadataRest));
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Override
    public ResponseEntity<PagedList<MgmtSoftwareModuleMetadata>> getMetadata(final Long softwareModuleId) {
        // check if software module exists otherwise throw exception immediately
        findSoftwareModuleWithExceptionIfNotFound(softwareModuleId, null);

        final Map<String, MetadataValue> metadata = softwareModuleManagement.getMetadata(softwareModuleId);
        return ResponseEntity.ok(new PagedList<>(MgmtSoftwareModuleMapper.toResponseSwMetadata(metadata), metadata.size()));
    }

    @Override
    public ResponseEntity<MgmtSoftwareModuleMetadata> getMetadataValue(final Long softwareModuleId, final String metadataKey) {
        final MetadataValue metadataValue = softwareModuleManagement.getMetadata(softwareModuleId).get(metadataKey);
        if (metadataValue == null) {
            throw new EntityNotFoundException("SoftwareModule metadata", softwareModuleId + ":" + metadataKey);
        }

        return ResponseEntity.ok(MgmtSoftwareModuleMapper.toResponseSwMetadata(metadataKey, metadataValue));
    }

    @Override
    public ResponseEntity<Void> updateMetadata(final Long softwareModuleId, final String metadataKey, final MgmtSoftwareModuleMetadataBodyPut metadata) {
        softwareModuleManagement.createMetadata(
                softwareModuleId, metadataKey, new MetadataValueCreate(metadata.getValue(), metadata.getTargetVisible()));
        return ResponseEntity.noContent().build();
    }

    @Override
    @AuditLog(entity = "SoftwareModule", type = AuditLog.Type.DELETE, description = "Delete Software Module Metadata")
    public ResponseEntity<Void> deleteMetadata(final Long softwareModuleId, final String metadataKey) {
        softwareModuleManagement.deleteMetadata(softwareModuleId, metadataKey);
        return ResponseEntity.noContent().build();
    }

    private static MgmtRepresentationMode parseRepresentationMode(final String representationModeParam) {
        return MgmtRepresentationMode.fromValue(representationModeParam).orElseGet(() -> {
            // no need for a 400, just apply a safe fallback
            log.warn("Received an invalid representation mode: {}", representationModeParam);
            return MgmtRepresentationMode.COMPACT;
        });
    }

    private SoftwareModule findSoftwareModuleWithExceptionIfNotFound(final Long softwareModuleId, final Long artifactId) {
        final SoftwareModule module = softwareModuleManagement.find(softwareModuleId)
                .orElseThrow(() -> new EntityNotFoundException(SoftwareModule.class, softwareModuleId));

        if (artifactId != null && module.getArtifact(artifactId).isEmpty()) {
            throw new EntityNotFoundException(Artifact.class, artifactId);
        }

        return module;
    }
}