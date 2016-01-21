## Direct Device Integration API (HTTP)
This API is based on HTTP standards and based on a polling mechanism.
The hawkbit update server provides REST resources which are consumed by the device to retrieve software update tasks.


##### Base Poll Resource
```
GET /{tenant}/controller/v1/{controllerId}
```
In the answer to the baseUrl polling the backend can send action links. A possible action is a deploy command.
The client makes a GET request on the given link:

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

##### Deployment Base Resource
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
    "id": "1"
}
```


##### Deployment Feedback Resource
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