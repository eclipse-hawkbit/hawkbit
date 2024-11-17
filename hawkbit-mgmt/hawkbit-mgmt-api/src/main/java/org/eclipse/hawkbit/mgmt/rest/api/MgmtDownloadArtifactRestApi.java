/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.rest.api;

import java.io.InputStream;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * API to download artifacts
 */
@FunctionalInterface
@Tag(name = "Download artifact", description = "API to download artifacts.")
// no request mapping specified here to avoid CVE-2021-22044 in Feign client
public interface MgmtDownloadArtifactRestApi {

    /**
     * Handles the GET request for downloading an artifact.
     *
     * @param softwareModuleId of the parent SoftwareModule
     * @param artifactId of the related LocalArtifact
     * @return responseEntity with status ok if successful
     */
    @GetMapping(value = MgmtRestConstants.SOFTWAREMODULE_V1_REQUEST_MAPPING + "/{softwareModuleId}/artifacts/{artifactId}/download")
    @ResponseBody
    ResponseEntity<InputStream> downloadArtifact(@PathVariable("softwareModuleId") Long softwareModuleId,
            @PathVariable("artifactId") Long artifactId);
}