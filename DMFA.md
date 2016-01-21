# !!! DRAFT !!! #
# Device Management Federation API #

This document describes the **DMF API**. This API is based on AMQP [(AMQP-Specification)](ht1tp://www.amqp.org/sites/amqp.org/files/amqp.pdf "AMQP-Specification"). The SP Update server provides a message queue (default: "*dmf_receiver*") on the corresponding Rabbit MQ service, which accepts all incoming messages.

Every Service, which wants to communicate, has to offer a own queue, to which the SP update server can send messages. The name of this queue is sent 

## Authentication ##
???

## Regular update process ##

### Step 1: Creating a Thing on SP ###

At the beginning every connecting service has to register its targets at the Sp update server. During the creating process the service has to send the name of the queue, which accepts messages, which are sent back from the Sp update server. 

The message which is sent by the connecting service to the SP update service has to fulfill the following format. The connecting service has to set the attributes in the header. The name of the tenent (*tenant*, an unique identifier of the thing/target (*thingId*) and an string which identifies the sending service (*sender*). Furthermore it has to set the *replyTo*-Property which defines the name of the queue, which receives messages sent by the SP Update Server. The body of the message is not analyzed and can be empty.

**Header**
<pre>
type=THING_CREATED
tenant=<b>#TENANTNAME#</b>
thingId=<b>#ExampleDevice001#</b>
sender=<b>#CONNECTORSERVICE#</b>
</pre>
**Properties**
<pre>
contentType=application/json
replyTo=<b>#queue.which.accepts.messages.from.sp#</b>
</pre>
**Example Body**
<pre> </pre>

### Step 2: Begin of update process ###
Afterwards an distribution set can be assigned to the created thing. This can be done on the UI or with the REST API. To start the update process the SP update server sends a message over the registered queue back to the connecting service. 

Inside the message send from the Sp update service the header specifies the target which has to be updated. Therefor the *tenent* and the unique *thingId* are used. The body contains an identifier which defines the update process (*actionId*) and the information of the software modules. The *actionId* is important to send the progress of the update process to the server. The information about the software describes all modules which are included in the current update process, their identifier, their type, their version and the corresponding artifacts with the urls where the target can download the artifacts.

**Header**
<pre>
type=EVENT 
tenant=<b>#TENANTNAME#</b>
thingId=<b>#ExampleDevice001#</b>
topic=DOWNLOAD_AND_INSTALL
</pre>
**Properties**
<pre>
contentType=application/json
</pre>
**Example Body**
<pre>
{
"actionId":172,
"softwareModules":[
    {
    "moduleId":6,
    "moduleType":"firmware",
    "moduleVersion":"7.7.7",
    "artifacts":[
        {
        "urls":{
            "coap":"coap://bumlux.test",
            "http":"coap://bumlux.test"}
        }]
    }]
}
</pre>

### Step 3: Sending the state update process <a name="sendingactionstate"></a>###

During the update progress the connected service can inform the sp update service about the progress and eventual warnings or errors. Therefore the *actionId* and the *softwareModuleId* are used to specify for which process and for which software module the progress update is for. Additionally in the Body there is a field *actionStatus*. This field is used to describe the type of the progress update and there are several allowed values:

- **RETRIEVED**: general acknowledge message 
- **DOWNLOAD**: describes all messages related to the download prorgess
- **RUNNING**: 
- **WARNING**: A smaller problem which influence the update process, but no need for stop it.
- **ERROR**: A big issues which not allows continuing the update process. 
- **FINISHED**: Successfully finished the update process.
- **CANCELED**: Response to a cancel request, when the process is canceled.
- **CANCEL_REJECTED**: Response to a cancel request, when canceling is not possible.

The update process is marked as running until the connected Server sends a **FINISHED**, an **ERROR** or a **CANCELED** message. Additionally the connected service can put some human readable information into the message, which describes the state of the action more detailed.

**Header**
<pre>
type=EVENT 
tenant=<b>#TENANTNAME#</b>
topic=UPDATE_ACTION_STATUS
</pre>
**Properties**
<pre>
contentType=application/json
</pre>
**Example Body**

<pre>
{
"actionId":115,
"softwareModuleId":3,
"actionStatus":"DOWNLOAD",
"message":["The download has started"]
}
</pre>

## Cancel process ##

During the update process the SP update server has the chance to cancel an update. Therefore it sends the following message. This message includes the *tenant* and the corresponding *thingId* on theheader.

**Header**
<pre>
type=EVENT
tenant=<b>#TENANTNAME#</b>
thingId=<b>#ExampleDevice001#</b>
topic=CANCEL_DOWNLOAD 
</pre>
**Properties**
<pre>
contentType=application/json
</pre>
**Example Body**
<pre>

</pre>

Receiving this message the connected service has to decide if an update can be canceled or not. If it is the update has to be canceled and a **CANCELED** message has to be sent to the SP server. Otherwise a **CANCEL_REJECTED** message has to be sent.

 ![](src/images/dmfa_cancel_response.png)