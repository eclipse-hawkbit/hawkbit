# Eclipse hawkBit - Offline Update

## Introduction
There are scenarios where a regular device update may fail and there is a need
for manual intervention to perform the software updates on devices; for example,
in case of failure-recovery from a bad update. In such cases we need a way to
record these manual updates into hawkBit's history so that hawkBit can serve as
the single source of truth regarding the updates done to devices.

This feature is provided by the Offline Update API which is provided as a
separate hawkBit extension and is implemented based on Spring plug-in mechanism.

## Enable Offline Update Extension
The module contains a spring-boot autoconfiguration for integration into
spring-boot projects. For using this extension in the hawkbit-runtime:

1. Set the following property in application.properties.
    ```
    offlineUpdate.enabled=true
    ```

2. Add the maven dependency in pom.xml of hawkbit-update-server.

    ```
    <dependency>
        <groupId>org.eclipse.hawkbit</groupId>
        <artifactId>hawkbit-extension-offline-update</artifactId>
        <version>${project.version}</version>
    </dependency>
    ```

## Functionality
A new REST end point is provided by the extension which can be used to send the
details of the software updates performed offline on a single or a set of
devices. A new distribution set is created based on the information provided.
This distribution set is then assigned to the corresponding targets hence
recording the information as part of their action history. The response of the
API is almost similar to that of creation of a new distribution set.

* API :
    ```
    POST /rest/v1/distributionsets/offlineInstall HTTP 1.1
    Content-Type: multipart/form-data
    ```
* [Postman](https://www.getpostman.com/apps) Example:
    ```
    POST /rest/v1/distributionsets/offlineInstall HTTP/1.1
    Host: 127.0.0.1:8080
    authorization: Basic YWRtaW46YWRtaW4=
    Content-Type: multipart/form-data; boundary=----WebKitFormBoundary7MA4YWxkTrZu0gW
    Cache-Control: no-cache
    Postman-Token: d4302d5c-4fcb-2179-7a71-a60c77f6a76a
    ------WebKitFormBoundary7MA4YWxkTrZu0gW
    Content-Disposition: form-data; name="softwareModuleDataInfo"
    {
        "softwareModules" : [
            {
                "name" : "offlineModule2",
                "description" : "operating system updated offline",
                "artifacts" : [
                    {
                        "filename":"20170530-242.patch",
                        "md5Hash":"a10f3b3bbdf5536550f6ac76e5a26db3",
                        "sha1Hash":"dd31d32801d571c833295a568373b92d81ba2e15",
                        "href":"https://FILESERVER/software/os/updates/20170530-242.patch",
                        "version":"1.0"
                    }
             ],
                "type":"os",
                "version":"1.0.0"
            }
        ],
        "migrationStepRequired" : true,
        "controllerIds" : ["1"]
    }
    ------WebKitFormBoundary7MA4YWxkTrZu0gW
    Content-Disposition: form-data; name="files"; filename=""
    Content-Type:
    ------WebKitFormBoundary7MA4YWxkTrZu0gW
    Content-Disposition: form-data; name="files"; filename=""
    Content-Type:
    ------WebKitFormBoundary7MA4YWxkTrZu0gW--
    ```

* Note : The new distribution set name is generated in the following format and
the version is always 1.0.0:
    ```
    <prefix>_<timestamp>_<uniqueId>
    ```

## Mechanism
1. Get the list of controller ids, for which offline update has to be recorded,
from the request parameter.
2. Checks if targets exist for corresponding controllers ids. If not then throw
appropriate exception.
3. Create the software modules based on the information provided as part of
request parameter `softwareModuleDataInfo`. Upload corresponding artifacts if
files are part of the multipart request, else update only the metadata.
4. Create a distribution set for the software modules. Distribution set's type
is chosen by searching for a type compatible with software module types that are
provided as part of the request.
5. For each controller, create an update action by assigning the distribution
set.
6. Complete all the actions created above by setting their status as 'finished'.
