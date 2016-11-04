---
layout: documentation
title: Data Model
---

{% include base.html %}
# Data Model
The hawkBit data model was designed to have enough flexibility to define complex software structures (e.g. operating system, runtimes, apps, different kind of artifacts) on one side and simplicity compared to the capabilities of a full blown configuration management on the other.

It does define a hierarchy of software that starts with a distribution, which can have (sub-)modules and these may have multiple artifacts. However, it does not consider any kind of dependency definitions between modules or artifacts. As a result dependency checks if necessary have to be done outside hawkBit, i.e. on the device itself or before the entity creation in hawkBit by the origin.

Provisioning Target Definition

A Provisioning Target is a neutral definition that may be an actual real device (e.g. gateway, embedded sensor) or a virtual device (e.g. vehicle, smart home).

The definition in hawkBit might reflect the transactional behavior if necessary on the device side. A vehicle might be updated device by device or as a whole. As a result one way of defining a vehicle in hawkBit could be to have one all inclusive Software Module or one module per (sub-) device.

Software Structure Definition

The structure defines the model of the supported software by the provisioning target

Distribution Set Type: defines a package structure that is supported by certain devices
Consists of Software Module Types both for
Firmware - device can have only one module of that type (e.g. the operating system)
Software - device can have multiple modules of that type (e.g. "Apps")
Software Content Definition

Distribution Set: can be deployed to a provisioning target
Software Module: is a sub element of the distribution
e.g. OS, application, firmware X, firmware Y
Artifact: binaries for a software module. Note: the decision which artifacts have to be downloaded are done on the device side.
e.g. Full package, signatures, binary deltas


Entity Relationships
The public defined entities and their relation which are reflected by the Management API.



Deleting and Archiving Software Modules
When a user deletes a Software Module, the update server cannot simply remove all the corresponding data. Because when the Software Module is already assigned to a Distribution Set or was assigned to a Target in the past, the hawkBit server has to make sure that remains a clean and full update history for every target. The history contains all information (e.g. name, version) of the software, which was assigned to a specific Target. Obviously storing the binary data of the artifacts is not necessary for the history purpose.

The delete process which is performed, when there are historical connections to targets is called SoftDelete. This process marks the Software Module as deleted and removes the artifact, but it won't delete the meta data, which describes the SoftwareModule and the associated Artifacts. SoftwareModules, which are marked as delete won't be visible for the user, when he is requesting all SoftwareModules.

Just in case there are no connections to Distribution Sets and targets the server will perform a HardDelete. This process deletes all stored data, including all meta information.

Note: in case of of a SoftDelete the unique constraints are still in place, i.e. you cannot create an entity with the same name/key. This constraint might be removed in future versions because of the impact on the user experience (i.e. he does not see the soft deleted module but cannot create a new one).