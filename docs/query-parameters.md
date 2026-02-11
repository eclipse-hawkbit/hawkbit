# Query Parameters

## Table of Contents

- [Sorting](#sorting)
- [Paging](#paging)
  - [Examples](#examples)
- [FIQL Syntax Search Query](#fiql-syntax-search-query)
  - [Basic operators and composite operators at a glance](#basic-operators-and-composite-operators-at-a-glance)
    - [Basic operators](#basic-operators)
    - [Composite operators](#composite-operators)
  - [Using wildcards to filter](#using-wildcards-to-filter)
  - [Combining Sorting, Paging, and FIQL](#combining-sorting-paging-and-fiql)

## Sorting

The `sort` query parameter defines the sort order for query results.

A sort criteria includes a field name and direction (`ASC` ascending or `DESC` descending).

Multiple criteria can be used, defining the sort order of entities in the result.

**Syntax:**

| Parameter | Value |
|-----------|-------|
| sort | sort_criteria {"," sort_criteria} |
| sort_criteria | field_name ":" ("ASC"\|"DESC") |
| field_name | alpha |
| alpha | (digit\|character){digit\|character}; |
| digit | "0"\|"1"\|"2"\|"3"\|"4"\|"5"\|"6"\|"7"\|"8"\|"9"; |
| character | lowercase_character\|uppercase_character\|special_character |
| special_character | "_" |
| lowercase_character | "a"\|"b"\|"c"\|"d"\|"e"\|"f"\|"g"\|"h"\|"i"\|"j"\|"k"\|"l"\|"m"\|"n"\|"o"\|"p"\|"q"\|"r"\|"s"\|"t"\|"u"\|"v"\|"w"\|"x"\|"y"\|"z"; |
| uppercase_character | "A"\|"B"\|"C"\|"D"\|"E"\|"F"\|"G"\|"H"\|"I"\|"J"\|"K"\|"L"\|"M"\|"N"\|"O"\|"P"\|"Q"\|"R"\|"S"\|"T"\|"U"\|"V"\|"W"\|"X"\|"Y"\|"Z"; |

**Example:**

```
/targets?sort=field_1:ASC,field_2:DESC,field_3:ASC
```

## Paging

Pagination is automatically applied to all GET requests for collection resources.

**Configuration parameters:**

- `offset`: paging offset (default is 0)
- `limit`: maximum entries per page (default is 50)

The maximum value for `limit` is 500. For groups, the maximum is 1000 (subject to change in future versions).

Invalid values default to standard settings. Limits exceeding the maximum use the maximum value instead.

### Examples

| URL | Description |
|-----|-------------|
| `/targets?sort=name:ASC` | Sorts targets by name, returns first 50 (default offset 0, default limit 50) |
| `/targets?sort=name:ASC&limit=10` | Sorts targets by name, returns first 10 |
| `/targets?sort=name:ASC&offset=10` | Sorts targets by name, returns next 50 targets after the 10th (starting with 11th) |
| `/targets?sort=name:ASC&offset=20&limit=10` | Sorts targets by name, returns next 10 targets after the 20th (starting with 21st) |
| `/targets?sort=name:ASC&offset=100&limit=600` | Sorts targets by name, returns next 500 targets (max limit, not 600) after the 100th (starting with 101st) |

## FIQL Syntax Search Query

The `q` parameter filters resource fields using Feed Item Query Language (FIQL).

**Syntax:**

```
q=FIQL-expression
```

**Expression structure:**

```
q=field<basic_operator>value<composite_operator>field<basic_operator>value<...>
```

- `q`: identifier for FIQL query
- `field`: resource field name (see Entity definitions for filterable fields)
- `value`: field value
- `<basic_operator>`: operators for simple queries
- `<composite_operator>`: operators to join simple queries

### Basic Operators and Composite Operators at a Glance

#### Basic Operators:

| Operator | Description | Example |
|----------|-------------|---------|
| `==` | Equal to expression | `/targets?q=name==ccu299792` |
| `!=` | Not equal to expression | `/targets?q=name!=ccu299792` |
| `=lt=` | Less than expression | `/distributionsets?q=id=lt=142` |
| `=le=` | Less than or equal expression | `/distributionsets?q=id=le=142` |
| `=gt=` | Greater than expression | `/distributionsets?q=id=gt=142` |
| `=ge=` | Greater than or equal expression | `/distributionsets?q=id=ge=142` |
| `=li=` | Like expression | `/targets?q=name=li=_________0` |
| `=in=` | In expression | `/targets?q=name=in=(ccu299792, shc137)` |
| `=out=` | Not in (out) expression | `/targets?q=name=out=(ccu299792, shc137)` |

#### Composite Operators:

| Operator     | Description | Example                                                           |
|--------------|-------------|-------------------------------------------------------------------|
| `and` or `;` | AND expression | `/rest/v1/targets?q=updatestatus==unknown and controllerId==SHC*` |
| `or`  or `,` | OR expression | `/rest/v1/targets?q=updatestatus==unknown or updatestatus==error` |

### Using Wildcards to Filter

Use `*` for wildcard matches.

| Example URL | Description |
|-------------|-------------|
| `/targets?q=name==ccu*` | Returns targets with names starting with ccu |
| `/targets?q=name==*ccu` | Returns targets with names ending with ccu |
| `/targets?q=name==*ccu*` | Returns targets containing ccu in their name |
| `/targets?q=name=="ccu\\*"` | Returns targets with names starting with ccu* |
| `/targets?q=name=="*\\*\*"` | Returns targets containing asterisk(*) in their names |

### Combining Sorting, Paging, and FIQL

| Example URL                                                                                         | Description |
|-----------------------------------------------------------------------------------------------------|-------------|
| `/targets?sort=controllerId:DESC&limit=5&q=updatestatus==pending`                                   | Returns 5 targets with updatestatus 'pending', sorted descending |
| `/softwaremodules?sort=name:ASC&q=version==5.1.1 and type==application`                             | Returns software modules sorted by name, with version 5.1.1 and type application |
| `/distributionsets?sort=name:DESC&limit=150&q=name==*CCU* or description==*CCU*`                    | Returns 150 distribution sets sorted descending by name with CCU in name or description |
| `/distributionsettypes?limit=100&offset=100&q=key==vhicletypex2015`                                 | Returns 100 distribution set types (starting with 101st) with key vhicletypex2015 |
| `/targets?sort=name:ASC&offset=100&limit=25&q=(description==*SH* or name==*SH*) and name=li=_____`  | Returns 25 targets (starting with 101st), sorted ascending, with 'SH' in name or description and exactly 5 characters |
| `/targets?sort=name:DESC&limit=50&q=name=in=(*ccu*, *ecu*) and updatestatus=in=(pending, in\_sync)` | Returns 50 targets sorted descending with 'ccu' or 'ecu' in name and updatestatus 'pending' or 'in_sync' |
