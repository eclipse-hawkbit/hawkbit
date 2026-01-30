# Direct Device Integration API

The hawkBit [update server](management-api) provides REST resources which are consumed by the device to retrieve software update tasks.  
This API is based on HTTP standards and a polling mechanism.

---

> ℹ️ **Note**: In DDI the target is identified using a **controllerId**.  
> Controller is used as a term for the actual service/client on the device.  
> That allows users to have in some cases even multiple clients on the same target for different tasks, e.g. Firmware update and App management.

---

### State Machine Mapping

For historical reasons the DDI has a different state machine and status messages than the [Target State Machine](targetstate) of the hawkBit update server.

This is kept in order to ensure that *DDI* stays compatible for devices out there in the field.  
A future version “2” of *DDI* might change that.  
*DDI* also defines more states than the update server, e.g. multiple DDI states are currently mapped by the *DDI* implementation to **RUNNING** state.  
It is possible that in the future hawkBit will fully leverage these additional states.

The *DDI* API allows the device to provide the following feedback messages:

| **DDI `status.execution` type** | **Handling by update server** | **Mapped ActionStatus type** |
|--------------------------------|--------------------------------|-------------------------------|
| CANCELED    | This is sent by the target as confirmation of a cancellation request by the update server. | CANCELED |
| REJECTED    | This is sent by the target in case an update or cancellation is rejected, i.e. cannot be fulfilled at this point in time. Note: the target should send a CLOSED→ERROR if it believes it will not be able to proceed the action at all. | WARNING |
| CLOSED      | Target completes the action either with [`status.result.finished`](../status-result-finished.md) SUCCESS or FAILURE as result. Note: DDI defines also a status NONE which will not be interpreted by the update server and handled like SUCCESS. | **ERROR** (DDI FAILURE) or **FINISHED** (DDI SUCCESS or NONE) |
| DOWNLOAD    | This can be used by the target to inform that it is downloading artifacts of the action. | DOWNLOAD |
| DOWNLOADED  | This can be used by the target to inform that it has downloaded artifacts of the action. | DOWNLOADED |
| PROCEEDING  | This can be used by the target to inform that it is working on the action. | RUNNING |
| SCHEDULED   | This can be used by the target to inform that it scheduled the action. | RUNNING |
| RESUMED     | This can be used by the target to inform that it continued to work on the action. | RUNNING |

---

See this [issue](https://github.com/eclipse-hawkbit/hawkbit/issues/952) for additional information, concerning the cancellation of updates.  
To finally accept a cancellation, you must send a `closed` status.execution type.
 
## REST Doc
<div style="text-align: right; margin-bottom: 8px;">
<button onclick="window.open('../rest-api/ddi.html', '_blank')" 
        style="padding:8px 16px; border-radius:6px; border:1px solid #007bff; 
               background:#007bff; color:#fff; cursor:pointer;">
  Open in new window
</button>
</div>

<iframe 
  src="rest-api/ddi.html" 
  width="100%" 
  height="1080px" 
  style="border: 1px solid #000000; border-radius: 16px;">
</iframe>
