/**
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ddi.rest.resource;

import static org.eclipse.hawkbit.ddi.rest.resource.AbstractDDiApiIntegrationTest.HTTP_PORT;

import org.jetbrains.annotations.NotNull;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

public class RequestOnHawkbitDefaultPortPostProcessor implements RequestPostProcessor {

    @NotNull
    @Override
    public MockHttpServletRequest postProcessRequest(MockHttpServletRequest request) {
        request.setRemotePort(HTTP_PORT);
        request.setServerPort(HTTP_PORT);
        return request;
    }
}
