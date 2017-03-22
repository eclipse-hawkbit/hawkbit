<img src=hawkbit_logo.png width=533 height=246 />

# Eclipse hawkBit™ - Update Server

Eclipse [hawkBit](https://projects.eclipse.org/projects/iot.hawkbit) is an domain independent back end solution for rolling out software updates to constrained edge devices as well as more powerful controllers and gateways connected to IP based networking infrastructure.

Build: [![Circle CI](https://circleci.com/gh/eclipse/hawkbit.svg?style=shield)](https://circleci.com/gh/eclipse/hawkbit) 
 [![Codacy Badge](https://api.codacy.com/project/badge/Grade/83b1ace1fba94ea2aec93b202b52f39a)](https://www.codacy.com/app/kai-zimmermann/hawkbit?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=eclipse/hawkbit&amp;utm_campaign=Badge_Grade) [![SonarQuality](https://sonar.ops.bosch-iot-rollouts.com/api/badges/gate?key=org.eclipse.hawkbit:hawkbit-parent)](https://sonar.ops.bosch-iot-rollouts.com) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.eclipse.hawkbit/hawkbit-parent/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.eclipse.hawkbit/hawkbit-parent)

# Documentation

see [hawkBit Documentation](https://www.eclipse.org/hawkbit/documentation/overview/introduction.html)

# Contact us

* Want to chat with the team behind hawkBit? [![Join the chat at https://gitter.im/eclipse/hawkbit](https://badges.gitter.im/eclipse/hawkbit.svg)](https://gitter.im/eclipse/hawkbit?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
* Having issues with hawkBit? Open a [GitHub issue](https://github.com/eclipse/hawkbit/issues).
* You can also check out our [Project Homepage](https://projects.eclipse.org/projects/iot.hawkbit) for further contact options.

# hawkBit sandbox

We offer a sandbox installation that is free for everyone to try out hawkBit. However, keep in mind that the sandbox database will be reset from time to time. It is also not possible to upload any artifacts into the sandbox. But you can use it to try out the Management UI, Management API and DDI API.

https://hawkbit.eu-gb.mybluemix.net/UI/

# Compile, Run and Getting Started

We are not providing an off the shelf installation ready hawkBit update server. However, we recommend to check out the [Example Application](examples/hawkbit-example-app) for a runtime ready Spring Boot based update server that is empowered by hawkBit. In addition we have [guide](http://www.eclipse.org/hawkbit/documentation/guide/runhawkbit.html) for setting up a complete landscape.

# API stability

hawkBit is currently in '0.X' semantic version. That is due to the need that there is still content in hawkBit that is in need for refactoring. That includes the maven module structure, Spring Boot Properties, Spring Boot auto configuration as well as internal Java APIs (e.g. the [repository API](https://github.com/eclipse/hawkbit/issues/197) ).

However, the external APIs (i.e. [Management API](https://github.com/eclipse/hawkbit/tree/master/hawkbit-mgmt-api), [DDI API](https://github.com/eclipse/hawkbit/tree/master/hawkbit-ddi-api), [DDI Artifact Download API](https://github.com/eclipse/hawkbit/tree/master/hawkbit-ddi-dl-api) and [DMF API](https://github.com/eclipse/hawkbit/tree/master/hawkbit-dmf-api) are on major version 'v1' and will be kept stable.

# hawkBit (Spring boot) starters

Next to the [Example Application](examples/hawkbit-example-app) we are also providing a set of [Spring Boot Starters](hawkbit-starters) to quick start your own [Spring Boot](https://projects.spring.io/spring-boot/) based application.

#### Clone and build hawkBit
```
$ git clone https://github.com/eclipse/hawkbit.git
$ cd hawkbit
$ mvn clean install
```
#### Start hawkBit Update Server
[Update Server](hawkbit-runtime/hawkbit-update-server)
```
$ java -jar ./hawkbit-runtime/hawkbit-update-server/target/hawkbit-update-server-#version#.jar
```
#### Start hawkBit device simulator
[Device Simulator](examples/hawkbit-device-simulator)
```
$ java -jar ./examples/hawkbit-device-simulator/target/hawkbit-device-simulator-#version#.jar
```
#### Generate Getting Started data
[Example Management API Client](examples/hawkbit-example-mgmt-simulator)
```
$ java -jar ./examples/hawkbit-example-mgmt-simulator/target/hawkbit-example-mgmt-simulator-#version#.jar
```

# Releases and Roadmap

* In the upcoming release 0.2:
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