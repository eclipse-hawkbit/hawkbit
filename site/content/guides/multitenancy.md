---
title: Multi-tenancy
parent: Guides
weight: 32
---

hawkBit is able to support with multiple tenants with some constraints. This guide provides insights in the basic concepts and how to setup your multiple tenants.
<!--more-->

## Big picture

Multi-tenancy is a core concept of hawkBit. It is supported by providing hawkBit authentication system supporting tenants as well as by using EclipseLink and Hibernate native multi-tenant support.

The tenant and user management, however, is only static. This means you shall configure all tenants and users into spring configurations. 

The tenant and user management could be extended to be more flexible - e.g. dynamic management, OAuth and so on. This however is left for integrators for specific use cases.

## Constraints
* static tenant and user management
* configuration only via TenantAwareUserProperties#user configuration (i.e. hawkbit.security.user properties), hence
* username is unique for the whole system - there could not be same username for different tenants
* in order to create dynamically tenants when user is logged in you shall set hawkbit.server.repository.implicitTenantCreateAllowed=true, 

## Example

```properties
# allow to auto/implicit create DEFAULT tenant (on mgmt api call)
hawkbit.server.repository.implicitTenantCreateAllowed=true
# setup foo tenant with two users john and smith_on_foo
hawkbit.security.user.john.tenant=foo
hawkbit.security.user.john.password={noop}john_password
hawkbit.security.user.john.roles=TENANT_ADMIN
hawkbit.security.user.smith_on_foo.tenant=foo
hawkbit.security.user.smith_on_foo.password={noop}smith_password
hawkbit.security.user.smith_on_foo.roles=TENANT_ADMIN
# setup bar tenant with single user smith_on_bar
hawkbit.security.user.smith_on_bar.tenant=bar
hawkbit.security.user.smith_on_bar.password={noop}smith_password
hawkbit.security.user.smith_on_bar.roles=TENANT_ADMIN
```
This example creates two tenants foo and bar:
* foo has two users john and smith_on_foo
  * john/john_password
  * smith_on_foo/smith_password
* bar has single user smith_on_bar (it is again smith but since the user smith user could not be used for both tenants foo we need to use different username)
  * smith_on_bar/smith_password
