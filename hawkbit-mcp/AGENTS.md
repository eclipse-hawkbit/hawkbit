# hawkbit-mcp

Spring AI MCP server (`-starter` holds the logic, `-server` is the bootable app `McpServerStart`, port 8082, endpoint `/mcp`). It wraps the Mgmt REST API through `hawkbit-sdk-mgmt` (Feign over the `hawkbit-mgmt-api` interfaces).

## Commands
- Build: `mvn -pl hawkbit-mcp/hawkbit-mcp-server -am install -DskipTests`. Note: `-pl hawkbit-mcp` alone builds only the parent pom.

## Rules
- A new Mgmt endpoint is exposed to MCP only if a tool is added in `hawkbit-mcp-starter/.../mcp/server/tools/HawkbitMcpToolProvider.java`. Gate it with the matching `hawkbit.mcp.operations.*` switch.
- Prompts and resources are bundled markdown in `hawkbit-mcp-starter/src/main/resources` (`prompts/`, `hawkbit-entity-definitions.md`). Keep `hawkbit-entity-definitions.md` in sync with `../docs/entity-definitions.md`.
- Credentials come from the env vars `HAWKBIT_USERNAME` / `HAWKBIT_PASSWORD`. Authentication is verified against hawkBit and cached (Caffeine).

## Read when
- Config, operations switches, transports → `README.md`, `../docs/mcp-server.md`
