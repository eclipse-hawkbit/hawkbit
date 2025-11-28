# Authorization

Authorization is handled separately for _Direct Device Integration (DDI) API_ and _Device Management Federation (DMF)
API_ (where successful authentication includes full authorization) and _Management API_ and _UI_ which is based on
Spring
security [authorities](https://github.com/eclipse-hawkbit/hawkbit/blob/master/hawkbit-security-core/src/main/java/org/eclipse/hawkbit/im/authentication/SpPermission.java).

However, keep in mind that hawkBit does not offer an off the shelf authentication provider to leverage these permissions
and the underlying multi user/tenant capabilities of hawkBit but it supports authentication providers offering an OpenID
Connect interface. Check out [Spring security documentation](http://projects.spring.io/spring-security/) for further
information. In
hawkBit [SecurityAutoConfiguration](https://github.com/eclipse-hawkbit/hawkbit/blob/master/hawkbit-autoconfigure/src/main/java/org/eclipse/hawkbit/autoconfigure/security/SecurityAutoConfiguration.java)
is a good starting point for integration.

The default implementation is single user/tenant with basic authentication and the logged-in user is provided with all
permissions. Additionally, the application properties may be configured for multiple static users;
see [Multiple Users](#multiple-users) for details.

---

## DDI API

An authenticated target is permitted to:

- retrieve commands from the server
- provide feedback to the the server
- download artifacts that are assigned to it

A target might be permitted to download artifacts without authentication (if enabled, see above). Only the download can
be permitted to disable the authentication. This can be used in scenarios where the artifacts itself are e.g. signed and
secured.

---

## Management API

### Multiple Users

hawkBit optionally supports configuring multiple static users through the application properties. In this case, the user
and password Spring security properties are ignored.
An example configuration is given below.

```properties
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
```

A permissions value of `ALL` will provide that user with all possible permissions. Passwords need to be specified with
the used password encoder in brackets. In this example, `noop` is used as the plaintext encoder. For production use, it
is recommended to use a hash function designed for passwords such as *bcrypt*. See
this [blog post](https://spring.io/blog/2017/11/01/spring-security-5-0-0-rc1-released#password-storage-format) for more
information on password encoders in Spring Security.

### OpenID Connect

hawkbit supports authentication providers which use the OpenID Connect standard, an authentication layer built on top of
the OAuth 2.0 protocol.
An example configuration is given below.

```properties
spring.security.oauth2.client.registration.oidc.client-id=clientID
spring.security.oauth2.client.provider.oidc.issuer-uri=https://oidc-provider/issuer-uri
spring.security.oauth2.client.provider.oidc.jwk-set-uri=https://oidc-provider/jwk-set-uri
```

Note: at the moment only DEFAULT tenant is supported. By default the resource_access/<client id>/roles claim is mapped
to hawkBit permissions.

### Permissions

#### Basics

hawkBit uses fine-grained permissions to restrict scopes to the managed entities. The main concept in permissions are:

1. Permission group - a management API usually has a `permission group` correspondent to the managed entity type. For
   instance Target Management's `permission group` is `TARGET` while Software Module Management's is `SOFTWARE_MODULE`.
2. Permission action (CRUD and special) - `CREATE`, `READ`, `UPDATE`, `DELETE` as well `HANDLE` and `APROVE` for
   Rollouts Management. The permissions are the formed with `<action>_<group>` pattern - e.g. `CREATE_TARGET`,
   `READ_SOFTWARE_MODULE`
3. Permission scope - permissions could be scoped further with RSQL filter scopes. I.e. one could restrict
   `CREATE_TARGET` permissions with a scope, e.g. `type.id==[permissions.md](permissions.md)1`. Then, the final
   authority that will be granted to user will be `<permission>/<scope>`, e.g. `CREATE_TRAGET/type.id==1`. If the
   permission is not scoped the authority will be just `<permission>`, e.g. `CREATE_TRAGET`. By default, scope support
   is disabled!

### Permission groups list

- READ/UPDATE/CREATE/DELETE_TARGET for:
    - Target types
- READ/UPDATE/CREATE/DELETE_TARGET for:
    - Target entities including metadata (that includes also the installed and assigned distribution sets)
    - Target tags
    - Target actions
    - Target registration rules
    - Bulk operations
    - Target filters
- READ/UPDATE/CREATE/DELETE_SOFTWARE_MODULE for:
    - Software Module types
- READ/UPDATE/CREATE/DELETE_SOFTWARE_MODULE for:
    - Software Modules
    - Artifacts
- READ/UPDATE/CREATE/DELETE_DISTRIBUTION_SET_TYPE for
    - Distribution Set types
- READ/UPDATE/CREATE/DELETE_DISTRIBUTION_SET for:
    - Distribution sets
    - DS tags
- DOWNLOAD_REPOSITORY_ARTIFACT
    - Permission to download artifacts of a software module (Note: READ_REPOSITORY allows only to read the metadata).
- READ_TARGET_SECURITY_TOKEN
    - Permission to read the target security token. The security token is security concerned and should be protected.
- READ/UPDATE/CREATE/DELETE_TENANT_CONFIGURATION/TENANT_CONFIGURATION
    - Permission to read/administrate the tenant settings.
- READ/UPDATE/CREATE/DELETE/HANDLE/APPROVE_ROLLOUT for:
    - Managing rollouts and provision targets through a rollout

### Permission Matrix for example uses cases that need more than one permission

| Use Case                                                                   | Needed permissions                                                               |
|----------------------------------------------------------------------------|----------------------------------------------------------------------------------|
| Search _targets_ by installed or assigned _distribution set_               | READ_DISTRIBUTION_SET, READ_TARGET                                               |
| Assign _DS_ to a _target_                                                  | READ_DISTRIBUTION_SET, UPDATE_TARGET                                             |
| Assign DS to target through a _Rollout_, i.e. _Rollout_ creation and start | READ_DISTRIBUTION_SET, READ_TARGET, READ_ROLLOUT, CREATE_ROLLOUT, HANDLE_ROLLOUT |
| Read _Rollout_ status including its _deployment groups_                    | READ_DISTRIBUTION_SET, READ_ROLLOUT                                              |
| Checks _targets_ inside _Rollout deployment group_                         | READ_DISTRIBUTION_SET, READ_TARGET, READ_ROLLOUT                                 |

#### Coarse Access Control

All sensitive operation of the management APIs are protected with permission checks. The permission checks are done with
Spring Security's `@PreAuthorize` annotation, mostly using a `hasPermission` which allows handling more complex the user
authorities. The permission evaluation is handled by a custom `PermissionEvaluator` registered
by [org.eclipse.hawkbit.repository.RepositoryConfiguration](https://github.com/eclipse-hawkbit/hawkbit/blob/master/hawkbit-repository/hawkbit-repository-core/src/main/java/org/eclipse/hawkbit/repository/RepositoryConfiguration.java).

These `@PreAuthorize` annotations ensures that only the users with required permissions will be able to call the
management methods.

#### Fine-grained Access Control - Access Controller

Further there is a concept of `Access Controller` which could provide further restrictions on the managed entities. If
the user have access to a management method, i.e. has the required permissions, his access to the entities could be
restricted by registered `Access Controller` for the management service. The `Access Controller` is a typed Spring
component which
implements [org.eclipse.hawkbit.acm.access.AccessController](https://github.com/eclipse-hawkbit/hawkbit/blob/master/hawkbit-repository/hawkbit-repository-jpa/src/main/java/org/eclipse/hawkbit/repository/jpa/acm/AccessController.java)
interface.

Out of the box hawkBit provides `Access Controller` implementations for:

* Target Management - `target`
* Target Type Management - `target-type`
* Software Module Management - `software-module`
* Software Module Type Management - `software-module-type`
* Distribution Set Management - `distribution-set`
* Distribution Set Type Management - `distribution-set-type`
* Action Management - `action`

By default, hawkbit doesn't support scopes. To enable scopes a Spring property `hawkbit.acm.access-controller.enabled`
shall be set to `true`. E.g. add in `application.properties`:

```properties
hawkbit.acm.access-controller.enabled=true
```

By default, if you have enabled scopes, all supported Access Controllers will be registered. If you want to disable it
for a specific service, you could set a property `hawkbit.acm.access-controller.<service>.enabled` to `false`. E.g. to
disable it for Target Management:

```properties
hawkbit.acm.access-controller.target.enabled=false
```

---

## Device Management Federation API

The provided _RabbitMQ_ [vhost and user](https://www.rabbitmq.com/access-control.html) should be provided with the
necessary permissions to send messages to hawkBit through the exchange and receive messages from it through the
specified queue.