# hawkBit MCP Server

This project provides an implementation of a **Model Context Protocol (MCP) server**
that exposes **Eclipse hawkBit** management capabilities to **intelligent agents**.

---

## Configuration

The MCP server is configured via `application.yaml`.

Example configuration:

```yaml
hawkbit:
  server:
    mgmt-url: http://localhost:8080   # hawkBit Management API URL

server:
  port: 8090                          # MCP server port

```

## Build

To build the project using Maven:

```bash
mvn clean package
```

This will generate the executable JAR under:
```bash
target/hawkbit-mcp-server-<version>.jar
```

## MCP Client Integration

To connect this server to an MCP client or Agent, register it in the client configuration.

Example MCP client configuration:
```json
{
  "mcpServers": {
    "hawkbit": {
      "command": "java",
      "args": [
        "-jar",
        "/path/target/hawkbit-mcp-server-0.0.1-SNAPSHOT.jar"
      ]
    }
  }
}
```
