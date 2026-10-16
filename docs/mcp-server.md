# MCP Server

In this guide we describe what the hawkBit MCP Server is and how to run, configure and connect an AI assistant to it.

---

### Overview

The hawkBit MCP Server is a standalone [Model Context Protocol (MCP)](https://modelcontextprotocol.io/) server. It lets
AI assistants (e.g. Claude Code, Claude Desktop, IDE agents) work with hawkBit - list and manage targets, distribution
sets, software modules, target filters, actions and rollouts - using plain language.

It is built with [Spring AI MCP](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-overview.html) and does not
access the hawkBit database. Every call goes through the hawkBit [Management API](management-api.md) using the
`hawkbit-sdk-mgmt` client (see [hawkBit SDK](hawkbit-sdk.md)). So it only needs a running hawkBit Update Server or
Management Server, and it can do only what the used hawkBit user is permitted to do.

The server provides:

- **Tools** - `list_targets`, `list_rollouts`, `list_distribution_sets`, `list_actions`, `list_software_modules`,
  `list_target_filters` and `manage_target`, `manage_rollout`, `manage_distribution_set`, `manage_action`,
  `manage_software_module`, `manage_target_filter`
- **Resources** - hawkBit documentation the assistant can read (`hawkbit://docs/...`, e.g. overview, quick start,
  architecture, SDK)
- **Prompts** - `hawkbit-context` (start-of-session context about hawkBit and the tools) and `rsql-help` (how to
  write filter queries)

---

### Build

```bash
$ git clone https://github.com/eclipse-hawkbit/hawkbit.git
$ cd hawkbit
$ mvn clean install -pl hawkbit-mcp/hawkbit-mcp-server -am -DskipTests
```

The server jar is `hawkbit-mcp/hawkbit-mcp-server/target/hawkbit-mcp-server-0-SNAPSHOT.jar`.

---

### Run with HTTP transport

In HTTP mode the server runs as a service that many users can share. It uses the MCP Streamable HTTP transport on
the `/mcp` endpoint, port `8082` by default:

```bash
$ java -jar hawkbit-mcp-server-0-SNAPSHOT.jar --hawkbit.mcp.mgmt-url=http://localhost:8080
```

Each MCP client sends its own hawkBit credentials with every request in the `Authorization` header (HTTP Basic). The
server checks them against hawkBit and caches the result for a short time.

Example MCP client configuration:

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

You can create the Base64 value of `<USERNAME>:<PASSWORD>` with:

```bash
$ echo -n "admin:admin" | base64
```

---

### Run with STDIO transport

In STDIO mode the MCP client starts the server itself as a local process and talks to it over standard input / output.
The hawkBit URL and credentials are passed as environment variables:

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
        "HAWKBIT_URL": "http://localhost:8080",
        "HAWKBIT_USERNAME": "admin",
        "HAWKBIT_PASSWORD": "admin"
      }
    }
  }
}
```

---

### Configuration

The most important options (as Spring Boot properties, e.g. `--name=value` on the command line, or environment
variables):

| Property / Environment variable         | Description                                            | Default                 |
|-----------------------------------------|--------------------------------------------------------|-------------------------|
| `hawkbit.mcp.mgmt-url` / `HAWKBIT_URL`  | URL of the hawkBit Management API                      | `http://localhost:8080` |
| `HAWKBIT_USERNAME`, `HAWKBIT_PASSWORD`  | hawkBit credentials, used in STDIO mode                | -                       |
| `server.port`                           | HTTP port                                              | `8082`                  |
| `hawkbit.mcp.validation.enabled`        | Check the request credentials against hawkBit (HTTP)   | `true`                  |
| `hawkbit.mcp.validation.cache-ttl`      | How long a credentials check result is cached          | `600s`                  |
| `hawkbit.mcp.validation.cache-max-size` | Maximum number of cached credentials check results     | `1000`                  |
| `hawkbit.mcp.tools-enabled`             | Offer the MCP tools                                    | `true`                  |
| `hawkbit.mcp.resources-enabled`         | Offer the MCP resources                                | `true`                  |
| `hawkbit.mcp.prompts-enabled`           | Offer the MCP prompts                                  | `true`                  |

---

### Limiting operations

By default all operations are allowed (if the hawkBit user has the needed permissions). You can switch operations off,
globally or per entity, with `hawkbit.mcp.operations.*`. A per-entity setting overrides the global one:

```properties
# no deletes at all ...
hawkbit.mcp.operations.delete-enabled=false
# ... except for targets
hawkbit.mcp.operations.targets.delete-enabled=true

# don't let the assistant start or approve rollouts
hawkbit.mcp.operations.rollouts.start-enabled=false
hawkbit.mcp.operations.rollouts.approve-enabled=false
```

Available switches:

- global: `list-enabled`, `create-enabled`, `update-enabled`, `delete-enabled`
- `targets`, `distribution-sets`, `software-modules`, `target-filters`, `rollouts`: `list-enabled`, `create-enabled`,
  `update-enabled`, `delete-enabled`
- `rollouts` also: `start-enabled`, `pause-enabled`, `stop-enabled`, `resume-enabled`, `approve-enabled`,
  `deny-enabled`, `retry-enabled`, `trigger-next-group-enabled`
- `actions`: `list-enabled`, `delete-enabled`, `delete-batch-enabled`
