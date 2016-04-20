/**
 * Copyright (c) 2011-2016 Bosch Software Innovations GmbH, Germany. All rights reserved.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ddi.rest.api;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.hawkbit.ddi.json.model.DdiArtifact;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.bind.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * REST resource handling for artifact download operations.
 */
@RequestMapping(DdiRestConstants.ARTIFACTS_V1_REQUEST_MAPPING)
public interface DdiArtifactStoreControllerRestApi {

    /**
     * Handles GET {@link DdiArtifact} download request. This could be full or
     * partial download request.
     *
     * @param fileName
     *            to search for
     * @param response
     *            to write to
     * @param request
     *            from the client
     * @param targetid
     *            of authenticated target
     *
     * @return response of the servlet which in case of success is status code
     *         {@link HttpStatus#OK} or in case of partial download
     *         {@link HttpStatus#PARTIAL_CONTENT}.
     */
    @RequestMapping(method = RequestMethod.GET, value = DdiRestConstants.ARTIFACT_DOWNLOAD_BY_FILENAME
            + "/{fileName}")
    @ResponseBody
    public ResponseEntity<Void> downloadArtifactByFilename(@PathVariable("fileName") final String fileName,
            final HttpServletResponse response, final HttpServletRequest request,
            @AuthenticationPrincipal final String targetid);

    /**
     * Handles GET {@link DdiArtifact} MD5 checksum file download request.
     *
     * @param fileName
     *            to search for
     * @param response
     *            to write to
     *
     * @return response of the servlet
     */
    @RequestMapping(method = RequestMethod.GET, value = DdiRestConstants.ARTIFACT_DOWNLOAD_BY_FILENAME
            + "/{fileName}" + DdiRestConstants.ARTIFACT_MD5_DWNL_SUFFIX)
    @ResponseBody
    public ResponseEntity<Void> downloadArtifactMD5ByFilename(@PathVariable("fileName") final String fileName,
            final HttpServletResponse response);

}
