# hawkBit MCP Server - Getting Started

You are connected to the **Eclipse hawkBit MCP Server**. hawkBit is a domain-independent
back-end framework for rolling out software updates to IoT devices.

## What You Can Do

### Tools Available
You have access to tools for querying the hawkBit Management API:
- `list_targets` - Query devices that can receive software updates
- `list_distribution_sets` - Query software packages for deployment
- `list_rollouts` - Query rollout campaigns for mass deployments
- `list_actions` - Query deployment operations assigned to targets
- `list_software_modules` - Query individual software components
- `list_target_filters` - Query RSQL filters for grouping targets

All tools support RSQL filtering. Read the "hawkBit Entity Definitions" resource for query syntax.

### Documentation Resources
The following documentation is available (read with MCP resources):

**Getting Started:**
- `hawkbit://docs/overview` - High-level introduction
- `hawkbit://docs/what-is-hawkbit` - Why hawkBit exists
- `hawkbit://docs/features` - Feature overview
- `hawkbit://docs/architecture` - System architecture

**Core Concepts:**
- `hawkbit://docs/datamodel` - Entity relationships (targets, distribution sets, modules)
- `hawkbit://docs/rollout-management` - How rollouts work
- `hawkbit://docs/target-state` - Target state machine
- `hawkbit://docs/authentication` - Security and authentication
- `hawkbit://docs/authorization` - Permissions and access control

**APIs:**
- `hawkbit://docs/management-api` - REST API for management
- `hawkbit://docs/ddi-api` - Device polling API
- `hawkbit://docs/dmf-api` - AMQP-based device federation

**Reference:**
- `hawkbit://docs/entity-definitions` - RSQL filtering syntax and examples

## Recommended First Steps

1. **For general questions about hawkBit**: Read `hawkbit://docs/overview` or `hawkbit://docs/features`
2. **For data model questions**: Read `hawkbit://docs/datamodel`
3. **For RSQL query help**: Read `hawkbit://docs/entity-definitions`
4. **For rollout/deployment questions**: Read `hawkbit://docs/rollout-management`
