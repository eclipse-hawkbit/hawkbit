---
title: Device Management Federation API
parent: API
weight: 83
---

The DMF API provides Java classes which allows that the message body can be deserialized at runtime into a Java object. Also Java classes can be used to serialize Java objects into JSON bodies to send a message to hawkBit.
Currently, bodies of messages are based on JSON.

<!--more-->

## Basics

There are three basic concepts of AMQP:

- Exchanges - what you publish to.
- Queues - what you consume from.
- Bindings - configuration that maps an exchange to a queue.

**Queues** are just a place for receiving messages.  
Bindings determine how messages get put in this place
Queues can also be bound to multiple exchanges.

**Exchanges** are just publish messages.
The user decides who can produce on an exchange and who can create bindings on that exchange for delivery to a specific queue.

hawkBit will create all necessary queues, exchanges and bindings for the user, making it easy to get started.
The exchange name for outgoing messages is **dmf.exchange**.

The user has to set a `reply_to` header (see chapter below), in order to specify the exchange to which hawkBit should reply to.

The following chapter describes the message body, header and properties.

Note: the DMF protocol was intended to be compatible to other use cases by design. As a result, DMF uses the term **thing** and not **target** but they are actually synonyms in this case.

## Messages sent to hawkBit (Client -> hawkBit)


### THING_CREATED

Message to register and update a provisioning target.

| Header  | Description                                    | Type                         | Mandatory |
|---------|------------------------------------------------|------------------------------|-----------|
| type    | Type of the message                            | Fixed string "THING_CREATED" | true      |
| thingId | The ID of the registered provisioning target   | String                       | true      |
| tenant  | The tenant this provisioning target belongs to | String                       | true      |
| sender  | Name of the message sender                     | String                       | false     |

| Message Properties | Description                     | Type   | Mandatory |
|--------------------|---------------------------------|--------|-----------|
| content_type       | The content type of the payload | String | true      |
| reply_to           | Exchange to reply to            | String | true      |

Example headers and payload:

| Header                                                                               | MessageProperties                                                  |
|--------------------------------------------------------------------------------------|--------------------------------------------------------------------|
| type=THING\_CREATED <br /> tenant=default <br /> thingId=abc  <br /> sender=myClient | content\_type=application/json <br /> reply_to=myExchangeToReplyTo |

Payload Template (optional):

```json
{
    "name": "String",
    "attributeUpdate": {
        "attributes": {
            "exampleKey1" : "exampleValue1",
            "exampleKey2" : "exampleValue2"
        },
        "mode": "String"
    }
}
```

The "name" property specifies the name of the thing, which by default is the thing ID. This property is optional.<br />
The "attributeUpdate" property provides the attributes of the thing, for details see UPDATE_ATTRIBUTES message. This property is optional.


### THING_REMOVED

Message to request the deletion of a provisioning target.

| Header  | Description                                    | Type                         | Mandatory |
|---------|------------------------------------------------|------------------------------|-----------|
| type    | Type of the message                            | Fixed string "THING_REMOVED" | true      |
| thingId | The ID of the registered provisioning target   | String                       | true      |
| tenant  | The tenant this provisioning target belongs to | String                       | false     |

| Message Properties | Description                     | Type   | Mandatory |
|--------------------|---------------------------------|--------|-----------|
| content_type       | The content type of the payload | String | true      |

Example headers

| Header                                                       | MessageProperties              |
|--------------------------------------------------------------|--------------------------------|
| type=THING\_REMOVED <br /> tenant=default <br /> thingId=abc | content\_type=application/json |

### UPDATE_ATTRIBUTES

Message to update target attributes. This message can be send in response to a REQUEST_ATTRIBUTES_UPDATE event, sent by hawkBit.

| Header  | Description                      | Type                             | Mandatory |
|---------|----------------------------------|----------------------------------|-----------|
| type    | Type of the message              | Fixed string "EVENT"             | true      |
| topic   | Topic name identifying the event | Fixed string "UPDATE_ATTRIBUTES" | true      |
| thingId | The ID of the registered thing   | String                           | true      |
| tenant  | The tenant this thing belongs to | String                           | false     |

