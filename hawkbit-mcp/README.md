# hawkBit MCP Server

A standalone [Model Context Protocol (MCP)](https://modelcontextprotocol.io/) server that provides AI assistants with tools to interact with [Eclipse hawkBit](https://www.eclipse.org/hawkbit/) for IoT device software update management.

## Building

From the project root directory:

```bash
mvn clean package -pl hawkbit-mcp -am -DskipTests
```

The JAR will be created at: `hawkbit-mcp/target/hawkbit-mcp-server-0-SNAPSHOT.jar`

## Configuration

The MCP server supports two transport modes:

| Mode | Use Case | Authentication |
|------|----------|----------------|
| **HTTP/SSE** | Remote access, multi-user | Per-request via `Authorization` header |
| **STDIO** | Local CLI tools (e.g., Claude Code) | Environment variables |


### HTTP Transport

Use HTTP transport when running the server as a standalone service:

```json
{
  "mcpServers": {
    "hawkbit-mcp": {
      "type": "http",
      "url": "http://localhost:8081/mcp",
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
echo -n "<TENANT>\\<USERNAME>:<PASSWORD>" | base64

# PowerShell
[Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes("<TENANT>\<USERNAME>:<PASSWORD>"))
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
        "HAWKBIT_USERNAME": "<TENANT>\\<USERNAME>",
        "HAWKBIT_PASSWORD": "<PASSWORD>"
      }
    }
  }
}
```

## Configuration Properties

| Property | Environment Variable | Description | Default |
|----------|---------------------|-------------|---------|
| `hawkbit.mcp.mgmt-url` | `HAWKBIT_URL` | hawkBit Management API URL | `http://localhost:8080` |
| `hawkbit.mcp.username` | `HAWKBIT_USERNAME` | Username for STDIO mode | - |
| `hawkbit.mcp.password` | `HAWKBIT_PASSWORD` | Password for STDIO mode | - |
| `hawkbit.mcp.validation.enabled` | - | Validate credentials against hawkBit | `true` |
| `hawkbit.mcp.validation.cache-ttl` | - | Cache TTL for auth validation | `600s` |

### Operation Controls

You can enable/disable specific operations globally or per-entity:

```properties
# Global: disable all deletes
hawkbit.mcp.operations.delete-enabled=false

# Per-entity: allow delete for targets only
hawkbit.mcp.operations.targets.delete-enabled=true

# Disable rollout lifecycle operations
hawkbit.mcp.operations.rollouts.start-enabled=false
hawkbit.mcp.operations.rollouts.approve-enabled=false
```
