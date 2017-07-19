/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.app;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.boot.autoconfigure.web.BasicErrorController;
import org.springframework.boot.autoconfigure.web.ErrorAttributes;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Error page controller that ensures that ocet stream does not return text in
 * case of an error.
 *
 */
@Controller
public class StreamAwareErrorController extends BasicErrorController {

    /**
     * A new {@link StreamAwareErrorController}.
     * 
     * @param errorAttributes
     *            the error attributes
     * @param serverProperties
     *            configuration properties
     */
    public StreamAwareErrorController(final ErrorAttributes errorAttributes, final ServerProperties serverProperties) {
        super(errorAttributes, serverProperties.getError());
    }

    @RequestMapping(produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Void> errorStream(final HttpServletRequest request, final HttpServletResponse response) {
        final HttpStatus status = getStatus(request);
        return new ResponseEntity<>(status);
    }

}
