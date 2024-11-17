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

import java.io.InputStream;

import jakarta.servlet.http.HttpServletRequest;

import org.eclipse.hawkbit.artifact.repository.model.DbArtifact;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtDownloadArtifactRestApi;
import org.eclipse.hawkbit.repository.ArtifactManagement;
import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.repository.exception.ArtifactBinaryNoLongerExistsException;
import org.eclipse.hawkbit.repository.exception.ArtifactBinaryNotFoundException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.model.Artifact;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.rest.util.FileStreamingUtil;
import org.eclipse.hawkbit.rest.util.HttpUtil;
import org.eclipse.hawkbit.rest.util.RequestResponseContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

@RestController
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class MgmtDownloadArtifactResource implements MgmtDownloadArtifactRestApi {

    @Autowired
    private SoftwareModuleManagement softwareModuleManagement;
    @Autowired
    private ArtifactManagement artifactManagement;

    /**
     * Handles the GET request for downloading an artifact.
     *
     * @param softwareModuleId of the parent SoftwareModule
     * @param artifactId of the related Artifact
     * @return responseEntity with status ok if successful
     */
    @Override
    public ResponseEntity<InputStream> downloadArtifact(final Long softwareModuleId, final Long artifactId) {
        final SoftwareModule module = softwareModuleManagement.get(softwareModuleId)
                .orElseThrow(() -> new EntityNotFoundException(SoftwareModule.class, softwareModuleId));
        if (module.isDeleted()) {
            throw new ArtifactBinaryNoLongerExistsException();
        }
        final Artifact artifact = module.getArtifact(artifactId)
                .orElseThrow(() -> new EntityNotFoundException(Artifact.class, artifactId));

        final DbArtifact file = artifactManagement
                .loadArtifactBinary(artifact.getSha1Hash(), module.getId(), module.isEncrypted())
                .orElseThrow(() -> new ArtifactBinaryNotFoundException(artifact.getSha1Hash()));
        final HttpServletRequest request = RequestResponseContextHolder.getHttpServletRequest();
        final String ifMatch = request.getHeader(HttpHeaders.IF_MATCH);
        if (ifMatch != null && !HttpUtil.matchesHttpHeader(ifMatch, artifact.getSha1Hash())) {
            return new ResponseEntity<>(HttpStatus.PRECONDITION_FAILED);
        }

        return FileStreamingUtil.writeFileResponse(file, artifact.getFilename(), artifact.getCreatedAt(),
                RequestResponseContextHolder.getHttpServletResponse(), request, null);
    }
}