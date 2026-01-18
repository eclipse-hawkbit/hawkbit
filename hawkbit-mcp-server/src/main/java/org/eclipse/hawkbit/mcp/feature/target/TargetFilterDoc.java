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
