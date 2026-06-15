/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.app.mgmt;

import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.webmvc.autoconfigure.error.BasicErrorController;
import org.springframework.boot.webmvc.error.ErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Error page controller that ensures that ocet stream does not return text in
 * case of an error.
 */
@Controller
// Exception squid:S3752 - errors need handling for all methods
@SuppressWarnings("squid:S3752")
public class ErrorController extends BasicErrorController {

    private static final String PATH = "path";

    /**
     * A new {@link ErrorController}.
     *
     * @param errorAttributes the error attributes
     * @param webProperties web configuration properties
     */
    public ErrorController(final ErrorAttributes errorAttributes, final WebProperties webProperties) {
        super(errorAttributes, webProperties.getError());
    }

    @RequestMapping(produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Void> errorStream(final HttpServletRequest request, final HttpServletResponse response) {
        final HttpStatus status = getStatus(request);
        return new ResponseEntity<>(status);
    }

    @Override
    @RequestMapping
    public ResponseEntity<Map<String, Object>> error(final HttpServletRequest request) {
        final HttpStatus status = getStatus(request);
        final Map<String, Object> body = getErrorAttributesWithoutPath(request);
        return new ResponseEntity<>(body, status);
    }

    private Map<String, Object> getErrorAttributesWithoutPath(final HttpServletRequest request) {
        final Map<String, Object> body = getErrorAttributes(request, getErrorAttributeOptions(request, MediaType.ALL));
        if (body != null && body.containsKey(PATH)) {
            body.remove(PATH);
        }
        return body;
    }
}