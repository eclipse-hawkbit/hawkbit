# Management API

The Management API is a RESTful API that enables to perform Create/Read/Update/Delete operations for provisioning
targets (i.e. devices) and repository content (i.e. software).
Based on the Management API you can manage and monitor software update operations via HTTP/HTTPS. The Management API
supports JSON payload with hypermedia as well as filtering, sorting and paging. Furthermore the Management API provides
permission based access control and standard roles as well as custom role creation.
The API is protected and needs authentication and authorization based on the security concept.

---

### API Version

hawkBit provides an consistent Management API interface that guarantees backwards compatibility for future releases by
version control.
The current version of the Management API is `version 1 (v1)` with the URI `http://localhost:8080/rest/v1/`

### API Resources

**Supported HTTP-methods:**

- `GET`
- `POST`
- `PUT`
- `DELETE`

### Query parameter

For filtering, sorting and paging as well as using the Feed Item Query Language (FIQL)
see [Query parameters](query-parameters.md).

### Headers

For all requests an `Authorization` header has to be set.

- Username: `username`
- Password: `password`
  Also have a look to the Security chapter.
  In addition, for `POST` and `PUT` requests the `Content-Type` header has to be set. Accepted content-types are.
- `application/json`
- `application/hal+json`

### Request Body

Besides the relevant data (`name`, `description`, `createdBy` etc.) of a resource entity, a resource entity also has
URIs `_links` to linked resource entities.
A Distribution Set entity may have for example URIs to artifacts, Software Modules, Software Module Types and metadata.

```json
"_links": {
  "artifacts": {
    "href": "http://localhost:8080/rest/v1/softwaremodules/83/artifacts"
  },
  "self": {
    "href": "http://localhost:8080/rest/v1/softwaremodules/83"
  },
  "type": {
    "href": "http://localhost:8080/rest/v1/softwaremoduletypes/43"
  },
  "metadata": {
  " href": "http://localhost:8080/rest/v1/softwaremodules/83/metadata?offset=0&limit=50"
  }
}
```

## Action Status Code Catalogue

The Action Status Code catalogue (`/rest/v1/action-status-codes`) provides a per-tenant registry of numeric status codes
that can be reported by devices or written by the server on action statuses. Each catalogue entry documents the meaning
and severity of a code.

### API Endpoints

- **GET /rest/v1/action-status-codes** - List all catalogue entries with support for RSQL filtering (`q`), pagination (
  `offset`, `limit`), and sorting (`sort`). Filterable fields: `code`, `name`, `severity`.
- **GET /rest/v1/action-status-codes/{id}** - Retrieve a single catalogue entry by its technical ID.
- **POST /rest/v1/action-status-codes** - Create a new catalogue entry. The `code` field must be unique per tenant.
- **PUT /rest/v1/action-status-codes/{id}** - Update an existing catalogue entry. The `code` field is immutable and
  ignored in the request body.
- **DELETE /rest/v1/action-status-codes/{id}** - Delete a catalogue entry. Action statuses and actions keep their
  codes; a code that is still referenced by action statuses is listed under `/undocumented` again and can be
  re-documented via POST.
- **GET /rest/v1/action-status-codes/undocumented** - List status codes reported on action statuses (by devices or
  written by the server) that have no catalogue entry (read-only; to document a code, POST it to the catalogue).

Entries are addressed by technical `id` in the path (`/{id}`), consistent with all other hawkBit Management APIs. The
`code` is a unique, immutable body field. The `severity` can be `TRACE`, `DEBUG`, `INFO`, `WARN`, or `ERROR` (default:
`INFO`). Attempting to create a duplicate code returns HTTP 409. Since the `code` is immutable, a wrong code is corrected by
deleting the entry and creating a new one.

**Permissions:**

- `READ_TARGET` - required for GET operations
- `CREATE_TARGET` - required for POST operations
- `UPDATE_TARGET` - required for PUT operations
- `DELETE_TARGET` - required for DELETE operations

### Server-Reported Status Codes

The server can optionally write status codes on the action statuses it creates itself. Each code is configured via a
tenant configuration key (set via `PUT /rest/v1/system/configs/{key}`):

- **action.status.code.assigned** - Written on the initial action status of an online assignment that starts without
  waiting for confirmation (direct assignment, rollout group start, or confirmation given by the initiator).
- **action.status.code.offline-assigned** - Written on the initial (finished) action status of an offline assignment (
  `?offline=true`).
- **action.status.code.skipped** - Written on the action status of a rollout action that is skipped (closed) because the
  distribution set is already assigned to the target.
- **action.status.code.retrieved** - Written on the action status when the device retrieves a deployment or cancellation
  via the DDI API.
- **action.status.code.download** - Written on the action status when the device downloads an artifact. This is an
  informational status and does not change the action's `lastStatusCode`.
- **action.status.code.auto-confirmation** - Written on the action status when an action is auto-confirmed due to an
  active auto-confirmation rule.
- **action.status.code.wait-for-confirmation** - Written on the initial action status of an action waiting for
  confirmation.
- **action.status.code.canceling** - Written on the action status of all server-side cancellations (manual cancel,
  cancel due to a new assignment, rollout or distribution set invalidation soft cancel, lapsed maintenance window).
- **action.status.code.canceled** - Written on the action status of server-side closings (force quit, auto-close due to
  a new assignment, rollout force cancel).

A key that is not set means no code is written (default behaviour unchanged). There are no fallbacks between keys. When
the server writes a status code, it also overwrites the action's `lastStatusCode` for all statuses except `download` and
`retrieved` (when the action is already canceling). The device's previously reported code remains in the status history.

Operators should choose server codes that do not collide with device-reported codes to avoid confusion in status
analysis.

## REST Doc

<div style="text-align: right; margin-bottom: 8px;">
<button onclick="window.open('../rest-api/mgmt.html', '_blank')" 
        style="padding:8px 16px; border-radius:6px; border:1px solid #007bff; 
               background:#007bff; color:#fff; cursor:pointer;">
  Open in new window
</button>
</div>

<iframe
src="rest-api/mgmt.html"
width="100%"
height="1080px"
style="border: 1px solid #000000; border-radius: 16px;">
</iframe>
