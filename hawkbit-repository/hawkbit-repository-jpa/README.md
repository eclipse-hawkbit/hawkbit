# hawkBit metadata repository

The repository is in charge for managing the meta data of the update server, e.g. provisioning targets, distribution sets, software modules etc.

# Build

[indent=0]
----
	$ mvn clean install
----

Note, in order to build correctly in your IDE, you have to add ./target/generated-sources/apt to your build path.

#Concepts

## Rollout
A rollout consists of the distribution set and a target-query-filter which defines the targets to be updated in this rollout. The targets within this filter are split up in configured amount of _Deployment Groups_ at creation time.

When starting a rollout, for all targets within this rollout deployment actions will be created. The deployment actions of the first group will be started immediately all other deployment actions will be scheduled.

> Due rollouts might include a large number of targets and deployment group, creation as well as starting a rollout might take some time and therefore the creation and starting of an rollout is executed asynchronously. The creation and starting progress is reflected by the rollout's status attribute

### Rollout Creation
The targets reflected by the target-query-filter is interpreted at creation time. Only targets which have been created at that time are included in the rollout. Targets which are created after the rollout creation will not be included. At rollout creation time all necessary deployment groups containing the targets will be created. Dynamically adding targets to a created or running rollout is currently not supported.

### Rollout Starting
The rollout is using the same concept based on _deployment acionts_ per target. All necessary _deployment acionts_ will be created at starting point but not all _deployment acionts_ will be set to running. Only the _deployment acionts_ of the first _deployment group_ is set to running, all other actions are created in _scheduled_ state.   
If targets having not finished _deployment actions_ at rollout starting time, these action are tried to cancel (state: canceling). Scheduled actions from another rollout are canceled directly on server side because they were never running before and can be safely canceled.

>Multiple rollouts can be running at the same time, but if the rollouts effect the same targets they will interfer the targets _deployment actions_ e.g. canceling/cancel already running/scheduled _deployment actions_.

### Deployment Group Condition/Action
#### Success Condition/Action
A _success condition_ defines when a deployment group is interpreted as success and triggers the _success action_. E.g. a threshold of successfully updated targets within the group. 

The _success action_ is executed if the _success_condition_ is fullfilled. Currently only the _success action_ starting the next following deployment group is available.  

#### Error Condition/Action
A _error condition_ defines when a deployment group is interpreted as failure and triggers the _error action_. E.g. a threshold of update failures of targets within the group. 

The _error action_ is executed if the _error condition_ is fullfilled. Currently only the _error action_ pausing the whole rollout is available, a paused rollout can be resumed by a user.

### Rollout Scheduler
The rollout is managed by a scheduler task which is checking for rollouts in _running_ state. This is done atomic by updating the row in the database, so only the rollouts are checked and processed for the current instance to avoid that multiple instances are processing a rollout.

>Due the scheduler, it might take some time until a rollouts is processed and switching is state or starting the next deployment group.

