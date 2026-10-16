# hawkBit MCP Server

A standalone [Model Context Protocol (MCP)](https://modelcontextprotocol.io/) server that provides AI assistants with
tools to interact with [Eclipse hawkBit](https://www.eclipse.org/hawkbit/) for IoT device software update management.

## Building

From the project root directory:

```bash
mvn clean package -pl hawkbit-mcp/hawkbit-mcp-server -am -DskipTests
```

The JAR will be created at: `hawkbit-mcp/hawkbit-mcp-server/target/hawkbit-mcp-server-0-SNAPSHOT.jar`

Modules:

- `hawkbit-mcp-starter` - MCP tools, resources, prompts, security and auto-configuration. Talks to the hawkBit
  Management API via `hawkbit-sdk-mgmt`.
- `hawkbit-mcp-server` - runnable Spring Boot application (`org.eclipse.hawkbit.mcp.server.McpServerStart`), listens on
  port `8082` by default.

## Configuration

The MCP server supports two transport modes:

| Mode         | Use Case                            | Authentication                         |
|--------------|-------------------------------------|----------------------------------------|
| **HTTP**     | Remote access, multi-user           | Per-request via `Authorization` header |
| **STDIO**    | Local CLI tools (e.g., Claude Code) | Environment variables                  |

### HTTP Transport

Use HTTP transport (MCP Streamable HTTP, `spring.ai.mcp.server.protocol=STREAMABLE`, endpoint `/mcp`) when running the
server as a standalone service:

```json
{
  "mcpServers": {
    "hawkbit-mcp": {
      "type": "http",
      "url": "http://localhost:8082/mcp",
      "headers": {
        "Authorization": "Basic <BASE64_ENCODED_CREDENTIALS>"
      }
    }
  }
}
```

Start the server separately:

```bash
java -jar hawkbit-mcp-server-0-SNAPSHOT.jar \
  --hawkbit.mcp.mgmt-url=<HAWKBIT_URL>
```

**Generating Base64 credentials:**

```bash
# Linux/Mac
echo -n "<USERNAME>:<PASSWORD>" | base64

# PowerShell
[Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes("<USERNAME>:<PASSWORD>"))
```

### STDIO Transport

Use STDIO transport for direct integration:

```json
{
  "mcpServers": {
    "hawkbit-mcp": {
      "command": "java",
      "args": [
        "-Dspring.ai.mcp.server.stdio=true",
        "-Dspring.main.web-application-type=none",
        "-jar",
        "/path/to/hawkbit-mcp-server-0-SNAPSHOT.jar"
      ],
      "env": {
        "HAWKBIT_URL": "<HAWKBIT_URL>",
        "HAWKBIT_USERNAME": "<USERNAME>",
        "HAWKBIT_PASSWORD": "<PASSWORD>"
      }
    }
  }
}
```

## Configuration Properties

| Property                                | Environment Variable | Description                                   | Default                 |
|-----------------------------------------|----------------------|-----------------------------------------------|-------------------------|
| `hawkbit.mcp.mgmt-url`                  | `HAWKBIT_URL`        | hawkBit Management API URL                    | `http://localhost:8080` |
| -                                       | `HAWKBIT_USERNAME`   | Username for STDIO mode                       | -                       |
| -                                       | `HAWKBIT_PASSWORD`   | Password for STDIO mode                       | -                       |
| `hawkbit.mcp.validation.enabled`        | -                    | Validate credentials against hawkBit (HTTP)   | `true`                  |
| `hawkbit.mcp.validation.cache-ttl`      | -                    | Cache TTL for auth validation results         | `600s`                  |
| `hawkbit.mcp.validation.cache-max-size` | -                    | Max entries in the auth validation cache      | `1000`                  |
| `hawkbit.mcp.tools-enabled`             | -                    | Register MCP tools                            | `true`                  |
| `hawkbit.mcp.resources-enabled`         | -                    | Register MCP resources (hawkBit docs)         | `true`                  |
| `hawkbit.mcp.prompts-enabled`           | -                    | Register MCP prompts                          | `true`                  |
| `server.port`                           | -                    | HTTP port                                     | `8082`                  |

`HAWKBIT_USERNAME` / `HAWKBIT_PASSWORD` are read directly via `@Value` in `HawkbitMcpProperties` (not as
`hawkbit.mcp.*` keys). In HTTP mode credentials come from the `Authorization` header of each request.

### Operation Controls

You can enable/disable specific operations globally or per-entity (`hawkbit.mcp.operations.*`, all enabled by
default):

- global: `list-enabled`, `create-enabled`, `update-enabled`, `delete-enabled`
- per-entity (`targets`, `distribution-sets`, `software-modules`, `target-filters`, `rollouts`): `list-enabled`,
  `create-enabled`, `update-enabled`, `delete-enabled` - override the global value when set
- `rollouts` additionally: `start-enabled`, `pause-enabled`, `stop-enabled`, `resume-enabled`, `approve-enabled`,
  `deny-enabled`, `retry-enabled`, `trigger-next-group-enabled`
- `actions`: `list-enabled`, `delete-enabled`, `delete-batch-enabled`

```properties
# Global: disable all deletes
hawkbit.mcp.operations.delete-enabled=false

# Per-entity: allow delete for targets only
hawkbit.mcp.operations.targets.delete-enabled=true

# Disable rollout lifecycle operations
hawkbit.mcp.operations.rollouts.start-enabled=false
hawkbit.mcp.operations.rollouts.approve-enabled=false
```

See also the user guide [MCP Server](../docs/mcp-server.md).
