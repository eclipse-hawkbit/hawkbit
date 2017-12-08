<img src=hawkbit_logo.png width=533 height=246 />

# Eclipse hawkBit™ - Update Server

Eclipse [hawkBit](http://www.eclipse.org/hawkbit/index.html) is an domain independent back end solution for rolling out software updates to constrained edge devices as well as more powerful controllers and gateways connected to IP based networking infrastructure.

Build: [![Circle CI](https://circleci.com/gh/eclipse/hawkbit.svg?style=shield)](https://circleci.com/gh/eclipse/hawkbit)
 [![Codacy Badge](https://api.codacy.com/project/badge/Grade/83b1ace1fba94ea2aec93b202b52f39a)](https://www.codacy.com/app/kai-zimmermann/hawkbit?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=eclipse/hawkbit&amp;utm_campaign=Badge_Grade) [![SonarQuality](https://sonar.ops.bosch-iot-rollouts.com/api/badges/gate?key=org.eclipse.hawkbit:hawkbit-parent)](https://sonar.ops.bosch-iot-rollouts.com) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.eclipse.hawkbit/hawkbit-parent/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.eclipse.hawkbit/hawkbit-parent)

# Documentation

see [hawkBit Documentation](https://www.eclipse.org/hawkbit/documentation/overview/introduction.html)

# Contact us

* Want to chat with the team behind hawkBit? [![Join the chat at https://gitter.im/eclipse/hawkbit](https://badges.gitter.im/eclipse/hawkbit.svg)](https://gitter.im/eclipse/hawkbit?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
* Having issues with hawkBit? Open a [GitHub issue](https://github.com/eclipse/hawkbit/issues).
* You can also check out our [Project Homepage](https://www.eclipse.org/hawkbit) for further contact options.

# Examples and Extensions

Next to the hawkBit core hosted here the project maintains as well [examples](https://github.com/eclipse/hawkbit-examples) and [extension](https://github.com/eclipse/hawkbit-extensions) repositories.

# hawkBit sandbox

We offer a sandbox installation that is free for everyone to try out hawkBit. However, keep in mind that the sandbox database will be reset from time to time. It is also not possible to upload any artifacts into the sandbox. But you can use it to try out the Management UI, Management API and DDI API.

https://hawkbit.eu-gb.mybluemix.net/UI/

In addition the following vendors offer free trial accounts for their hawkBit compatible products:

- [Bosch IoT Rollouts](https://www.bosch-iot-suite.com/rollouts/)

# Device Integration

hawkBit does not provide off the shelf clients for devices as part of the project. The long term goal is to provide an [Eclipse hono](https://github.com/eclipse/hono) integration which will provide connectivity through various IoT protocols and as a result allows a wide range of clients to connect to hawkBit. However, the hawkBit [Direct Device Integration (API) API](http://www.eclipse.org/hawkbit/documentation/interfaces/ddi-api.html) is HTTP/JSon based which should allow any update client to integrate quite easily.

There are clients outside of the Eclipse IoT eco system as well, e.g.:

* [SWupdate](https://github.com/sbabic/swupdate) which is a Linux Update agent with focus on a efficient and safe way to update embedded systems.

* [rauc-hawkbit](https://github.com/rauc/rauc-hawkbit) which is a python-based hawkBit client application and library for the [RAUC](https://github.com/rauc/rauc) update framework.

# Getting Started

We are providing a [Spring Boot](https://projects.spring.io/spring-boot/) based reference [Update Server](hawkbit-runtime/hawkbit-update-server) including embedded H2 DB for test and evaluation purposes. 
Run with docker:

```
$  docker run -d -p 8080:8080 hawkbit/hawkbit-update-server
```

Open the update server in your browser:

[localhost:8080](http://localhost:8080) 

See below for how to build and run the update server on your own. In addition we have a [guide](http://www.eclipse.org/hawkbit/documentation/guide/runhawkbit.html) for setting up a complete landscape.


# hawkBit (Spring boot) starters

Next to the [Update Server](hawkbit-runtime/hawkbit-update-server) we are also providing a set of [Spring Boot Starters](hawkbit-starters) to quick start your own [Spring Boot](https://projects.spring.io/spring-boot/) based application.

# Clone, build and run hawkBit

## Build and start hawkBit [Update Server](hawkbit-runtime/hawkbit-update-server)

```
$ git clone https://github.com/eclipse/hawkbit.git
$ cd hawkbit
$ mvn clean install
$ java -jar ./hawkbit-runtime/hawkbit-update-server/target/hawkbit-update-server-#version#.jar
```

## Start hawkBit [Device Simulator](https://github.com/eclipse/hawkbit-examples/tree/master/hawkbit-device-simulator) (optional)

```
$ git clone https://github.com/eclipse/hawkbit-examples.git
$ cd hawkbit-examples
$ mvn clean install
```

```
$ java -jar ./hawkbit-device-simulator/target/hawkbit-device-simulator-#version#.jar
```

## Generate getting started data with the [Management API example](https://github.com/eclipse/hawkbit-examples/tree/master/hawkbit-example-mgmt-simulator) (optional)

```
$ java -jar ./hawkbit-example-mgmt-simulator/target/hawkbit-example-mgmt-simulator-#version#-exec.jar
```

# Releases and Roadmap

* In the upcoming release [0.2](https://github.com/eclipse/hawkbit/issues/390):
  * Rollout management for large scale update campaigns.
  * Clustering capabilities for the update server.
  * Upgrade of Spring Boot and Vaadin dependencies (Boot 1.4, Vaadin 7.7).
  * Improvements on modularization and customizability based on Spring's auto-configuration mechanism.
  * Provide Spring Boot Starters for custom apps based on hawkBit.
  * Provide standard runtime by means of Spring Boot based hawkBit update server (and hopefully a docker image).
  * And of course tons of usability improvements and bug fixes.
* Future releases
  * Complete repository refactoring.
  * Integrate with Eclipse hono as DMF provider.
  * Flexible DMF messaging infrastructure (e.g. with Spring Cloud Stream).
  * Migrate to Spring Framework 5, Spring Boot 2 and Vaadin 8
  * Re-evaluate JPA as persistence provider (e.g. look into jOOQ)

# Status and API stability

hawkBit is currently in '0.X' semantic version. That is due to the need that there is still content in hawkBit that is in need for refactoring. That includes the maven module structure, Spring Boot Properties, Spring Boot auto configuration as well as internal Java APIs (e.g. the [repository API](https://github.com/eclipse/hawkbit/issues/197) ).

However, the device facing [DDI API](https://github.com/eclipse/hawkbit/tree/master/hawkbit-ddi-api) is on major version 'v1' and will be kept stable.

Server facing and [DMF API](https://github.com/eclipse/hawkbit/tree/master/hawkbit-dmf/hawkbit-dmf-api) are [Management API](https://github.com/eclipse/hawkbit/tree/master/hawkbit-mgmt-api) are on v1 as well. However, we cannot fully guarantee the same stability during hawkBit's 0.X development but we will try as best we can.

