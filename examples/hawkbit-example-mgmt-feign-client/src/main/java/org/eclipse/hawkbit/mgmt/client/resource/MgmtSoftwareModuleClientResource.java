/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.client.resource;

import org.eclipse.hawkbit.mgmt.json.model.artifact.MgmtArtifact;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtSoftwareModuleRestApi;
import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import feign.Param;

/**
 * Client binding for the SoftwareModule resource of the management API.
 */
@FeignClient(name = "MgmtSoftwareModuleClient", url = "${hawkbit.url:localhost:8080}")
public interface MgmtSoftwareModuleClientResource extends MgmtSoftwareModuleRestApi {

    @Override
    @RequestMapping(method = RequestMethod.POST, value = "/{softwareModuleId}/artifacts")
    ResponseEntity<MgmtArtifact> uploadArtifact(@PathVariable("softwareModuleId") final Long softwareModuleId,
            @Param("file") final MultipartFile file,
            @RequestParam(value = "filename", required = false) final String optionalFileName,
            @RequestParam(value = "md5sum", required = false) final String md5Sum,
            @RequestParam(value = "sha1sum", required = false) final String sha1Sum);
}
