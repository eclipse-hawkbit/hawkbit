---
title: Vaadin 8 UI discontinuation
parent: Blog
weight: 99
---

In this article, we want to give an overview of the future of the hawkBit UI

## hawkBit Vaadin 8 UI discontinuation

The hawkBit UI uses Vaadin as a web UI framework. It uses Vaadin 8 (8.14.3). This major version, according [Vaadin Roadmap](https://vaadin.com/roadmap), has no free support since 21st Feb 2022. There are some version releases after that date (8.15.0 - 8.16.0) that are Apache 2.0 licensed. However, since 8.16.1 ([see here](https://mvnrepository.com/artifact/com.vaadin/vaadin-server)) the license is [Commercial Vaadin Developer License 4.0](https://vaadin.com/license/cvdl-4.0), so they could not be used in hawkBit.

We believe it is not a good practice to keep an out of free support library in an open source project like hawkBit. And moreover, even if we keep it, if a security vulnerability is discovered - all users shall opt for commercial support or to drop UI.

There is another critical obstacle with keeping Vaadin 8 UI. At the moment hawkBit uses Spring Boot 2.7. According to [Spring Boot EOL](https://endoflife.date/spring-boot) Spring Boot 2.7 stream will reach end of support 24th Nov 2023. So, hawkBit shall be migrated to Spring Boot 3.0+. Since Vaadin 8 seem to be incompatible with Spring Boot 3 (they added support for Spring Boot 3 in Vaadin 24 ([Vaadin 24 pre release](https://vaadin.com/blog/vaadin-24-pre-release-available-for-spring-boot-3.0)) we shall drop Vaadin UI 8 anyway.

Many months ago we asked for community help to migrate hawkBit UI to newer Vaadin versions - [Urgent migration needed to a newer Vaadin version
](https://github.com/eclipse-hawkbit/hawkbit/issues/1376) and gitter channel. However, there was no volunteer found to do the migration.

All this being said, unfortunately, we've come to the decision to drop the Vaadin 8 UI from the Eclipse hawkBit and the latest hawkBit release 0.3.0 is the last version of hawkBit that includes it. For the next 0.4.0 release we plan to remove this Vaadin 8 UI. Thus the hawkBit may become an UI-less project.

There were steps taken to mitigate the problem: 
* extending the REST API
* introducing Swagger UI which allow easier use of the REST API

however, the user experience will become much worse.

We hope to see future contributions with migration of the UI or, why not, even new generation UI! 

