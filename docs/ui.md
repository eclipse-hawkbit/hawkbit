# hawkBit UI

In this guide we describe what the hawkBit UI is and how to run and configure it.

---

### Overview

The hawkBit UI is a web application for managing hawkBit in the browser. With it you can work with targets, target
filter queries, auto assignments, software modules, distribution sets and rollouts, and change the tenant
configuration.

The UI is a separate application. It has no database of its own - everything it shows and changes goes through the
hawkBit [Management API](management-api.md). So it needs a running hawkBit Update Server (monolith) or hawkBit
Management Server (micro-services), and you can do in the UI only what your hawkBit user is permitted to do.

---

### Run with Docker Compose

The easiest way is to use one of the Docker Compose files with UI from the
[docker](https://github.com/eclipse-hawkbit/hawkbit/tree/master/docker) folder (`*-with-ui-*.yml`), for example:

```bash
$ git clone https://github.com/eclipse-hawkbit/hawkbit.git
$ cd hawkbit/docker
$ docker compose -f postgres/docker-compose-monolith-with-ui-postgres.yml up -d
```

There are variants for MySQL and PostgreSQL, for monolith and micro-services, with and without a DB init container
(`mysql/` and `postgres/` folders).

---

### Run as Java application

Build hawkBit (see [Quick Start](quick-start.md)), start the hawkBit Update Server and then the UI, pointing it to the
Management API:

```bash
$ java -jar hawkbit-ui/target/hawkbit-ui.jar --hawkbit.server.mgmtUrl=http://localhost:8080
```

Building the UI from sources needs Node.js.

---

### Access and login

Open [http://localhost:8088](http://localhost:8088) in your browser.

Log in with a hawkBit user, for instance the default **admin/admin** of the hawkBit Update Server. The UI checks the
credentials against the Management API and then uses them for all its calls. So, users and their permissions are
configured in the hawkBit server, not in the UI (see [Authorization](authorization.md)).

---

### Configuration

| Property                                        | Description                                  | Default                 |
|-------------------------------------------------|----------------------------------------------|-------------------------|
| `server.port` (or environment variable `PORT`)  | Port of the UI                               | `8088`                  |
| `hawkbit.server.mgmtUrl`                        | URL of the hawkBit Management API            | `http://localhost:8080` |
| `hawkbit.server.security.oauth2.client.enabled` | Log in via an OpenID Connect (OIDC) provider | `false`                 |

In Docker you can set them with the `SPRING_APPLICATION_JSON` environment variable, e.g.:

```yaml
hawkbit-ui:
  image: "hawkbit/hawkbit-ui:latest"
  environment:
    - 'SPRING_APPLICATION_JSON={"hawkbit.server.mgmtUrl": "http://hawkbit:8080"}'
  ports:
    - 8088:8088
```

---

### Login with OpenID Connect

Instead of username and password the UI can redirect users to an OpenID Connect provider (e.g. Keycloak, Auth0). See
[OpenID Connect](authorization.md#openid-connect) for the needed UI and server configuration.
