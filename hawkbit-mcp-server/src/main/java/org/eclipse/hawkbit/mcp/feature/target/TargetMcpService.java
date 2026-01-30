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

import org.eclipse.hawkbit.mgmt.json.model.PagedList;
import org.eclipse.hawkbit.mgmt.json.model.target.MgmtTarget;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtTargetRestApi;
import org.eclipse.hawkbit.sdk.HawkbitClient;
import org.eclipse.hawkbit.sdk.Tenant;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Service;

@Service
public class TargetMcpService {

        private final MgmtTargetRestApi targetApi;

        public TargetMcpService(final HawkbitClient hawkbitClient, final Tenant tenant) {
                this.targetApi = hawkbitClient.mgmtService(MgmtTargetRestApi.class, tenant);
        }

        @McpTool(name = "listTargets", description = """
                        Search for targets (devices) with pagination.
                        Supports FIQL filters.

                        Filter schema:
                        hawkbit://targets/filter
                        """)
        PagedList<MgmtTarget> getTargets(
                        @McpToolParam(description = "FIQL filter expression. Example: updatestatus==ERROR", required = false) String rsqlParam,

                        @McpToolParam(description = "Page offset (zero-based).", required = true) int offset,

                        @McpToolParam(description = "Page size (max 50).", required = true) int size,

                        @McpToolParam(description = "Sort parameter. Example: name:asc.", required = false) String sortParam) {
                return targetApi.getTargets(rsqlParam, offset, size, sortParam).getBody();
        }

}
