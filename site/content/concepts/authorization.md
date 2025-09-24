---
title: Authorization
parent: Concepts
weight: 52
---

Authorization is handled separately for _Direct Device Integration (DDI) API_ and _Device Management Federation (DMF)
API_ (where successful authentication includes full authorization) and _Management API_ and _UI_ which is based on
Spring
security [authorities](https://github.com/eclipse-hawkbit/hawkbit/blob/master/hawkbit-security-core/src/main/java/org/eclipse/hawkbit/im/authentication/SpPermission.java).
<!--more-->

However, keep in mind that hawkBit does not offer an off the shelf authentication provider to leverage these permissions
and the underlying multi user/tenant capabilities of hawkBit but it supports authentication providers offering an OpenID
Connect interface. Check out [Spring security documentation](http://projects.spring.io/spring-security/) for further
information. In
hawkBit [SecurityAutoConfiguration](https://github.com/eclipse-hawkbit/hawkbit/blob/master/hawkbit-autoconfigure/src/main/java/org/eclipse/hawkbit/autoconfigure/security/SecurityAutoConfiguration.java)
is a good starting point for integration.

The default implementation is single user/tenant with basic auth and the logged in user is provided with all
permissions. Additionally, the application properties may be configured for multiple static users;
see [Multiple Users](#multiple-users) for details.

## DDI API

An authenticated target is permitted to:

- retrieve commands from the server
- provide feedback to the the server
- download artifacts that are assigned to it

A target might be permitted to download artifacts without authentication (if enabled, see above). Only the download can
be permitted to disable the authentication. This can be used in scenarios where the artifacts itself are e.g. signed and
secured.

## Management API and UI

### Multiple Users

hawkBit optionally supports configuring multiple static users through the application properties. In this case, the user
and password Spring security properties are ignored.
An example configuration is given below.

    hawkbit.security.user.admin.password={noop}admin
    hawkbit.security.user.admin.firstname=Test
    hawkbit.security.user.admin.lastname=Admin
    hawkbit.security.user.admin.email=admin@test.de
    hawkbit.security.user.admin.permissions=ALL
    
    hawkbit.security.user.test.password={noop}test
    hawkbit.security.user.test.firstname=Test
    hawkbit.security.user.test.lastname=Tester
    hawkbit.security.user.test.email=test@tester.com
    hawkbit.security.user.test.permissions=READ_TARGET,UPDATE_TARGET,CREATE_TARGET,DELETE_TARGET

A permissions value of `ALL` will provide that user with all possible permissions. Passwords need to be specified with
the used password encoder in brackets. In this example, `noop` is used as the plaintext encoder. For production use, it
is recommended to use a hash function designed for passwords such as *bcrypt*. See
this [blog post](https://spring.io/blog/2017/11/01/spring-security-5-0-0-rc1-released#password-storage-format) for more
information on password encoders in Spring Security.

### OpenID Connect

hawkbit supports authentication providers which use the OpenID Connect standard, an authentication layer built on top of
the OAuth 2.0 protocol.
An example configuration is given below.

    spring.security.oauth2.client.registration.oidc.client-id=clientID
    spring.security.oauth2.client.provider.oidc.issuer-uri=https://oidc-provider/issuer-uri
    spring.security.oauth2.client.provider.oidc.jwk-set-uri=https://oidc-provider/jwk-set-uri

Note: at the moment only DEFAULT tenant is supported. By default the resource_access/<client id>/roles claim is mapped
to hawkBit permissions. 

### Delivered Permissions

- READ_/UPDATE_/CREATE_/DELETE_TARGET for:
    - Target entities including metadata (that includes also the installed and assigned distribution sets)
    - Target tags
    - Target actions
    - Target registration rules
    - Bulk operations
    - Target filters

- READ_/UPDATE_/CREATE_/DELETE_REPOSITORY for:
    - Distribution sets
    - Software Modules
    - Artifacts
    - DS tags

- SOFTWARE_MODULE_DOWNLOAD
    - Permission to download artifacts of a software module (Note: READ_REPOSITORY allows only to read the metadata).

- READ_TARGET_SECURITY_TOKEN
  - Permission to read the target security token. The security token is security concerned and should be protected.

- READ_TENANT_CONFIGURATION/TENANT_CONFIGURATION
    - Permission to read/administrate the tenant settings.

- READ_/UPDATE_/CREATE_/DELETE_/HANDLE_/APPROVE_ROLLOUT for:
    - Managing rollouts and provision targets through a rollout.

### Permission Matrix for example uses cases that need more than one permission

| Use Case                                                                   | Needed permissions                                                         |
|----------------------------------------------------------------------------|----------------------------------------------------------------------------|
| Search _targets_ by installed or assigned _distribution set_               | READ_REPOSITORY, READ_TARGET                                               |
| Assign _DS_ to a _target_                                                  | READ_REPOSITORY, UPDATE_TARGET                                             |
| Assign DS to target through a _Rollout_, i.e. _Rollout_ creation and start | READ_REPOSITORY, READ_TARGET, READ_ROLLOUT, CREATE_ROLLOUT, HANDLE_ROLLOUT |
| Read _Rollout_ status including its _deployment groups_                    | READ_REPOSITORY, READ_ROLLOUT                                              |
| Checks _targets_ inside _Rollout deployment group_                         | READ_REPOSITORY, READ_TARGET, READ_ROLLOUT                                 |

## Device Management Federation API

The provided _RabbitMQ_ [vhost and user](https://www.rabbitmq.com/access-control.html) should be provided with the
necessary permissions to send messages to hawkBit through the exchange and receive messages from it through the
specified queue.
