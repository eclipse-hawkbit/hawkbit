/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.rest.api;

import java.io.InputStream;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 *
 */
@RequestMapping(MgmtRestConstants.SOFTWAREMODULE_V1_REQUEST_MAPPING)
@FunctionalInterface
public interface MgmtDownloadArtifactRestApi {

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
    ResponseEntity<InputStream> downloadArtifact(@PathVariable("softwareModuleId") Long softwareModuleId,
            @PathVariable("artifactId") Long artifactId);

}
