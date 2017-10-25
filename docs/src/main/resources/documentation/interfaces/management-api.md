---
layout: documentation
title: Management-API
---

{% include base.html %}

# Management API

## Overview
The Management API is a RESTful API that enables to perform Create/Read/Update/Delete operations for provisioning targets (i.e. devices) and repository content (i.e. software). Based on the Management API you can manage and monitor software update operations via HTTP/HTTPS. The _Management API_ supports JSON payload with hypermedia as well as filtering, sorting and paging. Furthermore the Management API provides permission based access control and standard roles as well as custom role creation.  

The API is protected and needs authentication and authorization based on the security concept.

## API Version

_hawkBit_ provides an consistent Management API interface that guarantees backwards compatibility for future releases by version control.

The current version of the Management API is `version 1 (v1)` with the URI http://localhost:8080/rest/v1/

## API Resources

Supported HTTP-methods are:

- GET
- POST
- PUT
- DELETE

Available Management APIs resources are:

* [Targets](https://docs.bosch-iot-rollouts.com/documentation/rest-api/targets-api-guide.html)
* [Distribution Sets](https://docs.bosch-iot-rollouts.com/documentation/rest-api/distributionsets-api-guide.html)
* [Distribution Set Types](https://docs.bosch-iot-rollouts.com/documentation/rest-api/distributionsettypes-api-guide.html)
* [Software Modules](https://docs.bosch-iot-rollouts.com/documentation/rest-api/softwaremodules-api-guide.html)
* [Software Module Types](https://docs.bosch-iot-rollouts.com/documentation/rest-api/softwaremoduletypes-api-guide.html)
* [Target Tag](https://docs.bosch-iot-rollouts.com/documentation/rest-api/targettag-api-guide.html)
* [Distribution Set Tag](https://docs.bosch-iot-rollouts.com/documentation/rest-api/distributionsettag-api-guide.html)
* [Rollouts](https://docs.bosch-iot-rollouts.com/documentation/rest-api/rollout-api-guide.html)
* [System Configuration](https://docs.bosch-iot-rollouts.com/documentation/rest-api/tenant-api-guide.html)


## Headers

For all requests an `Authorization` header has to be set.

* Username: `Tenant\username`
* Password: `password`

Also have a look to the [Security](../security/security.html) chapter.

In addition, for POST and PUT requests the `Content-Type` header has to be set. Accepted content-types are.

* `application/json`
* `application/hal+json`

## Request Body

Besides the relevant data (name, description, createdBy etc.) of a resource entity, a resource entity also has URIs (`_links`) to linked resource entities.

A _Distribution Set_ entity may have for example URIs to artifacts, _Software Modules_, _Software Module Types_ and metadata.


{% highlight json %}
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
{% endhighlight %}