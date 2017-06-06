---
layout: documentation
title: DDI-API
---

{% include base.html %}

This API is based on HTTP standards and based on a polling mechanism.

The _hawkbit_ [update server](https://github.com/eclipse/hawkbit) provides REST resources which are consumed by the device to retrieve software update tasks.

Note: in DDI the target is identified using a  **controllerId**. Controller is used as a term for the actual service/client on the device. That allows users to have in some cases even multiple clients on the same target for different tasks, e.g. Firmware update and App management.

# State Machine Mapping
For historical reasons the DDI has a different state machine and status messages than the [Target State Machine](../architecture/targetstate.html) of the _hawkBit_ update server.

This is kept in order to ensure that _DDI_ stays compatible for devices out there in the field. A future version "2" of _DDI_ might change that. _DDI_ also defines more states than the update server, e.g. multiple DDI states are currently mapped by the _DDI_ implementation to _RUNNING_ state. It is possible that in the future _hawkBit_ will fully leverage these additional states.

The _DDI_ API allows the device to provide the following feedback messages:

DDI `status.execution` type | handling by update server                                                                                                                                                                                                                | Mapped ActionStatus type
--------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -----------------------------------------------------
CANCELED                    | This is send by the target as confirmation of a cancelation request by the update server.                                                                                                                                                | CANCELED
REJECTED                    | This is send by the target in case an update of a cancelation is rejected, i.e. cannot be fulfilled at this point in time. Note: the target should send a CLOSED->ERROR if it believes it will not be able to proceed the action at all. | WARNING
CLOSED                      | Target completes the action either with `status.result.finished` SUCCESS or FAILURE as result. Note: DDI defines also a status NONE which will not be interpreted by the update server and handled like SUCCESS.                         | ERROR (DDI FAILURE) or FINISHED (DDI SUCCESS or NONE)
PROCEEDING                  | This can be used by the target to inform that it is working on the action.                                                                                                                                                               | RUNNING
SCHEDULED                   | This can be used by the target to inform that it scheduled on the action.                                                                                                                                                                | RUNNING
RESUMED                     | This can be used by the target to inform that it continued to work on the action.                                                                                                                                                        | RUNNING

# Resource Overview
The following chapters provide basic examples for the most important resources but we provide also a [detailed resource documentation](https://docs.bosch-iot-rollouts.com/documentation/rest-api/rootcontroller-api-guide.html).

## Base Poll Resource

```
GET /{tenant}/controller/v1/{controllerId}
```

In the answer to the baseUrl polling the backend can send action links. A possible action is a deploy command. The client makes a GET request on the given link:

_Example Response_

```
{
    "config": {
        "polling": {
            "sleep": "00:05:00"
        }
    },
    "_links": {
        "deploymentBase": {
            "href": "http://localhost:8080/default/controller/v1/example/deploymentBase/1?c=644088541"
        },
        "configData": {
            "href": "http://localhost:8080/default/controller/v1/example/configData"
        }
    }
}
```

## Deployment Base Resource

```
GET /{tenant}/controller/v1/{controllerId}/deploymentBase/{id}
```

_Example Response_

```
{
    "deployment": {
        "download": "forced",
        "update": "forced",
        "chunks": [
            {
                "part": "os",
                "version": "1.0.0",
                "name": "Linux",
                "artifacts": [
                    {
                        "filename": "linux.zip",
                        "hashes": {
                            "sha1": "46fc56de883ec027759d8513458fe1010aa7e793",
                            "md5": "5813e9655bd6871d0c25b8d510fd8605"
                        },
                        "size": 52167,
                        "_links": {
                            "download": {
                                "href": "http://localhost:8080/default/controller/v1/example/softwaremodules/1/artifacts/linux.zip"
                            },
                            "md5sum": {
                                "href": "http://localhost:8080/default/controller/v1/example/softwaremodules/1/artifacts/linux.zip.MD5SUM"
                            }
                        }
                    }
                ]
            }
        ]
    },
    "id": "1",
    "actionHistory": {
        "status": "RETRIEVED",
        "messages": [
            "Installing update",
            "Downloading artifacts"
            ]
    }
}
```

## Deployment Feedback Resource
To every deployment the client can post feedback back to the update-server about the deployment status.

```
POST /{tenant}/controller/v1/{controllerId}/deploymentBase/{id}/feedback
Content-Type: application/json
```

_Example Body Deployment Success_

```
{
    "id": 1,
    "time": "20140511T121314",
    "status": {
        "execution": "closed",
        "result": {
            "finished": "success",
            "progress": {}
        }
    }
}
```

_Example Body Deployment Proceeding_

```
{
    "id": "1",
    "time": "20140511T121314",
    "status": {
        "execution": "proceeding",
        "result": {
            "finished": "none",
            "progress": {
                "cnt": 2,
                "of": 5
            }
        },
        "details": [
            "checking hash sums"
        ]
    }
}
```

_Example Body Deployment Error_

```
{
    "id": 1,
    "time": "20140511T121314",
    "status": {
        "execution": "rejected",
        "result": {
            "finished": "failure",
            "progress": {}
        },
        "details": [
            "something bad happend"
        ]
    }
}
```