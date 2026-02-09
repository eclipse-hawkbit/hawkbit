# Rollout management
Software update operations in large scale IoT scenarios with hundreds of thousands of devices require special handling.

That includes:

- _Technical Scalability_ by means of horizontal scale of the hawkBit server cluster in the cloud.
- _Global_ artifact _content delivery_ capacities.
- _Functional Scalability_ by means of:
    - Secure handling of large volumes of devices at rollout creation time.
    - Monitoring of the rollout progress.
    - Emergency rollout shutdown in case of problems on to many devices.

- Reporting capabilities for a complete understanding of the rollout progress at each point in time.

Eclipse hawkBit sees these capabilities under the term Rollout Management.

The following capabilities are currently supported by the _Rollout Management_:

- Create, update and start of rollouts.
    - Selection of targets as input for the rollout based on _target filter_ functionality.
    - Selection of a _DistributionSet_.
    - Auto-splitting of the input target list into a defined number deployment groups.
- Approval workflow
    - Has to be enabled explicitly in configuration.
    - Enables a workflow that requires a user with adequate permissions to review any new or updated rollout before it
      can be started.
    - Allows integration with 3rd party workflow engines.

- Cascading start of the deployment groups based on installation status of the previous group.
- Emergency shutdown of the rollout in case a group exceeds the defined error threshold.
- Rollout progress monitoring for the entire rollout and the individual groups.

---

## Cascading Deployment Group Execution

The cascading execution of the deployment groups is based on two thresholds that can be defined by the rollout creator.

- **Success Condition**: Defined by the percentage of successfully installed targets in the current group. When this threshold is reached, a success action is executed.
    - (Default) **NextGroup** success action, enabling a fully cascading rollout, starting the next group automatically.
    - **Pause** success action allows the rollout to pause after a group’s success condition is met, enabling additional external verification or manual checks before proceeding. The rollout will await a manual resume action, which then starts the next group.
- **Error Condition**: Defined by an absolute number or percentage of failed installations. This triggers an emergency shutdown of the entire rollout.

---

## Rollout state machine

### State Machine on Rollout

<p align="center">
  <img src="images/rollout-state-machine.png" alt="Clustering Diagram" width="1200"/>
</p>

### State Machine on Rollout Deployment Group
<p align="center">
  <img src="images/deploymnet-group-state-machine.png" alt="Clustering Diagram" width="1200"/>
</p>

---
