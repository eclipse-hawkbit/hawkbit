<img src=hawkbit_logo.png width=533 height=246 />

# Eclipse hawkBit™ - Update Server

Eclipse [hawkBit](http://www.eclipse.org/hawkbit/index.html) is an domain independent back end solution for rolling out
software updates to constrained edge devices as well as more powerful controllers and gateways connected to IP based
networking infrastructure.

Build:
[![Build Status](https://github.com/eclipse-hawkbit/hawkbit/actions/workflows/verify.yml/badge.svg?branch=master)](https://github.com/eclipse-hawkbit/hawkbit/actions/workflows/verify.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=eclipse-hawkbit_hawkbit&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=eclipse-hawkbit_hawkbit)
[![Maven Central](https://img.shields.io/maven-central/v/org.eclipse.hawkbit/hawkbit-parent?label=maven-central&color=blue)](https://search.maven.org/search?q=g:org.eclipse.hawkbit)
[![Lines of code](https://img.shields.io/badge/dynamic/xml.svg?label=Lines%20of%20code&url=https%3A%2F%2Fwww.openhub.net%2Fprojects%2Fhawkbit.xml%3Fapi_key%3D30bc3f3fad087c2c5a6a67a8071665ba0fbe3b6236ffbf71b7d20849f4a5e35a&query=%2Fresponse%2Fresult%2Fproject%2Fanalysis%2Ftotal_code_lines&colorB=lightgrey)](https://www.openhub.net/p/hawkbit)

License:
[![License](https://img.shields.io/badge/License-EPL%202.0-green.svg)](https://opensource.org/licenses/EPL-2.0)

Docker:
[![Docker](https://img.shields.io/docker/v/hawkbit/hawkbit-update-server/latest?color=blue)](https://hub.docker.com/r/hawkbit/hawkbit-update-server)
[![Docker MYSQL](https://img.shields.io/docker/v/hawkbit/hawkbit-update-server/latest-mysql?color=blue)](https://hub.docker.com/r/hawkbit/hawkbit-update-server)
[![Docker pulls](https://img.shields.io/docker/pulls/hawkbit/hawkbit-update-server.svg)](https://hub.docker.com/search?q=hawkbit%2Fhawkbit-update-server&type=image)

# Documentation

see [hawkBit Documentation](https://www.eclipse.dev/hawkbit/)

# SpringBus (with Spring Cloud Stream under the hood)

- sends ApplicationEvent via ApplicationEventPublisher
    - Event is sent firs locally to all @EventListener annotated methods within the same application context
    - If ApplicationEvent is instance of RemoteApplicationEvent - Event is sent to all remote applications via Spring Cloud Bus
- Depending on the service binder (e.g. fanout / group)
    - fanout - service binder will create anonymous queue only for its replica and will receive all events
    - group - service binder will bind to existing queue with that (group) name and only 1 replica will receive the event from that group
- Possible ISSUES:
    - Possible duplications
        - replica (dmf with group dmf) that sends the event will first handle it locally, then send it to the bus
        - bus will send the event to all replicas of the group (dmf)
            - (OK) if same replica (that handled it locally) is part of the group, it will receive the event again (but ignore it because isFromSelf() - originservice)
            - (NOT OK) if different replica (that is part of the group) receives the event, it will handle it again (making it a duplicate event)
    - Single channel - springCloudInputChannel
        - not possible to design cache events channel and process events channel (e.g. grouped)

# Spring Cloud Stream
- sends AbstractRemoteEvent (new event pojo) via Spring Cloud Stream
    - Event is not handled locally, but sent remotely via Spring Cloud Stream
    - Event is distributed and handle depending on service binder (e.g. fanout / group)
        - Currently we reuse the same service binder settings as before - e.g. with event batching (1000 events with default timeout of 5s)
            - this means that events are not sent immediately, but batched and sent in bulk and delay of 5 sec is possible compared to SpringBus previous implementation
        - Remote Event is received and re-published via ApplicationEventPublisher so that all @EventListener annotated methods can handle it (just like before)

| SpringBus                                                         |                         SpringCloudStream + StreamBridge                          |
|-------------------------------------------------------------------|:---------------------------------------------------------------------------------:|
| Publishes event via ApplicationEventPublisher                     |   (If event is not AbstractRemoteEvent) Publishes via ApplicationEventPublisher   |
| Handles locally first                                             |           (If event is AbstractRemoteEVent) Publishes via StreamBridge            |
| If (RemoteApplicationEvent) - forward to bus                      | Replicas receive event (depending on their fanout/group strategy on that channel) |
| Replicas receive event (depending on their fanout/group strategy) |                                                                                   |
| (No delays, possible duplication processes of the Event           |          (Delay because Event is awaited to bounce back to any replica)           |
