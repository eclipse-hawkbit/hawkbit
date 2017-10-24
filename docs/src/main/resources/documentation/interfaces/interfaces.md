---
layout: documentation
title: Interfaces
---

{% include base.html %}

![](../images/hawkBit_overview.jpeg){:width="100%"}


# Graphical User Interface

To get started _hawkBit_ offers a [Management UI](management-ui.html) that allows operators to manage the repository and trigger provisioning operations.

In addition Eclipse _hawkBit_ offers developers multiple options to integrate.

# Application Integration
The _hawkBit_ [Management API](management-api.html) allows applications to manage the repository and trigger provisioning operations. It is in general feature compliant with the _Management UI_. However, small differences may occur here and there. The authentication and authorization structure is identical, i.e. a user can login both at Management API and UI with the same credentials and has the same permissions available.

# Device Integration
For device integration two options exist.

The [Direct Device Integration API](ddi-api.html) allows direct integration from the device to the _hawkBit_ server. It has been designed with simplicity in mind as its is fully focused on software update. It allows device integrators to separate concerns by means of having distinguished channels for business data and general device management tasks on one side and software update on the other. As a result it is possible to keep the _lifesaving_ provisioning process controller on the device separate from the more complex business functionality. A benefit of such an architecture should not be underestimated.

As result of such a simple HTTP/REST/JSON based API even a major back-end migration or disaster can be covered with simple web server hosting a text file that contains only the command to update one more time to execute a migration on the device. The API was designed on purpose in way to have that last resort even if the plan is that this will never be necessary.

The [Device Management Federation API](dmf-api.html) however allows to combine the business data and _hawkBit_ connectivity. This is especially usefull if a constrained device cannot handle a TLS/HTTP connection, is supporting a standard device management protocol that covers also the software update part (e.g. TR-069, OMA-DM, LWM2M) or the device is already connected and _hawkBit_ is introduced later on.

The decision for the right device integration path is up to the integration party.
