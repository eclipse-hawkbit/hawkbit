---
layout: documentation
title: Features
---

{% include base.html %}

# Feature overview

### Device and software repository
- Repository that holds the provisioning targets and assignable software distributions.
- That includes a full software update history for every device.
- Support for pre-commission devices in the repository and plug and play, i.e. device is created if it is authenticated for the first time.

### Update Management
- Directly deploy a defined software distribution to a device (by Management UI or API).
- Update handling is independent of the device type, integration approach or connectivity.

### Management UI/Console
- Create/Read/Update/Delete operations for provisioning targets (i.e. devices) and repository content (i.e. software).
- Manage and monitor software update operations.
- Optimized for professional users, e.g. administrators, developers and 2nd/3rd level support staff.
- Ease of use drag-and-drop paradigm.
- Flexible grouping of data.
- Flexible filters for data browsing.
- Responsive to resolution.
- Lazy loading of data.
- All information on one page.
- Optional integration with Bosch IoT Permissions service for full multi user support with fine granular permission based authorization.

### Artifact Content Delivery
- Partial downloads supported.
- Download resume supported (RFC7233).
- Content management by RESTful API and UI (see above).
- Authorization based on software assignment, i.e. a device can only download what has been assigned to it in the first place.
- Delta artifact hosting supported.
- Artifact signature hosting supported.

### Rollout/Campaign Management
- Secure handling of large volumes of devices at rollout creation time.
- Flexible deployment group definition as part of a rollout.
- Monitoring of the rollout progress.
- Emergency rollout shutdown in case of update failures.

## Interfaces
### Management API
- RESTful API
- Create/Read/Update/Delete operations for provisioning targets (i.e. devices) and repository content (i.e. software).
- Manage and monitor software update operations.
- Online API documentation.
- JSON payload with Hypermedia support.
- Supports filtering, sorting and paging.

### Direct Device Integration API
- RESTful HTTP based API for direct device integration
    - JSON payload.
    - Traffic optimized (content based Etag generation, not modified).
- Feedback channel from device.
- TLS encryption.

### Device Management Federation API
- Indirect device integration through a device management service or application into hawkBit.
- Optimized for high service to service throughput with [AMQP](https://www.rabbitmq.com/amqp-0-9-1-reference.html) messaging interface.
- Separate AMQP vHost per tenant for maximum security.

