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

### Certificate Authentication by Reverse Proxy

hawkBit offers a certificate-based authentication mechanism, referred to as Mutual TLS, which eliminates the need to share a security token with the server. To implement this, you'll require a reverse proxy deployed before the hawkBit server to handle authentication. This process involves obtaining certificates for both the client and the reverse proxy and configuring hawkBit accordingly.

Initially, you'll need to obtain certificates for these components, which is not covered in this description. Once you have acquired certificates for both the client and the Hawkbit server, whether from the same or different Certificate Authorities (CAs), you'll need to place these certificates in their respective locations.

- CA1 signs Client.crt
- CA2 signs Proxy.crt

- Client has the Client.crt, Client.key and CA2
- Proxy has the Proxy.crt, Proxy.key and CA1 

Here is a diagram that shows all the communication between the hawkBit, reverse proxy and client. 

![Reverse Proxy Architecutre](../../images/security/reverseProxyArchitecture.png)

#### Nginx Reverse Proxy Configurations
After placing your certificates, you need to deploy your proxy server and apply the provided configurations. You can apply Mutual TLS specifically to the URL given below to implement the process only for devices using the Device Integration API:

`hawkbit.dev.example.com/default/controller/`

This ensures that other clients, like UI users, can connect to Hawkbit without requiring client certificates. They can use Username and Password in the Management API, eliminating the need for authentication and making it more user-friendly.

```nginx
# Nginx Hawkbit Configurations

# Gets the Common Name of the certificate from the client certificate. 
map $ssl_client_s_dn $ssl_client_s_dn_cn {
    default "";
    ~CN=(?<CN>[^,]+) $CN;
}

server {
    listen 80;
    listen [::]:80;

    server_name hawkbit.dev.example.com www.hawkbit.dev.example.com;
    server_tokens off;

    location /.well-known/acme-challenge/ {
        root /var/www/certbot;
    }

    location / {
        return 301 https://hawkbit.dev.example.com$request_uri;
    }
}

server {
    listen 443 ssl;
    listen [::]:443 ssl;

    server_name hawkbit.dev.example.com;

    ssl_certificate /etc/nginx/ssl/live/hawkbit.dev.example.com/fullchain.pem;
    ssl_certificate_key /etc/nginx/ssl/live/hawkbit.dev.example.com/privkey.pem;

    ssl_client_certificate /etc/nginx/client-cer/BORDA-ROOTCA.crt;
    ssl_verify_client      optional;
    ssl_verify_depth 3;


   # For devices that is using device integration API, 
   # Mutual TLS is required.
   location /default/controller/ {
        if ($ssl_client_verify != SUCCESS) {
           return 403;
        }

        proxy_pass http://hawkbit.dev.example.com:8080;
        proxy_set_header Host $http_host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;

        # Client certificate Common Name and Issuer Hash is required
        # for auth in hawkbit. 
        proxy_set_header X-Ssl-Client-Cn $ssl_client_s_dn_cn;
        proxy_set_header X-Ssl-Issuer-Hash-1 Hawkbit;

        # These are required for clients to upload and download software. 
        proxy_request_buffering off;
        client_max_body_size 1000m;
   }

   location /DEFAULT/controller/ {
        if ($ssl_client_verify != SUCCESS) {
           return 403;
        }

        proxy_pass http://hawkbit.dev.example.com:8080;
        proxy_set_header Host $http_host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        
        proxy_set_header X-Ssl-Client-Cn $ssl_client_s_dn_cn;
        proxy_set_header X-Ssl-Issuer-Hash-1 Hawkbit;

        proxy_request_buffering off;
        client_max_body_size 1000m;
   }

   # For clients that is using UI or Management API
   location / {
        proxy_pass http://hawkbit.dev.example.com:8080;
        proxy_set_header Host $http_host; 
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
	    proxy_request_buffering off;
	    client_max_body_size 1000m;
   }   
}
```
To authenticate the request to Hawkbit itself, the common name and issuer hash of the presented client certificate are required. The issuer hash of a certificate is the hash of the certificate that signed the client certificate, which in our case is the CA.

You can use the following command to get the issuer hash:

`openssl x509 -in client_certificate.crt -issuer_hash -noout`

However, in the Nginx configuration, obtaining the issuer hash is not possible without addons. Therefore, this header is manually entered as Hawkbit.

When deploying Nginx, you will need a `.yml` file. Here's an example `docker-compose.yml` file for Nginx Docker.

