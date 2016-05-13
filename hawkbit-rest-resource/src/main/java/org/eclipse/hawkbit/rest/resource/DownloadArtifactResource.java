/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.rest.resource;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.hawkbit.artifact.repository.model.DbArtifact;
import org.eclipse.hawkbit.repository.ArtifactManagement;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.SoftwareManagement;
import org.eclipse.hawkbit.repository.model.LocalArtifact;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.rest.resource.helper.RestResourceConversionHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Jonathan Knoblauch
 *
 */
@RestController
@RequestMapping(RestConstants.SOFTWAREMODULE_V1_REQUEST_MAPPING)
public class DownloadArtifactResource {

    @Autowired
    private SoftwareManagement softwareManagement;

    @Autowired
    private ArtifactManagement artifactManagement;

    /**
     * Handles the GET request for downloading an artifact.
     *
     * @param softwareModuleId
     *            of the parent SoftwareModule
     * @param artifactId
     *            of the related LocalArtifact
     * @param servletResponse
     *            of the servlet
     * @param request
     *            of the client
     *
     * @return responseEntity with status ok if successful
     */
    @RequestMapping(method = RequestMethod.GET, value = "/{softwareModuleId}/artifacts/{artifactId}/download")
    @ResponseBody
    public ResponseEntity<Void> downloadArtifact(@PathVariable("softwareModuleId") final Long softwareModuleId,
            @PathVariable("artifactId") final Long artifactId, final HttpServletResponse servletResponse,
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
