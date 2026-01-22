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

import static org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants.DOWNLOAD_ARTIFACT_ORDER;
import static org.eclipse.hawkbit.rest.ApiResponsesConstants.GetResponses;
import static org.eclipse.hawkbit.rest.ApiResponsesConstants.INTERNAL_SERVER_ERROR_500;

import java.io.InputStream;

import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.eclipse.hawkbit.rest.OpenApi;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * API to download artifacts
 */
@FunctionalInterface
@Tag(
        name = "Download artifact", description = "API to download artifacts.",
        extensions = @Extension(name = OpenApi.X_HAWKBIT, properties = @ExtensionProperty(name = "order", value = DOWNLOAD_ARTIFACT_ORDER)))
// no request mapping specified here to avoid CVE-2021-22044 in Feign client
public interface MgmtDownloadArtifactRestApi {

    /**
     * Handles the GET request for downloading an artifact.
     *
     * @param softwareModuleId of the parent SoftwareModule
     * @param artifactId of the related repository Artifact
     * @return responseEntity with status ok if successful
     */
    @GetResponses
    @ApiResponses(value = {
            @ApiResponse(responseCode = INTERNAL_SERVER_ERROR_500, description = "Artifact download or decryption failed",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @GetMapping(value = MgmtRestConstants.SOFTWAREMODULE_V1_REQUEST_MAPPING + "/{softwareModuleId}/artifacts/{artifactId}/download")
    @ResponseBody
    ResponseEntity<InputStream> downloadArtifact(
            @PathVariable("softwareModuleId") Long softwareModuleId,
            @PathVariable("artifactId") Long artifactId);
}