```yml
version: '3'

services:
  webserver:
    image: nginx:latest
    ports:
      - 80:80
      - 443:443
    restart: always
    volumes:
      - ./nginx/conf/:/etc/nginx/conf.d/:ro
      - ./certbot/www:/var/www/certbot/:ro
      - ./certbot/conf/:/etc/nginx/ssl/:ro
      - ./client-cer/:/etc/nginx/client-cer/
      - ./landing-page/:/etc/webserver/landing-page
  certbot:
    image: certbot/certbot:latest
    volumes:
      - ./certbot/www/:/var/www/certbot/:rw
      - ./certbot/conf/:/etc/letsencrypt/:rw
```
`/client-cer/:/etc/nginx/client-cer/` is the designated location for the certificate authority that has signed the client certificate. The presented client certificate will be verified against this CA.

#### Swupdate Suricatta Configurations

If the client is utilizing the SWUpdate Suricatta service, the configurations on the device or client side should also be adjusted as follows:

The location of the config file is `/etc/swupdate/swupdate.conf`

```yml
suricatta :
{
tenant = "default";
id = "[ID]";
url = "[URL]";
nocheckcert = false;
cafile = "[CAFile]";
sslkey = "/etc/ssl/certs/[ID].key";
sslcert = "/etc/ssl/certs/[ID].crt";
};
```

If your client service is a linux, you can use the command bellow to see the logs produced by the swupdate.

`journalctl --follow -u swupdate`

#### Hawkbit Configurations

There are also some configurations that you need update when you are deploying your hawkbit service. If your deploying it as a docker container you need to update your docker-compose.yml file such as,

```yml
version: '3'

services:
  # HawkBit service
  hawkbit:
    image: "hawkbit/hawkbit-update-server:latest-mysql"
    environment:
      SPRING_APPLICATION_JSON: '{
        "hawkbit.artifact.url.protocols.download-http.rel": "download-http",
        "hawkbit.artifact.url.protocols.download-http.hostname": "hawkbit.dev.example",
        "hawkbit.artifact.url.protocols.download-http.protocol": "https",
        "hawkbit.artifact.url.protocols.download-http.supports": "DMF,DDI",
        "hawkbit.artifact.url.protocols.download-http.ref": "{protocol}://{hostnameRequest}/{tenant}/controller/v1/{controllerId}/softwaremodules/{softwareModuleId}/artifacts/{artifactFileName}"
      }'
    restart: no
    ports:
      - "8080:8080"
    labels:
      NAME: "hawkbit"

```
You need to add the configurations that start with the hawkbit.artifact.url.protocols to the either The link is generated by Hawkbit and then transmitted to the client, enabling the client to download the firmware package through this link. Remember to replace "hostname" with your actual hostname.

In Hawkbit's UI section, under system configuration, make sure to select Allow targets to authenticate via a certificate authenticated by a reverse proxy and input the issuer hash as "Hawkbit". This can be whetever you have configured in the nginx configuration in `proxy_set_header X-Ssl-Issuer-Hash-1`

![Reverse Proxy Settings](../../images/security/reverseProxySettings.png)

After successfully generating your certificates with the correct chain, deploying your Nginx and Hawkbit services with appropriate configurations, and updating the settings on the device side, you will be able to establish a certificate-based authentication mechanism. This will eliminate the necessity of sharing a security token with the server.

#### Testing

You can test the communication by using the Curl command below to see if you successfully implemented Mutual TLS:

`curl -L -v --cert Client.crt --key Client.key --cacert CA2.pem https://hawkbit.dev.example.com/default/controller/v1/{device-id}`

In the UI, after uploading an SWU package and requesting a firmware update, you can use the link below to attempt to install the software package.

`curl -L -v --cert Client.crt --key Client.key --cacert CA2.pem https://hawkbit.dev.example.com/default/controller/v1/{device-id}/softwaremodules/{artifact-id}/artifacts/hawkbit_updated_5.swu --output outputfile`

#### References

Here are some references that can assist you in creating certificates and deploying your services to implement reverse proxy.

- [Configuring Hawkbit under SSL/TLS using Private CA/Sub CA](https://sbabic.github.io/swupdate/hawkbit-setup.html)
- [Preparing HawkBit for Production Use](https://www.apertis.org/concepts/preparing-hawkbit-for-production/)
- [OTA for Embedded Linux Devices using SWUpdate](https://interrupt.memfault.com/blog/ota-for-embedded-linux-devices)
- [Enabling HTTPS using Nginx and Let's Encrypt in Docker](https://mindsers.blog/post/https-using-nginx-certbot-docker/)
- [How To Create a Self-Signed SSL Certificate for Nginx in Ubuntu 20.04](https://www.digitalocean.com/community/tutorials/how-to-create-a-self-signed-ssl-certificate-for-nginx-in-ubuntu-20-04-1)

## DMF API
Authentication is provided by _RabbitMQ_ [vhost and user credentials](https://www.rabbitmq.com/access-control.html) that is used for the integration.

## Management API
- Basic Auth

## Management UI
- Login Dialog
- OpenID Connect

