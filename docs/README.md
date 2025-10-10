# IoT. Update. Device.

Eclipse hawkBit™ is a domain independent back-end framework for rolling out software updates to constrained edge devices as well as more powerful controllers and gateways connected to IP based networking infrastructure.

<p align="center">
  <img src="images/hawkBit_overview.jpeg" alt="eclipse foundation logo" width="1200">
</p>

---

## Interfaces

hawkBit offers a direct device integration via HTTP or a device management federation API which allows to connect devices with different protocol adapter. Users can make use of the graphical user interface and other service can interact with hawkBit through the RESTful management API.

---

## Rollout

<div style="display: flex; align-items: flex-start;">

<div style="flex: 1; padding-right: 20px;">

hawkBit supports an easy and flexible rollout management which allows you to update a large amount of devices in separated groups.

- Cascading start of the deployment groups based on installation status of the previous group.  
- Emergency shutdown of the rollout in case a group exceeds the defined error threshold.  
- Rollout progress monitoring for the entire rollout and the individual groups.  

</div>

<div style="flex: 1;">

<img src="images/rollout.png" alt="Rollout Diagram" width="700"/>

</div>

</div>

---

## Package Model

<div style="display: flex; align-items: flex-start;">

<div style="flex: 1;">

<img src="images/packagemodel.png" alt="Package Model Diagram" width="600"/>

</div>

<div style="flex: 1; padding-left: 20px;">

A software update does not always contain only a single file.  
The hawkBit meta model allows you to configure your files in virtual software and distribution packages.  

</div>

</div>
