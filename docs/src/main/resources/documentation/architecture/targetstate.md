---
layout: documentation
title: Target State Machine
---

{% include base.html %}

# Target States

A target has a current state which reflects the provisioning status of the device at this point in time. State changes are driven either by the update server by means of starting an update or by the controller on the provisioning target that gives feedback to the update server, e.g. "I am here", "I am working on a provisioning", "I have finished a provisioning".

# Defined states

State      | Description
---------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------
UNKNOWN    | Set by default for a pre-commissioned target until first status update received from the target. Is the initial starting point for targets created by UI or management API.
IN_SYNC    | Assigned _Distribution Set_ is installed.
PENDING    | Installation of assigned _Distribution Set_ is not yet confirmed.
ERROR      | Installation of assigned _Distribution Set_ has failed.
REGISTERED | Target registered at the update server but no _Distribution Set_ assigned. Is the initial starting point for plug-and-play devices.

# Transitions
![](../images/architecture/targetStatusStates.png){:width="100%"}