| Message Properties          | Description                      | Type   | Mandatory |
|-----------------------------|----------------------------------|--------|-----------|
| content_type                | The content type of the payload  | String | true      |

Example header and payload:

| Header                                                                               | MessageProperties                     |
|--------------------------------------------------------------------------------------|---------------------------------------|
| type=EVENT <br /> tenant=default <br /> thingId=abc  <br /> topic=UPDATE\_ATTRIBUTES | content\_type=application/json <br /> |

Payload Template:

```json
{
    "attributes": {
        "exampleKey1" : "exampleValue1",
        "exampleKey2" : "exampleValue2"
    },
    "mode": "String"
}
```

The "mode" property specifies the update mode that should be applied. This property is optional. Possible [mode](https://github.com/eclipse/hawkbit/tree/master/hawkbit-dmf/hawkbit-dmf-api/src/main/java/org/eclipse/hawkbit/dmf/json/model/DmfUpdateMode.java) values:

| Value   | Description                                                                                                                                                                         |
|---------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| MERGE   | The target attributes specified in the payload are merged into the existing attributes. This is the default mode that is applied if no "mode" property is specified in the payload. |
| REPLACE | The existing attributes are replaced with the target attributes specified in the payload.                                                                                           |
| REMOVE  | The target attributes specified in the payload are removed from the existing attributes.                                                                                            |

### UPDATE_ACTION_STATUS

Message to send an action status event to hawkBit.

| Header | Description                      | Type                                | Mandatory |
|--------|----------------------------------|-------------------------------------|-----------|
| type   | Type of the message              | Fixed string "EVENT"                | true      |
| topic  | Topic name identifying the event | Fixed string "UPDATE_ACTION_STATUS" | true      |
| tenant | The tenant this thing belongs to | String                              | false     |

| Message Properties | Description                     | Type   | Mandatory |
|--------------------|---------------------------------|--------|-----------|
| content_type       | The content type of the payload | String | true      |

Payload Template (the Java representation is [ActionUpdateStatus](https://github.com/eclipse/hawkbit/tree/master/hawkbit-dmf/hawkbit-dmf-api/src/main/java/org/eclipse/hawkbit/dmf/json/model/DmfActionUpdateStatus.java)):

```json
{
  "actionId": long,
  "softwareModuleId": long,
  "actionStatus":"String",
  "message":["String"]
}
```

Possible [actionStatus](https://github.com/eclipse/hawkbit/tree/master/hawkbit-dmf/hawkbit-dmf-api/src/main/java/org/eclipse/hawkbit/dmf/json/model/DmfActionStatus.java) values:

| Value           | Description                             |
|-----------------|-----------------------------------------|
| DOWNLOAD        | Device is downloading                   |
| DOWNLOADED      | Device completed download               |
| RETRIEVED       | Device has retrieved the artifact       |
| RUNNING         | Update is running                       |
| FINISHED        | Update process finished successful      |
| ERROR           | Error during update process             |
| WARNING         | Warning during update process           |
| CANCELED        | Cancel update process successful        |
| CANCEL_REJECTED | Cancel update process has been rejected |

Example header and payload:

| Header                                                                | MessageProperties             |
|-----------------------------------------------------------------------|-------------------------------|
| type=EVENT  <br /> tenant=default <br /> topic=UPDATE\_ACTION\_STATUS | content_type=application/json |

```json
{
  "actionId":137,
  "softwareModuleId":17,
  "actionStatus":"DOWNLOAD",
  "message":["The download has started"]
}
```

### PING

hawkBit allows DMF clients to check the availability of the DMF service. For this scenario DMF specifies a PING message that can be sent by the client:

| Header | Description                    | Type                | Mandatory |
|--------|--------------------------------|---------------------|-----------|
| type   | Type of the message            | Fixed string "PING" | true      |
| tenant | The tenant the PING belongs to | String              | false     |

| Message Properties | Description                                                                 | Type   | Mandatory |
|--------------------|-----------------------------------------------------------------------------|--------|-----------|
| correlationId      | CorrelationId that allows the client to map a PING request to PING_RESPONSE | String | true      |

## Messages sent by hawkBit (hawkBit -> Client)

### CANCEL_DOWNLOAD

Message to cancel an update task.

| Header  | Description                                    | Type                           | Mandatory |
|---------|------------------------------------------------|--------------------------------|-----------|
| type    | Type of the message                            | Fixed string "Event"           | true      |
| thingId | The ID of the registered provisioning target   | String                         | true      |
| topic   | Topic name identifying the event               | Fixed string "CANCEL_DOWNLOAD" | true      |
| tenant  | The tenant this provisioning target belongs to | String                         | false     |

| Message Properties | Description                     | Type   | Mandatory |
|--------------------|---------------------------------|--------|-----------|
| content_type       | The content type of the payload | String | true      |

Payload template:

```json
{
    "actionId": long
}
```

Example Headers and Payload:

| Header                                                                             | MessageProperties             |
|------------------------------------------------------------------------------------|-------------------------------|
| type=EVENT <br /> tenant=default <br /> thingId=abc  <br /> topic=CANCEL\_DOWNLOAD | content_type=application/json |

```json
{
"actionId":137
}
```

After sending this message, an action status event with either actionStatus=CANCELED or actionStatus=CANCEL_REJECTED has to be returned.

Example header and payload when cancellation is successful:

| Header                                                                | MessageProperties             |
|-----------------------------------------------------------------------|-------------------------------|
| type=EVENT  <br /> tenant=default <br /> topic=UPDATE\_ACTION\_STATUS | content_type=application/json |

```json
{
  "actionId":137,
  "softwareModuleId":17,
  "actionStatus":"CANCELED",
  "message":["The update was canceled."]
}
```

Example header and payload when cancellation is rejected:

| Header                                                                | MessageProperties             |
|-----------------------------------------------------------------------|-------------------------------|
| type=EVENT  <br /> tenant=default <br /> topic=UPDATE\_ACTION\_STATUS | content_type=application/json |

```json
{
  "actionId":137,
  "softwareModuleId":17,
  "actionStatus":"CANCEL_REJECTED",
  "message":["The cancellation was not possible since the target sent an unexpected response."]
}
```


### DOWNLOAD_AND_INSTALL or DOWNLOAD

Message sent by hawkBit to initialize an update or download task. Note: in case of a maintenance window configured but not yet active the message will have the topic _DOWNLOAD_ instead of _DOWNLOAD_AND_INSTALL_.

| Header  | Description                                    | Type                                              | Mandatory |
|---------|------------------------------------------------|---------------------------------------------------|-----------|
| type    | Type of the message                            | Fixed string "EVENT"                              | true      |
| thingId | The ID of the registered provisioning target   | String                                            | true      |
| topic   | Topic name identifying the event               | Fixed string "DOWNLOAD_AND_INSTALL" or "DOWNLOAD" | true      |
| tenant  | The tenant this provisioning target belongs to | String                                            | false     |

| Message Properties | Description                     | Type   | Mandatory |
|--------------------|---------------------------------|--------|-----------|
| content_type       | The content type of the payload | String | true      |

Payload Template (the Java representation is [DmfDownloadAndUpdateRequest](https://github.com/eclipse/hawkbit/tree/master/hawkbit-dmf/hawkbit-dmf-api/src/main/java/org/eclipse/hawkbit/dmf/json/model/DmfDownloadAndUpdateRequest.java)):

```json
{
"actionId": long,
"targetSecurityToken": "String",
"softwareModules":[
    {
    "moduleId": long,
    "moduleType":"String",
    "moduleVersion":"String",
    "artifacts":[
        {
        "filename":"String",
        "urls":{
            "HTTP":"String",
            "HTTPS":"String"
            },
        "hashes":{
            "md5":"String",
            "sha1":"String"
            },
        "size":long
        }],
    "metadata":[
        {
            "key":"String",
            "value":"String"
        }
    ]
    }]
}
```

Example header and payload:

| Header                                                                                    | MessageProperties             |
|-------------------------------------------------------------------------------------------|-------------------------------|
| type=EVENT  <br /> tenant=default <br /> thingId=abc  <br /> topic=DOWNLOAD\_AND\_INSTALL | content_type=application/json |

```json
{
"actionId":137,
"targetSecurityToken":"bH7XXAprK1ChnLfKSdtlsp7NOlPnZAYY",
"softwareModules":[
    {
    "moduleId":7,
    "moduleType":"firmware",
    "moduleVersion":"7.7.7",
    "artifacts":[
        {
        "filename":"artifact.zip",
        "urls":{
            "HTTP":"http://download-from-url.com",
            "HTTPS":"https://download-from-url.com"
            },
        "hashes":{
            "md5":"md5hash",
            "sha1":"sha1hash"
            },
        "size":512
        }],
    "metadata":[
        {
            "key":"installationType",
            "value":"5784K#"
        }
    ]
    }]
}
```


### MULTI_ACTION

If `multi.assignments.enabled` is enabled, this message is sent instead of DOWNLOAD_AND_INSTALL, DOWNLOAD, or CANCEL_DOWNLOAD
 by hawkBit to initialize update, download, or cancel task(s).

 With weight, one can set the priority to the action. The higher the weight, the higher is the priority of an action.

| Header  | Description                                    | Type                        | Mandatory |
|---------|------------------------------------------------|-----------------------------|-----------|
| type    | Type of the message                            | Fixed string "EVENT"        | true      |
| thingId | The ID of the registered provisioning target   | String                      | true      |
| topic   | Topic name identifying the event               | Fixed string "MULTI_ACTION" | true      |
| tenant  | The tenant this provisioning target belongs to | String                      | false     |

| Message Properties | Description                     | Type   | Mandatory |
|--------------------|---------------------------------|--------|-----------|
| content_type       | The content type of the payload | String | true      |

Payload Template (the Java representation is [DmfMultiActionRequest](https://github.com/eclipse/hawkbit/tree/master/hawkbit-dmf/hawkbit-dmf-api/src/main/java/org/eclipse/hawkbit/dmf/json/model/DmfMultiActionRequest.java)):

```json
[{
"topic": "String",
"weight": long,
"action": {
  "actionId": long,
  "targetSecurityToken": "String",
  "softwareModules":[
      {
      "moduleId": long,
      "moduleType":"String",
      "moduleVersion":"String",
      "artifacts":[
          {
          "filename":"String",
          "urls":{
              "HTTP":"String",
              "HTTPS":"String"
              },
          "hashes":{
              "md5":"String",
              "sha1":"String"
              },
          "size":long
          }],
      "metadata":[
          {
              "key":"String",
              "value":"String"
          }
      ]
      }]
  }
},
{
"topic": "String",
"weight": long,
"action": {
  "actionId": long,
  "targetSecurityToken": "String",
  "softwareModules":[
      {
      "moduleId": long,
      "moduleType":"String",
      "moduleVersion":"String",
      "artifacts":[
          {
          "filename":"String",
          "urls":{
              "HTTP":"String",
              "HTTPS":"String"
              },
          "hashes":{
              "md5":"String",
              "sha1":"String"
              },
          "size":long
          }],
      "metadata":[
          {
              "key":"String",
              "value":"String"
          }
      ]
      }]
  }
}]
```

Example header and payload:

| Header                                                                           | MessageProperties             |
|----------------------------------------------------------------------------------|-------------------------------|
| type=EVENT  <br /> tenant=default <br /> thingId=abc  <br /> topic=MULTI\_ACTION | content_type=application/json |

```json
[{
"topic": "DOWNLOAD_AND_INSTALL",
"weight": 600,
"action": {
  "actionId":137,
  "targetSecurityToken":"bH7XXAprK1ChnLfKSdtlsp7NOlPnZAYY",
  "softwareModules":[
      {
      "moduleId":7,
      "moduleType":"firmware",
      "moduleVersion":"7.7.7",
      "artifacts":[
          {
          "filename":"artifact.zip",
          "urls":{
              "HTTP":"http://download-from-url.com",
              "HTTPS":"https://download-from-url.com"
              },
          "hashes":{
              "md5":"md5hash",
              "sha1":"sha1hash"
              },
          "size":512
          }],
      "metadata":[
          {
              "key":"installationType",
              "value":"5784K#"
          }
      ]
      }]
  }
},
{
"topic": "DOWNLOAD",
"weight": 500,
"action": {
  "actionId":138,
  "targetSecurityToken":"bH7XXAprK1ChnLfKSdtlsp7NOlPnZAYY",
  "softwareModules":[
      {
      "moduleId":4,
      "moduleType":"firmware",
      "moduleVersion":"7.7.9",
      "artifacts":[
          {
          "filename":"artifact.zip",
          "urls":{
              "HTTP":"http://download-from-url.com",
              "HTTPS":"https://download-from-url.com"
              },
          "hashes":{
              "md5":"md5hash",
              "sha1":"sha1hash"
              },
          "size":512
          }],
      "metadata":[
          {
              "key":"installationType",
              "value":"5784K#"
          }
      ]
      }]
  }
}]
```


### THING_DELETED

Message sent by hawkBit when a target has been deleted.

| Header  | Description                                    | Type                         | Mandatory |
|---------|------------------------------------------------|------------------------------|-----------|
| type    | Type of the message                            | Fixed string "THING_DELETED" | true      |
| thingId | The ID of the registered provisioning target   | String                       | true      |
| tenant  | The tenant this provisioning target belongs to | String                       | true      |

Example header:

| Header                                                       | MessageProperties |
|--------------------------------------------------------------|-------------------|
| type=THING\_DELETED <br /> tenant=default <br /> thingId=abc |                   |


### REQUEST_ATTRIBUTES_UPDATE

Message sent by Eclipse hawkBit when a re-transmission of target attributes is requested.

| Header  | Description                                    | Type                                     | Mandatory |
|---------|------------------------------------------------|------------------------------------------|-----------|
| type    | Type of the message                            | Fixed string "EVENT"                     | true      |
| thingId | The ID of the registered provisioning target   | String                                   | true      |
| topic   | Topic name identifying the event               | Fixed string "REQUEST_ATTRIBUTES_UPDATE" | true      |
| tenant  | The tenant this provisioning target belongs to | String                                   | true      |

Example headers:

| Header                                                                                       | MessageProperties |
|----------------------------------------------------------------------------------------------|-------------------|
| type=EVENT <br /> tenant=default <br /> thingId=abc <br /> topic=REQUEST\_ATTRIBUTES\_UPDATE |                   |


### PING_RESPONSE

_hawkBit_ will respond to the PING message with a PING_RESPONSE type message that has the same correlationId as the original PING message:

| Header | Description                    | Type                         | Mandatory |
|--------|--------------------------------|------------------------------|-----------|
| type   | Type of the message            | Fixed string "PING_RESPONSE" | true      |
| tenant | The tenant the PING belongs to | String                       | false     |

| Message Properties | Description                                | Type   | Mandatory |
|--------------------|--------------------------------------------|--------|-----------|
| correlationId      | CorrelationId of the original PING request | String | true      |
| content_type       | The content type of the payload            | String | true      |

The PING_RESPONSE also contains a timestamp (i.e. the difference, measured in milliseconds, between the current time and midnight, January 1, 1970 UTC) as plain text. It is not guaranteed that this timestamp is completely accurate.

| Header                                    | MessageProperties       |
|-------------------------------------------|-------------------------|
| type=PING_RESPONSE  <br /> tenant=default | content_type=text/plain |

```text
1505215891247
```
