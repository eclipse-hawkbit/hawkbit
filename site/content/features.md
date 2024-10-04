---
title: Features
weight: 40
---


## Device and Software Repository
- Repository that holds the provisioning targets and assignable software distributions.
- Targets to be logically grouped by Target Types.
- That includes a full software update history for every device.
- Support for pre-commission devices in the repository and plug and play, i.e. device is created if it is authenticated for the first time.

## Update Management
- Directly deploy a defined software distribution to a device (by Management API).
- Update handling is independent of the device type, integration approach or connectivity.
- Optional user consent flow, download and install updates only after respective end user has confirmed it. 
- Mass cancel the distribution of an update by invalidating the distribution set.
- Use action status codes for easier analysis.

## Artifact Content Delivery
- Partial downloads supported.
- Download resume supported (RFC7233).
- Content management by RESTful API and UI (see above).
- Authorization based on software assignment, i.e. a device can only download what has been assigned to it in the first place.
- Delta artifact hosting supported.
- Artifact signature hosting supported.
- Plug-point for artifact encryption allowing to encrypt artifacts on upload.

## Rollout/Campaign Management
- Secure handling of large volumes of devices at rollout creation time.
- Flexible deployment group definition as part of a rollout.
- Monitoring of the rollout progress.
- Emergency rollout shutdown in case of update failures.
- Manually trigger next rollout group.

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

