# hawkBit SDK

In this guide we describe what is hawkBit SDK, how can it be configured and used.

---

### Overview

hawkBit provides a Client-Side Software Development Kit which allows you to:
- Connect a device or a Gateway to hawkBit
- Fetch assignments / update campaigns
- Download firmware or software packages
- Report progress and status
- Interact with Management, DDI, and DMF APIs through one client

[hawkBit SDK](https://github.com/eclipse-hawkbit/hawkbit/tree/master/hawkbit-sdk) has support for DDI, DMF and Mgmt APIs. In other words it allows you to simulate (only simulate?) the behaviour of a DDI/DMF gateway/target or interact with hawkBit through MgmtAPI.

---

### Configuration

Add the desired modules : 

```xml
<dependency>
    <groupId>org.eclipse.hawkbit</groupId>
    <artifactId>hawkbit-sdk-device</artifactId>
</dependency>
<dependency>
    <groupId>org.eclipse.hawkbit</groupId>
    <artifactId>hawkbit-sdk-dmf</artifactId>
</dependency>
<dependency>
    <groupId>org.eclipse.hawkbit</groupId>
    <artifactId>hawkbit-sdk-mgmt</artifactId>
</dependency>
```
Of course if you will not be using DMF there is no need to add `hawkbit-sdk-dmf` or vise versa for DDI case.

The default configuration comes already built-in with the SDK.
But it can be changed in  ```application.properties``` of your service/application:
```properties
hawkbit.tenant.tenant-id=DEFAULT
hawkbit.tenant.username=user
hawkbit.tenant.password=pass
hawkbit.tenant.gateway-token=gateway_token

# DMF case
hawkbit.tenant.dmf.username=dmf_user
hawkbit.tenant.dmf.password=dmf_password
hawkbit.tenant.dmf.virtual-host=dmf_vhost
```

And also you can configure the endpoints of HawkBit and DDI (if used separately as microservices) : 
```properties
# Hawkbit Server config
hawkbit.server.mgmt-url=http://localhost:8080
hawkbit.server.ddi-url=http://localhost:8085
```

Many other configuration options are possible, you can check [HawkbitServer](https://github.com/eclipse-hawkbit/hawkbit/blob/master/hawkbit-sdk/hawkbit-sdk-commons/src/main/java/org/eclipse/hawkbit/sdk/HawkbitServer.java) and [Tenant](https://github.com/eclipse-hawkbit/hawkbit/blob/master/hawkbit-sdk/hawkbit-sdk-commons/src/main/java/org/eclipse/hawkbit/sdk/Tenant.java) classes for more possible configuration properties.

---

### Example applications

If you have applied the configurations from the steps above then you can start building your application. Since we have defined `hawkbit.server` and `hawkbit.tenant` properties then you can add the `HawkbitClient` bean into your application : 

```java
    @Bean
    HawkbitClient hawkbitClient(final HawkbitServer hawkBitServer, final Encoder encoder, final Decoder decoder, final Contract contract) {
        return new HawkbitClient(hawkBitServer, encoder, decoder, contract);
    }
```

Then you can define the authentication setup helper, which offers helper methods for setting up your authentication method(s) :

```java
    @Bean
    AuthenticationSetupHelper mgmtApi(final Tenant tenant, final HawkbitClient hawkbitClient) {
        return new AuthenticationSetupHelper(tenant, hawkbitClient);
    }
```

And finally you can define a Bean instance of `DdiTenant` which offers you interaction with DDI API :

```java
    @Bean
    DdiTenant ddiTenant(final Tenant defaultTenant, final HawkbitClient hawkbitClient) {
        return new DdiTenant(defaultTenant, hawkbitClient);
    }
```

Hawkbit Mgmt API can be now also reached via `HawkbitClient` instance : 

```java
final MgmtTargetRestApi mgmtTargetRestApi = hawkbitClient.mgmtService(MgmtTargetRestApi.class, tenant);

// directly use MGMT REST API methods
PagedList<MgmtTarget> pagedList = mgmtTargetRestApi.getTargets("controllerid==*",0, 100,
                    null).getBody();
```


You can check out the example applications located in [hawkbit-sdk-demo](https://github.com/eclipse-hawkbit/hawkbit/tree/master/hawkbit-sdk/hawkbit-sdk-demo) module.
There you can find example usage for [DDI](https://github.com/eclipse-hawkbit/hawkbit/blob/master/hawkbit-sdk/hawkbit-sdk-demo/src/main/java/org/eclipse/hawkbit/sdk/demo/device/DeviceApp.java) and [DMF](https://github.com/eclipse-hawkbit/hawkbit/blob/master/hawkbit-sdk/hawkbit-sdk-demo/src/main/java/org/eclipse/hawkbit/sdk/demo/dmf/DmfApp.java)  via the hawkbit SDK.


