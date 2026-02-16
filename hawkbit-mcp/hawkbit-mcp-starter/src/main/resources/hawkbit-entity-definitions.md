# hawkBit Entity Definitions and RSQL Filtering Guide

This document describes the entities available in hawkBit and how to filter and sort them using RSQL queries through the MCP tools.

## RSQL Query Syntax

RSQL (RESTful Service Query Language) is a query language for filtering and searching entities. It uses a simple, URL-friendly syntax.

### Comparison Operators

| Operator | Description | Example |
|----------|-------------|---------|
| `==` | Equal to | `name==MyTarget` |
| `!=` | Not equal to | `status!=ERROR` |
| `=lt=` or `<` | Less than | `createdAt=lt=1609459200000` |
| `=le=` or `<=` | Less than or equal | `weight=le=500` |
| `=gt=` or `>` | Greater than | `lastTargetQuery=gt=1609459200000` |
| `=ge=` or `>=` | Greater than or equal | `id=ge=100` |
| `=in=` | In list | `status=in=(RUNNING,FINISHED)` |
| `=out=` | Not in list | `updateStatus=out=(ERROR,UNKNOWN)` |

### Logical Operators

| Operator | Description | Example |
|----------|-------------|---------|
| `;` or `and` | Logical AND | `name==Test*;status==RUNNING` |
| `,` or `or` | Logical OR | `status==ERROR,status==CANCELED` |

Always use "and" or "or" for operators when grouping conditions - since this is the human-readable format.

### Wildcard Support

Use `*` as a wildcard character for pattern matching:
- `name==Device*` - Names starting with "Device"
- `name==*Controller` - Names ending with "Controller"
- `name==*test*` - Names containing "test"

### Sub-Entity Filtering

Access nested entity fields using dot notation:
- `assignedDistributionSet.name==MyDS`
- `target.controllerId==device123`
- `type.key==os`

### Map/Metadata Filtering

For metadata and attributes, use dot notation with the key:
- `metadata.environment==production`
- `controllerAttributes.revision==1.5`

---

## Entity Definitions

### Target

Targets represent devices or software instances that can receive software updates.

**Filterable/Sortable Fields:**

| Field                              | Description                                                          | Type |
|------------------------------------|----------------------------------------------------------------------|------|
| `controllerId`                     | Unique identifier of the target                                      | String |
| `name`                             | Display name                                                         | String |
| `description`                      | Description text                                                     | String |
| `updateStatus`                     | Current update status (UNKNOWN, IN_SYNC, PENDING, ERROR, REGISTERED) | Enum |
| `address`                          | IP address or URI                                                    | String |
| `lastTargetQuery`                  | Last time the target polled (timestamp in ms)                        | Long |
| `createdAt`                        | Creation timestamp                                                   | Long |
| `createdBy`                        | Creator username                                                     | String |
| `lastModifiedAt`                   | Last modification timestamp                                          | Long |
| `lastModifiedBy`                   | Last modifier username                                               | String |
| `assignedDistributionSet.name`     | Name of assigned distribution set                                    | String |
| `assignedDistributionSet.version`  | Version of assigned distribution set                                 | String |
| `installedDistributionSet.name`    | Name of installed distribution set                                   | String |
| `installedDistributionSet.version` | Version of installed distribution set                                | String |
| `targetType.key`                   | Target type key                                                      | String |
| `targetType.name`                  | Target type name                                                     | String |
| `tags.name`                        | Tag name                                                             | String |
| `group`                            | Group name                                                           | String |
| `metadata.<key>`                   | Metadata value by key                                                | String |
| `controllerAttributes.<key>`       | Controller attribute by key                                          | String |

**Example Queries:**
```
# Find targets with update errors
updateStatus==ERROR

# Find targets by name pattern
name==device-*

# Find targets with specific distribution set assigned
assignedDistributionSet.name==Firmware;assignedDistributionSet.version==2.0.0

# Find targets that haven't polled in 24 hours (timestamp example)
lastTargetQuery=lt=1704067200000

# Find targets by tag
tags.name==production

# Find targets by metadata
metadata.location==factory-A

# Find targets by controller attribute
controllerAttributes.firmware_version==1.2.3
```

---

### Distribution Set

Distribution Sets are collections of software modules that can be deployed to targets.

**Filterable/Sortable Fields:**

| Field | Description | Type |
|-------|-------------|------|
| `id` | Unique identifier | Long |
| `name` | Distribution set name | String |
| `version` | Version string | String |
| `description` | Description text | String |
| `type.key` | Distribution set type key | String |
| `type.name` | Distribution set type name | String |
| `valid` | Whether the DS is valid for deployment | Boolean |
| `createdAt` | Creation timestamp | Long |
| `createdBy` | Creator username | String |
| `lastModifiedAt` | Last modification timestamp | Long |
| `lastModifiedBy` | Last modifier username | String |
| `tags.name` | Tag name | String |
| `modules.name` | Software module name | String |
| `metadata.<key>` | Metadata value by key | String |

**Example Queries:**
```
# Find distribution sets by name
name==Firmware*

# Find valid distribution sets only
valid==true

# Find by type
type.key==os_app

# Find by tag
tags.name==release-candidate

# Find distribution sets containing a specific module
modules.name==bootloader
```

---

### Rollout

