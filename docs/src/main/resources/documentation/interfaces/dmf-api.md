---
layout: documentation
title: DMF-API
---

{% include base.html %}

Currently bodies of messages are based on JSON. The DMF API provides java classes which allows that the message body can be deserialized at runtime into a java object. Also java classes can be used to serialize java objects into JSON bodies to send a message to _hawkBit_.

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

_hawkBit_ will create all necessary queues, exchanges and bindings for the user, making it easy to get started.
The exchange name for outgoing messages is **dmf.exchange**.
The queue name for incoming messages is **sp_direct_queue**. Unless a ``reply_to`` header is set, _hawkBit_ will reply on the **sp.direct.exchange** which is bound to the **sp_direct_queue**.

The user can set a ``reply_to`` header (see chapter below), to change the default sender exchange.

The following chapter describes the message body, header and properties.

Note: the DMF protocol was intended to be open for other non update use cases by design (e.g. [Eclipse Hono](https://github.com/eclipse/hono). As a result, DMF uses the term **thing** and not **target** but they are actually synonyms in this case.

## Messages sent to _hawkBit_
All messages have to be sent to the exchange **dmf.exchange**.

### Message to register a thing

| Message Header                      | Description                      | Type                                | Mandatory                                                    
|-----------------------------|----------------------------------|-------------------------------------|----------------
| type          | Type of the message              | Fixed string "THING_CREATED "       | true
| thingId       | The ID of the registered thing   | String                              | true
| sender        | Name of the message sender       | String                              | false
| tenant        | The tenant this thing belongs to | String                              | false


| Message Properties                      | Description                      | Type                                | Mandatory                                                    
|-----------------------------|----------------------------------|-------------------------------------|----------------
| content_type                 | The content type of the payload  | String                              | true
| reply_to                     | Exchange to reply to. The default is sp.direct.exchange which is bound to the sp_direct_queue             | String                              | false

**Example Header**

| Headers                               | MessageProperties               |                                                              
|---------------------------------------|---------------------------------|
| type=THING\_CREATED <br /> tenant=tenant123 <br /> thingId=abc  <br /> sender=Lwm2m   | content\_type=application/json <br /> reply_to (optional) =sp.connector.replyTo  


### Message to update target attributes

| Message Header                      | Description                      | Type                                | Mandatory                                                    
|-----------------------------|----------------------------------|-------------------------------------|----------------
| type          | Type of the message              | Fixed string "EVENT"       | true
| topic         | Topic to handle events different | Fixed string "UPDATE_ATTRIBUTES" | true
| thingId       | The ID of the registered thing   | String                              | true
| tenant        | The tenant this thing belongs to | String                              | false


| Message Properties                      | Description                      | Type                                | Mandatory                                                    
|-----------------------------|----------------------------------|-------------------------------------|----------------
| content_type                 | The content type of the payload  | String                              | true


**Example Header and Payload**

| Headers                               | MessageProperties               |                                                              
|---------------------------------------|---------------------------------|
| type=EVENT <br /> tenant=tenant123 <br /> thingId=abc  <br /> topic=UPDATE\_ATTRIBUTES | content\_type=application/json <br /> 

Payload Template

```json
{
	"attributes": {
		"exampleKey1" : "exampleValue1",
		"exampleKey2" : "exampleValue2"
	}
}
```

### Message to send an action status event to _hawkBit_

The Java representation is ActionUpdateStatus:

| Header                      | Description                      | Type                                | Mandatory                                                    
|-----------------------------|----------------------------------|-------------------------------------|--------------
| type          | Type of the message              | Fixed string "EVENT"                | true
| topic         | Topic to handle events different | Fixed string "UPDATE_ACTION_STATUS" | true
| tenant        | The tenant this thing belongs to | String                              | false

| Message Properties                      | Description                      | Type                                | Mandatory                                                    
|-----------------------------|----------------------------------|-------------------------------------|----------------
| content_type                 | The content type of the payload  | String                              | true

Payload Template

```json
{
"actionId": long,
"softwareModuleId": long,
"actionStatus":"String",
"message":["String"]
}
```
Possible actionStatus

| Header          | Description                        |                                                
|-----------------|------------------------------------|
| DOWNLOAD        | Device is downloading               |
| RETRIEVED       | Device management service has retrieved something          |
| RUNNING         | Update is running                  |
| FINISHED        | Update process finished successful |
| ERROR           | Error during update process        |
| WARNING         | Warning during update process      |
| CANCELED        | Cancel update process successful     |
| CANCEL_REJECTED | Cancel update process has been rejected      |


**Example Header and Payload**

| Headers                               | MessageProperties               |                                                                           
|---------------------------------------|---------------------------------|
| type=EVENT  <br /> tenant=tenant123 <br /> topic=UPDATE\_ACTION\_STATUS   | content_type=application/json  

```json
{
"actionId":137,
"softwareModuleId":17,
"actionStatus":"DOWNLOAD",
"message":["The download has started"]
}
```

### Message to cancel an update task

| Header                      | Description                      | Type                                | Mandatory                                                    
|-----------------------------|----------------------------------|-------------------------------------|----------------
| type          | Type of the message              | Fixed string "Event"                | true
| thingId       | The ID of the registered thing   | String                              | true
| topic         | Topic to handle events different | Fixed string "CANCEL_DOWNLOAD"      | true
| tenant        | The tenant this thing belongs to | String                              | false

| Message Properties                      | Description                      | Type                                | Mandatory                                                    
|-----------------------------|----------------------------------|-------------------------------------|----------------
| content_type                 | The content type of the payload  | String                              | true

**Example Header**

| Headers                               | MessageProperties               |                                                              
|---------------------------------------|---------------------------------|
| type=EVENT <br /> tenant=tenant123 <br /> thingId=abc  <br /> topic=CANCEL\_DOWNLOAD   | content_type=application/json  


After this message has been sent, an action status event with either actionStatus=CANCELED or actionStatus=CANCEL_REJECTED has to be returned.

**Example Header and Payload when cancellation is successful**

| Headers                               | MessageProperties               |                                                                           
|---------------------------------------|---------------------------------|
| type=EVENT  <br /> tenant=tenant123 <br /> topic=UPDATE\_ACTION\_STATUS   | content_type=application/json  

```json
{
"actionId":137,
"softwareModuleId":17,
"actionStatus":"CANCELED",
"message":["The update was canceled."]
}
```

**Example Header and Payload when cancellation was rejected**

| Headers                               | MessageProperties               |                                                                           
|---------------------------------------|---------------------------------|
| type=EVENT  <br /> tenant=tenant123 <br /> topic=UPDATE\_ACTION\_STATUS   | content_type=application/json  

```json
{
"actionId":137,
"softwareModuleId":17,
"actionStatus":"CANCEL_REJECTED",
"message":["The cancellation was not possible since the target sent an unexpected response."]
}
```
## Messages sent by _hawkBit_
All messages from _hawkBit_ will be sent to the **sp_direct_queue** or the one specified in the ``reply_to`` property.

### Message sent by _hawkBit_ to initialize an update task


| Header                      | Description                      | Type                                | Mandatory                                                    
|-----------------------------|----------------------------------|-------------------------------------|----------------
| type          | Type of the message              | Fixed string "EVENT"                | true
| thingId       | The ID of the registered thing   | String                              | true
| topic         | Topic to handle events different | Fixed string "DOWNLOAD_AND_INSTALL" | true
| tenant        | The tenant this thing belongs to | String                              | false


| Message Properties                      | Description                      | Type                                | Mandatory                                                    
|-----------------------------|----------------------------------|-------------------------------------|----------------
| content_type                 | The content type of the payload  | String                              | true

The Java representation is DownloadAndUpdateRequest:


Payload Template

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

**Example Header and Payload**

| Headers                               | MessageProperties               |                                                                           
|---------------------------------------|---------------------------------|
| type=EVENT  <br /> tenant=tenant123 <br /> thingId=abc  <br /> topic=DOWNLOAD\_AND\_INSTALL   | content_type=application/json  

```json
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