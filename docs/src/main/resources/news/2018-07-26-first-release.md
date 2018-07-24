---
layout: news
title: First release
date: 2018-07-26 08:00:00
section: news
author:
name: Jeroen Laverman
id: laverman
---
{% include base.html %}

# Eclipse hawkBit 0.2.0 - First Release

hawkBit is a domain independent back-end framework for rolling out software updates to constrained edge devices as well 
as more powerful controllers and gateways connected to IP based networking infrastructure. It is part of the Eclipse IoT 
since 2015 and with _0.2.0_ a first release is available.

In this article, we want to give an overview over the latest highlights of hawkBit and let you know how you can get 
started in seconds.

## Finally, it is here! 

After being around in the Eclipse IoT realm for quite some time now, we are more than happy to announce our first release: 
_Eclipse hawkBit 0.2.0_. The release can be found on [Maven Central](https://mvnrepository.com/artifact/org.eclipse.hawkbit) 
and [Docker Hub](https://hub.docker.com/r/hawkbit/hawkbit-update-server/). It includes the following core features:

* Device and Software Repository
* Update Management
* Artifact Content Delivery
* Rollout Management

which are accessible via the following interfaces:

* Management UI
* Management API
* Direct Device Integration (DDI) API
* Device Management Federation (DMF) API

![hawkBit Overview](../img/hawkBit_overview.jpeg)


## What's new?

Whenever there is a new release, the first question that comes to mind is: What's new? Since this is our first release, 
one could argue that everything is new. However, most of the features are already well established. This holds true, for 
example, for our APIs or the Rollout Management. Nevertheless, there have been some recent updates to hawkBit, which we 
do not want to leave unmentioned: 

### Streamlined UI

The probably most noticeable change has been the removal of the two buttons (`Drop here to delete` and `Actions`) at the 
bottom of the _Deployment_, _Distributions_, and _Upload_ view. This is a major usability improvement! For example, 
deleting an item required (1) dragging an item onto the delete button, (2) opening the delete pop-up, and (3) confirming 
the deletion. Now, an item can be easily removed by clicking on its remove icon and confirming the action. This is not 
only faster and more intuitive, it also saves a lot of display real estate which can now be used to focus on what is 
important. We hope you like this change as much as we do! _(Requires: hawkBit > 0.2.2)_

![Screenshot of improved UI](../img/hawkbit_ui.png)

### MS SQL Server

Eclipse hawkBit supports a range of different SQL databases. Up to now these were the internal H2 database (which can be 
used for testing, development, or trial) and MySQL/MariaDB for production grade usage. This list is now extended by 
Microsoft's SQL Server which is also available in production grade, as well as, IBM's DB2 for testing and development.

### Docker images

In order to enable interested parties to get started with hawkBit convenitely, we decided to provide the 
[Update Server as a Docker image](https://hub.docker.com/r/hawkbit/hawkbit-update-server/) on Docker Hub. The image comes 
in two flavors: the default image uses the internal H2 database, while the images with `-mysql` suffix contain the MySQL 
driver to allow connecting a MySQL database. In addition to the Docker image, the hawkBit repository contains a 
[docker-compose.yml](https://github.com/eclipse/hawkbit/blob/master/hawkbit-runtime/hawkbit-update-server/docker/docker-compose.yml) 
that not only starts the Update Server, but futher includes a MySQL database and a  RabbitMQ message broker so you're 
able to use Device Management Federation (DMF) as well. 

To start the hawkBit Update Server image, open a terminal and run: 

```
$ docker run -d -p 8080:8080 hawkbit/hawkbit-update-server
```
> _Note: This requires a running [Docker deamon](https://docs.docker.com/install/) on your system._

Now, browse to [http://localhost:8080](http://localhost:8080) and log-in with `admin:admin`.

## Community updates

Although features and functionality play a major role in the hawkBit project, there are also some interesting news from 
the community. As of July 2018, there have been:

* Pull Requests: 587
* Forks: 54
* Stars: 137
* Gitter Chat members: 119
* Contributors: 25

### New project lead and committers

We are happy to announce that the hawkBit project got a new project lead. In addition to 
[Kai Zimmermann](https://projects.eclipse.org/user/6364), project lead from the first hour, 
[Jeroen Laverman](https://projects.eclipse.org/user/10982) joined the lead to support him in this responsibility. 
Moreover, with [Stefan Behl](https://projects.eclipse.org/user/10842) and Jeroen Laverman, two new committers are aboard. 

### GitHub bot

Besides real people, the community gets support by [hawkBit-bot](https://github.com/hawkbit-bot), a GitHub bot helping to 
manage pull-requests and issues on GitHub keeping everything nice and tidy.


## What's next?

Looking ahead, there are two major topics that we want to tackle next: First, there is the migration of our Ui from Vaadin 
7 to Vaadin 8, since Vaadin announced the end-of-life for our current version. Another big topic will be the update
to Spring Boot 2. 

