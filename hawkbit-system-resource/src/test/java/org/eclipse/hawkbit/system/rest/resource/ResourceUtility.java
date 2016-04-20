/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.system.rest.resource;

import java.io.IOException;

import org.eclipse.hawkbit.rest.json.model.ExceptionInfo;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Utility additions for the REST API tests.
 *
 *
 */
public final class ResourceUtility {
    private static final ObjectMapper mapper = new ObjectMapper();

    static ExceptionInfo convertException(final String jsonExceptionResponse)
            throws JsonParseException, JsonMappingException, IOException {
        return mapper.readValue(jsonExceptionResponse, ExceptionInfo.class);
    }

}
