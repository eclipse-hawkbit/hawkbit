---
layout: documentation
title: Introduction
---

{% include base.html %}

## Overview

![](../images/hawkbit_logo.png?){:width="300px" .image-left}
<br />
<br />
<br />
hawkBit is an domain independent back-end framework for rolling out software updates to constrained edge devices as well as more powerful controllers and gateways connected to IP based networking infrastructure.
<br />

## Motivation for Software Updates in IoT
Having software update capabilities ensures a secure IoT by means that it gives IoT projects a fighting chance against pandoras box that they opened the moment their devices got connected. From that moment on devices are at the forefront of IT security threats many embedded software developers historically never had to face. Shipping for instance a Linux powered device connected to the Internet without any security updates ever applied during its lifetime is kind of a suicidal act these days.

A more charming argument for software update is that it enables agile development for hardware and hardware near development. Concepts like a minimum viable product can be applied for devices as not all features need to be ready at manufacturing time. Changes on the cloud side of the IoT project can be applied to the devices at runtime as well.

Sometimes Software Update is a business model on its own as it makes devices much more attractive to the customer if they are updateable, i.e. they do not only buy a product because of its current feature set but make also a bet on its future capabilities. In addition new revenue streams may arise from the fact that feature extensions can potentially be monetized (e.g. Apps) without the need to design, manufacture and ship a new device (revision).

## Motivation for hawkBit

**Updating software** (components) on constrained edge devices as well as more powerful controllers and gateways is as mentioned before a **common requirement** in most IoT scenarios.

At the time being, this process is **usually handled by the IoT solution itself**, sometimes backed by a full fledged device management system. We believe that this approach generates unnecessary **duplicate work** in the IoT space, in particular when considering the challenges of implementing a safe and reliable remote software update process: the software update process must never fail and also must never be compromised as, at the one hand, it can be used to fix almost any issue/problem on the device but at the same time also poses the greatest security threat if mis-used to introduce malicious code to the device.

In addition we believe the software update process to be relatively **independent from particular application domains** when seen from the back-end (cloud) perspective. Updating the software for an entire car may differ from updating the firmware of a single sensor with regard to the connectivity of the device to the cloud and also to the complexity of the software package update process on the device. However, the process of rolling out the software, e.g. uploading an artifact to the repository, assigning it to eligible devices, managing the roll out campaign for a large number of devices, orchestrating content delivery networks to distribute the package, monitoring and reporting the progress of the roll-out and last but not least requirements regarding security and reliability are quite similar.

Software update itself is often seen as a sub process of general device management. In fact, most device management systems include functionality for triggering groups of devices to perform an update, usually accompanied by an artifact repository and basic reporting and monitoring capabilities. This is true for both systems specifically targeting IoT as well as systems originating from the mobile area.

Existing **device management systems** usually **lack** the capability to **efficiently organize roll outs at IoT scale**, e.g. splitting the roll out into sub groups, cascading them, automatically stopping the roll out after a defined error threshold etc. They are also usually restricted to a single device management protocol, either a proprietary one or one of the existing standard protocols like LWM2M, OMA-DM or TR-069. Even if they support more than one such protocol, they are often a result of the device management protocol they started with and restricted in their adoption capabilities to others.

At the same time the wide functional scope of a full fledged **device management system introduces unnecessary (and unwanted) complexity** to many IoT projects. This is particularly true for IoT solutions working with constrained devices where requirements regarding generic device management are often very limited only but a secure & reliable software update process is still mandatory.

As a result we have the need for a domain independent solution
*	that works for the majority of IoT projects
*	that goes beyond the pure update and handles more complex **roll out strategies** needed by large scale IoT projects.
*	that at the same time is **focused on software updates** in the IoT space
*	and that is able to work on its own for simple scenarios while having the capability to integrate with existing device management systems and protocols.

# Requirements to a _cloud ready_ IoT Software Update system

* **Technical Scalability**: connect millions of devices and ship terabytes of software on a global scale.
* **Functional Scalability**: rollouts with hundreds of thousands of individual devices in it.
* **Reliability**: software update as the last line of defense against device faults and vulnerabilities.
* **Managed device complexity**: device topologies inside each individual provisioning target.
* **Integration flexibility**: connect and integrate through various (non-)standardized device management protocols directly or through federated device managements.