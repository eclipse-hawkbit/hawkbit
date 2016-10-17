# hawkBit UAA Extension
The hawkBit UAA extension enables hawkBit to use the [User Account and Authentication (UAA)](https://github.com/cloudfoundry/uaa) based on the Spring Security OAuth project.
In general the mechanism can be adapted to use any OAUTH2 server. The `UserPrincipalInfoTokenServices` might be adapted for other OAUTH2 providers to extract the principal information.

The `UaaOAuthAutoConfiguration` setups the necessary servlet-filter to intercept the security-chain to implement the OAUTH2 workflow. It allows to redirect to the UAA-Login-Form via the URL `/uaalogin` as well as to use the bearer access token within the hawkBit REST-API using the HTTP Authorization header `Authorization: bearer ezro63ut....`.

To retrieve the bearer access token, check out the OAUTH2 authorization resource of the [UAA-API](https://github.com/cloudfoundry/uaa/blob/master/docs/UAA-APIs.rst)

## Using UAA Extension
To use this extension in the example application you just need to add the maven-dependencies to the example-application `pom.xml` and configure the necessary properties in the `application.properties`
```
<dependency>
  <groupId>org.eclipse.hawkbit</groupId>
  <artifactId>hawkbit-extension-uaa</artifactId>
  <version>${project.version}</version>
</dependency>
```

## Configuration
The `UaaClientProperties` must contain the necessary configuration to setup the OAUTH2 client-id and client-credentials, as well as the necessary OAUTH2 URLS as well as the JWT signing key.

```
uaa.client.clientId=hawkBit
uaa.client.clientSecret=hawkBitSecret
uaa.client.accessTokenUri=http://localhost:8080/uaa/oauth/token
uaa.client.userAuthorizationUri=http://localhost:8080/uaa/oauth/authorize
uaa.client.clientAuthenticationScheme=form
uaa.resource.userInfoUri=http://localhost:8080/uaa/userinfo
uaa.resource.jwt.keyValue=uaasign
```

## Multitenancy
The implementation uses the `zid` (zoneId) as tenant information. The default zone-id in the UAA is `uaa`, so every user which logs into hawkBit logs in for the tenant `uaa`. You can use the UAA zones mechanism to implement multi-tenancy mechanism to hawkbit or you can change the strategy. To change the tenant extraction strategy you can adapt the `UserPrincipalInfoTokenServices` which creates the hawkBit `UserPrincipal` which contains the tenant information of the principal. 

## Token Signing
Using the bearer token within the hawkBit REST-APIs the bearer token is verified by hawkBit using either a symmetric or asymmetric keys. The necessary `key-value` must be configured in the configuration. 

# UAA Configuration
The [User Account and Authentication (UAA)](https://github.com/cloudfoundry/uaa) can be started as an stand-alone application.
More information to configuration see [UAA-Docs](https://github.com/cloudfoundry/uaa/blob/master/docs).

## uaa.yml
The `uaa.yml` contains the necessary bootstrap configuration of the UAA. To work with hawkBit you'll need to setup the OAUTH2 client which hawkBit is using. Furthermore you need to setup the necessary hawkBit permissions to allow hawkBit to do authorization decision based on the known permissions.

Example `uaa.yml` configuration:
```
scim:
  users:
    - hawkbitadmin|hawkbitadmin|hawkbitadmin@test.org|hawkbitadmin|hawkbitadmin|uaa.admin,READ_TARGET,CREATE_TARGET,UPDATE_TARGET,DELETE_TARGET,READ_REPOSITORY,UPDATE_REPOSITORY,CREATE_REPOSITORY,DELETE_REPOSITORY,SYSTEM_MONITOR,SYSTEM_DIAG,SYSTEM_ADMIN,DOWNLOAD_REPOSITORY_ARTIFACT,TENANT_CONFIGURATION,ROLLOUT_MANAGEMENT
  groups:
    zones.read: Read identity zones
    zones.write: Create and update identity zones
    idps.read: Retrieve identity providers
    idps.write: Create and update identity providers
    clients.admin: Create, modify and delete OAuth clients
    clients.write: Create and modify OAuth clients
    clients.read: Read information about OAuth clients
    clients.secret: Change the password of an OAuth client
    scim.write: Create, modify and delete SCIM entities, i.e. users and groups
    scim.read: Read all SCIM entities, i.e. users and groups
    scim.create: Create users
    scim.userids: Read user IDs and retrieve users by ID
    scim.zones: Control a user's ability to manage a zone
    scim.invite: Send invitations to users
    password.write: Change your password
    oauth.approval: Manage approved scopes
    oauth.login: Authenticate users outside of the UAA
    openid: Access profile information, i.e. email, first and last name, and phone number
    groups.update: Update group information and memberships
    uaa.user: Act as a user in the UAA
    uaa.resource: Serve resources protected by the UAA
    uaa.admin: Act as an administrator throughout the UAA
    uaa.none: Forbid acting as a user
    uaa.offline_token: Allow offline access

jwt:
   token:
      signing-key: uaasign
      verification-key: uaasign
login:
  branding:
    companyName: hawkbit
#    squareLogo: |
#      this is an invalid
#      base64 logo with
#      line feeds
#    productLogo: |
#      this is an invalid
#      base64 logo with
#      line feeds

oauth:
  user:
    authorities:
      - openid
  clients:
    hawkbit:
      id: hawkbit
      secret: hawkbitsecret
      authorized-grant-types: password,implicit,authorization_code,client_credentials,refresh_token
      scope: READ_TARGET,CREATE_TARGET,UPDATE_TARGET,DELETE_TARGET,READ_REPOSITORY,UPDATE_REPOSITORY,CREATE_REPOSITORY,DELETE_REPOSITORY,SYSTEM_MONITOR,SYSTEM_DIAG,SYSTEM_ADMIN,DOWNLOAD_REPOSITORY_ARTIFACT,TENANT_CONFIGURATION,ROLLOUT_MANAGEMENT,openid,uaa.user,uaa.admin,password.write,scim.userids,cloud_controller.admin,scim.read,scim.write
      authorities: uaa.admin,openid,scim.read,zones.uaa.admin,scim.userids,scim.zones
      autoapprove: true 
```

## Dockerize UAA
The UAA is not shipped as a docker container unfortunately so you build the UAA as docker image by your own
Dockerfile
```
FROM tomcat:8.5.6-jre8
COPY uaa.war /usr/local/tomcat/webapps/uaa.war
RUN echo "CLOUD_FOUNDRY_CONFIG_PATH=/etc/uaa" >> /usr/local/tomcat/conf/catalina.properties
```
The UAA configuration is based on the `uaa.yml` file which can be placed into the `/etc/uaa/uaa.yml` on the docker host system.

After building the `uaa` docker image you can then start the `uaa-server` using docker
``` 
docker run -p 8080:8080 -d -e "SPRING_PROFILES_ACTIVE=hsqldb" -v "/etc/uaa:/etc/uaa" uaa
```
