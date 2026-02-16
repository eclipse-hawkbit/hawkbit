/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mcp.server.client;

import java.io.Serial;

public class McpAuthenticationException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public McpAuthenticationException(String message) {
        super(message);
    }
}