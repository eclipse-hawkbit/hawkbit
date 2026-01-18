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