Rollouts are used to deploy software to groups of targets in a controlled manner.

**Filterable/Sortable Fields:**

| Field | Description | Type |
|-------|-------------|------|
| `id` | Unique identifier | Long |
| `name` | Rollout name | String |
| `description` | Description text | String |
| `status` | Rollout status (CREATING, READY, PAUSED, STARTING, RUNNING, FINISHED, etc.) | Enum |
| `distributionSet.id` | Distribution set ID | Long |
| `distributionSet.name` | Distribution set name | String |
| `distributionSet.version` | Distribution set version | String |
| `distributionSet.type` | Distribution set type | String |
| `createdAt` | Creation timestamp | Long |
| `createdBy` | Creator username | String |
| `lastModifiedAt` | Last modification timestamp | Long |
| `lastModifiedBy` | Last modifier username | String |

**Example Queries:**
```
# Find running rollouts
status==RUNNING

# Find rollouts by name
name==Campaign*

# Find rollouts for a specific distribution set
distributionSet.name==Firmware;distributionSet.version==2.0.0

# Find finished or paused rollouts
status=in=(FINISHED,PAUSED)
```

---

### Action

Actions represent deployment operations assigned to targets.

**Filterable/Sortable Fields:**

| Field | Description | Type |
|-------|-------------|------|
| `id` | Unique identifier | Long |
| `status` | Action status (SCHEDULED, RUNNING, FINISHED, ERROR, CANCELED, etc.) | Enum |
| `active` | Whether the action is currently active | Boolean |
| `weight` | Priority weight (0-1000) | Integer |
| `lastActionStatusCode` | Last status code reported | Integer |
| `externalRef` | External reference string | String |
| `target.controllerId` | Target controller ID | String |
| `target.name` | Target name | String |
| `target.updateStatus` | Target update status | Enum |
| `distributionSet.id` | Distribution set ID | Long |
| `distributionSet.name` | Distribution set name | String |
| `distributionSet.version` | Distribution set version | String |
| `rollout.id` | Rollout ID | Long |
| `rollout.name` | Rollout name | String |
| `rolloutGroup.id` | Rollout group ID | Long |
| `rolloutGroup.name` | Rollout group name | String |
| `createdAt` | Creation timestamp | Long |
| `createdBy` | Creator username | String |
| `lastModifiedAt` | Last modification timestamp | Long |
| `lastModifiedBy` | Last modifier username | String |

**Example Queries:**
```
# Find active actions
active==true

# Find actions by status
status==RUNNING

# Find failed actions
status==ERROR

# Find actions for a specific target
target.controllerId==device-001

# Find actions for a specific rollout
rollout.name==Campaign2024

# Find high-priority actions
weight=gt=800

# Find actions with specific status code
lastActionStatusCode==200
```

---

### Software Module

Software Modules are individual software components that make up distribution sets.

**Filterable/Sortable Fields:**

| Field | Description | Type |
|-------|-------------|------|
| `id` | Unique identifier | Long |
| `name` | Module name | String |
| `version` | Version string | String |
| `description` | Description text | String |
| `type.key` | Software module type key | String |
| `type.name` | Software module type name | String |
| `createdAt` | Creation timestamp | Long |
| `createdBy` | Creator username | String |
| `lastModifiedAt` | Last modification timestamp | Long |
| `lastModifiedBy` | Last modifier username | String |
| `metadata.<key>` | Metadata value by key | String |

**Example Queries:**
```
# Find modules by name
name==bootloader*

# Find modules by type
type.key==os

# Find modules by version
version==2.0.*

# Find modules with specific metadata
metadata.checksum==abc123
```

---

### Target Filter Query

Target Filter Queries define RSQL filters for grouping targets, used for rollouts and auto-assignment.

**Filterable/Sortable Fields:**

| Field | Description | Type |
|-------|-------------|------|
| `id` | Unique identifier | Long |
| `name` | Filter name | String |
| `autoAssignDistributionSet.name` | Auto-assign DS name | String |
| `autoAssignDistributionSet.version` | Auto-assign DS version | String |
| `createdAt` | Creation timestamp | Long |
| `createdBy` | Creator username | String |
| `lastModifiedAt` | Last modification timestamp | Long |
| `lastModifiedBy` | Last modifier username | String |

**Example Queries:**
```
# Find filters by name
name==Production*

# Find filters with auto-assignment configured
autoAssignDistributionSet.name==*

# Find filters for a specific auto-assign distribution set
autoAssignDistributionSet.name==Firmware;autoAssignDistributionSet.version==2.0.0
```

---

## Common Query Patterns

### Combining Multiple Conditions (AND)
```
status==RUNNING;createdAt=gt=1704067200000
```

### Alternative Conditions (OR)
```
status==ERROR,status==CANCELED
```

### Complex Queries with Grouping
```
(status==RUNNING,status==SCHEDULED);target.updateStatus!=ERROR
```

### Timestamp Filtering
Timestamps are in milliseconds since Unix epoch:
```
# Created after January 1, 2024
createdAt=gt=1704067200000

# Modified in the last 24 hours (example timestamp)
lastModifiedAt=gt=1704153600000
```

### Wildcard Patterns
```
# Starts with
name==prefix*

# Ends with
name==*suffix

# Contains
name==*substring*
```
