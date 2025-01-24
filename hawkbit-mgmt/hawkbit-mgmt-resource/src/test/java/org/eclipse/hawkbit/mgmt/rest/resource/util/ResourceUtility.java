/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.rest.resource.util;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.hawkbit.mgmt.json.model.artifact.MgmtArtifact;
import org.eclipse.hawkbit.rest.json.model.ExceptionInfo;

/**
 * Utility additions for the REST API tests.
 */
public final class ResourceUtility {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static ExceptionInfo convertException(final String jsonExceptionResponse) throws IOException {
        return OBJECT_MAPPER.readValue(jsonExceptionResponse, ExceptionInfo.class);
    }

    public static MgmtArtifact convertArtifactResponse(final String jsonResponse) throws IOException {
        return OBJECT_MAPPER.readValue(jsonResponse, MgmtArtifact.class);
    }
}