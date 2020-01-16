---
title: Authentication
parent: Concepts
weight: 51
---

A hawkBit update server can be accessed in four different ways:

- _Direct Device Integration (DDI) API_ by **targets**.
- _Management API_ by 3rd party **applications**.
- _Device Management Federation (DMF) API_ by 3rd party **applications** through AMQP.
- _Management UI_ by **users**.
<!--more-->


## DDI API Authentication Modes

### Security Token

hawkBit supports multiple ways to authenticate a target against the server. The different authentication modes can be individual enabled and disabled within hawkBit. Both on system level (with Spring Boot properties) as per individual tenant.

#### Target Security Token Authentication
There is a 32 alphanumeric character security-token for each created target within IoT hawkBit. This token can be used to authenticate the target at hawkBit through the HTTP-Authorization header with the custom scheme _TargetToken_.

```
GET /SPDEMO/controller/v1/0e945f95-9117-4500-9b0a-9c6d72fa6c07 HTTP/1.1
Host: your.hawkBit.server
Authorization: TargetToken bH7XXAprK1ChnLfKSdtlsp7NOlPnZAYY
```

The target security token is provided in [DMF API](../../apis/dmf_api/) as part of the update message in order to allow DMF clients to leverage the feature or can it be manually retrieved per target by [Management API](../../apis/management_api/) or in the [Management UI](../../ui) in the target details.

Note: needs to be enabled in your hawkBit installation **and** in the tenant configuration. That allows both the operator as well as the individual customer (if run in a multi-tenant setup) to enable this access method. See [DdiSecurityProperties](https://github.com/eclipse/hawkbit/blob/master/hawkbit-security-core/src/main/java/org/eclipse/hawkbit/security/DdiSecurityProperties.java) for system wide enablement.

The additional activation for the individual tenant:

![Enable Target Token](../../images/security/targetToken.png)

#### Gateway Security Token Authentication
Often the targets are connected through a gateway which manages the targets directly and as a result are indirectly connected to the hawkBit update server.

To authenticate this gateway and allow it to manage all target instances under its tenant there is a _GatewayToken_ to authenticate this gateway through the HTTP-Authorization header with a custom scheme _GatewayToken_. This is of course also handy during development or for testing purposes. However, we generally recommend to use this token with care as it allows to act _in the name of_ any device.

```
GET /SPDEMO/controller/v1/0e945f95-9117-4500-9b0a-9c6d72fa6c07 HTTP/1.1
Host: your.hawkBit.server
Authorization: GatewayToken 3nkswAZhX81oDtktq0FF9Pn0Tc0UGXPW
```

Note: needs to be enabled in your hawkBit installation **and** in the tenant configuration. That allows both the operator as well as the individual customer (if run in a multi-tenant setup) to enable this access method. See [DdiSecurityProperties](https://github.com/eclipse/hawkbit/blob/master/hawkbit-security-core/src/main/java/org/eclipse/hawkbit/security/DdiSecurityProperties.java) for system wide enablement.

The additional activation for the individual tenant:

![Enable Gateway Token](../../images/security/gatewayToken.png)

#### Anonymous access
Here we offer general anonymous access for all targets (see [DdiSecurityProperties](https://github.com/eclipse/hawkbit/blob/master/hawkbit-security-core/src/main/java/org/eclipse/hawkbit/security/DdiSecurityProperties.java)) which we consider not really sufficient for a production system but it might come in handy to get a project started in the beginning.

However, anonymous download on the other side might be interesting even in production for scenarios where the artifact itself is already encrypted.

The activation for the individual tenant:

![Enable Anonymous Download](../../images/security/anonymousDownload.png)

## DMF API
Authentication is provided by _RabbitMQ_ [vhost and user credentials](https://www.rabbitmq.com/access-control.html) that is used for the integration.

## Management API
- Basic Auth

## Management UI
- Login Dialog
- OpenID Connect

