# RSQL Query Syntax for hawkBit

RSQL is a query language for filtering entities. Use it with the `rsql` parameter in list tools.

## Operators

| Operator | Meaning | Example |
|----------|---------|---------|
| `==` | Equal | `name==MyTarget` |
| `!=` | Not equal | `status!=ERROR` |
| `=lt=` | Less than | `createdAt=lt=1704067200000` |
| `=gt=` | Greater than | `lastTargetQuery=gt=1704067200000` |
| `=in=` | In list | `status=in=(RUNNING,FINISHED)` |
| `=out=` | Not in list | `updateStatus=out=(ERROR,UNKNOWN)` |

## Combining Conditions

- **AND**: Use `;` → `status==RUNNING;name==Device*`
- **OR**: Use `,` → `status==ERROR,status==CANCELED`

## Wildcards

Use `*` for pattern matching:
- `name==Device*` - Starts with "Device"
- `name==*Controller` - Ends with "Controller"
- `name==*test*` - Contains "test"

## Nested Fields

Access related entities with dot notation:
- `assignedDistributionSet.name==Firmware`
- `target.controllerId==device-001`
- `metadata.environment==production`

## Common Queries

```
# Targets with errors
updateStatus==ERROR

# Running rollouts
status==RUNNING

# Actions for a specific target
target.controllerId==device-001

# Distribution sets by type
type.key==os_app
```

For complete field reference, read `hawkbit://docs/entity-definitions`.
