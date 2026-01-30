/*
 * Copyright (c) 2026 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.hawkbit.mcp.feature.target;

import org.springaicommunity.mcp.annotation.McpResource;
import org.springframework.stereotype.Component;

@Component
public class TargetFilterDoc {

    @McpResource(uri = "hawkbit://targets/filter", name = "Target Filter Schema", description = "Canonical schema for target filtering.", mimeType = "text/plain")
    public String getTargetFilterSchema() {
        return TargetFilterSchema.documentation();
    }
}
