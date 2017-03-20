/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.rest.resource;

import java.io.InputStream;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.hawkbit.artifact.repository.model.DbArtifact;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtDownloadArtifactRestApi;
import org.eclipse.hawkbit.repository.ArtifactManagement;
import org.eclipse.hawkbit.repository.SoftwareManagement;
import org.eclipse.hawkbit.repository.exception.ArtifactBinaryNotFoundException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.model.Artifact;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.rest.util.RequestResponseContextHolder;
import org.eclipse.hawkbit.rest.util.RestResourceConversionHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

/**
 *
 */
@RestController
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class MgmtDownloadArtifactResource implements MgmtDownloadArtifactRestApi {

    @Autowired
    private SoftwareManagement softwareManagement;

    @Autowired
    private ArtifactManagement artifactManagement;

    @Autowired
    private RequestResponseContextHolder requestResponseContextHolder;

    /**
     * Handles the GET request for downloading an artifact.
     *
     * @param softwareModuleId
     *            of the parent SoftwareModule
     * @param artifactId
     *            of the related Artifact
     *
     * @return responseEntity with status ok if successful
     */
    @Override
    @ResponseBody
    public ResponseEntity<InputStream> downloadArtifact(@PathVariable("softwareModuleId") final Long softwareModuleId,
            @PathVariable("artifactId") final Long artifactId) {
        final SoftwareModule module = softwareManagement.findSoftwareModuleById(softwareModuleId)
                .orElseThrow(() -> new EntityNotFoundException(SoftwareModule.class, softwareModuleId));
        final Artifact artifact = module.getArtifact(artifactId)
                .orElseThrow(() -> new EntityNotFoundException(Artifact.class, artifactId));

        final DbArtifact file = artifactManagement.loadArtifactBinary(artifact.getSha1Hash())
                .orElseThrow(() -> new ArtifactBinaryNotFoundException(artifact.getSha1Hash()));
        final HttpServletRequest request = requestResponseContextHolder.getHttpServletRequest();
        final String ifMatch = request.getHeader("If-Match");
        if (ifMatch != null && !RestResourceConversionHelper.matchesHttpHeader(ifMatch, artifact.getSha1Hash())) {
            return new ResponseEntity<>(HttpStatus.PRECONDITION_FAILED);
        }

        return RestResourceConversionHelper.writeFileResponse(artifact,
                requestResponseContextHolder.getHttpServletResponse(), request, file);

    }

}
