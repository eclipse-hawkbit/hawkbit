# Management API

The Management API is a RESTful API that enables to perform Create/Read/Update/Delete operations for provisioning targets (i.e. devices) and repository content (i.e. software).
Based on the Management API you can manage and monitor software update operations via HTTP/HTTPS. The Management API supports JSON payload with hypermedia as well as filtering, sorting and paging. Furthermore the Management API provides permission based access control and standard roles as well as custom role creation.
The API is protected and needs authentication and authorization based on the security concept.

---

### API Version
hawkBit provides an consistent Management API interface that guarantees backwards compatibility for future releases by version control.
The current version of the Management API is `version 1 (v1)` with the URI `http://localhost:8080/rest/v1/`

### API Resources
**Supported HTTP-methods:**
- `GET`
- `POST`
- `PUT`
- `DELETE`

### Headers
For all requests an `Authorization` header has to be set.
- Username: `username`
- Password: `password`
Also have a look to the Security chapter.
In addition, for `POST` and `PUT` requests the `Content-Type` header has to be set. Accepted content-types are.
- `application/json`
- `application/hal+json`


### Request Body
Besides the relevant data (`name`, `description`, `createdBy` etc.) of a resource entity, a resource entity also has URIs `_links` to linked resource entities.
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
        "href": "http://localhost:8080/rest/v1/softwaremodules/83/metadata?offset=0&limit=50"
    }
```


## REST Doc
<div style="text-align: right; margin-bottom: 8px;">
<button onclick="window.open('../rest-api/mgmt.html', '_blank')" 
        style="padding:8px 16px; border-radius:6px; border:1px solid #007bff; 
               background:#007bff; color:#fff; cursor:pointer;">
  Open in new window
</button>
</div>

<iframe 
  src="../rest-api/mgmt.html" 
  width="100%" 
  height="1080px" 
  style="border: 1px solid #000000; border-radius: 16px;">
</iframe>