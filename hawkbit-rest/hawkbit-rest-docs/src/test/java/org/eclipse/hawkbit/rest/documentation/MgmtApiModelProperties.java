/**
 * Copyright (c) 2018 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.rest.documentation;

/**
 * Model properties for the Management API documentation.
 */
public final class MgmtApiModelProperties {

    // Versioned entity
    public static final String VERSION = "Package version.";
    public static final String VENDOR = "The software vendor.";

    public static final String ACTION_ID = "ID of the action.";

    public static final String LINKS_ASSIGNED_DS = "Links to assigned distribution sets.";
    public static final String LINKS_INSTALLED_DS = "Links to installed distribution sets.";
    public static final String LINKS_ATTRIBUTES = "Links to attributes of the target.";
    public static final String LINKS_ACTIONS = "Links to actions of the target.";
    public static final String LINK_TO_ACTION = "The link to the action.";
    public static final String LINK_TO_DS = "The link to the distribution set.";
    public static final String LINKS_ACTION_STATUSES = "The link to all statuses of the action.";
    public static final String LINK_TO_ARTIFACTS = "The link to all artifact of a software module.";
    public static final String LINK_TO_SM_TYPE = "The link to the software module type.";
    public static final String LINK_TO_METADATA = "The link to the metadata.";
    public static final String LINK_TO_MANDATORY_SMT = "Link to mandatory software modules types in this distribution set type.";
    public static final String LINK_TO_OPTIONAL_SMT = "Link to optional software modules types in this distribution set type.";
    public static final String LINK_TO_ROLLOUT = "The link to the rollout.";
    public static final String LINK_TO_TARGET_TYPE = "The link to the target type.";
    public static final String LINK_TO_TARGET = "The link to the target.";

    public static final String LINK_TO_AUTO_CONFIRM = "The link to the detailed auto confirm state.";

    // software module types
    public static final String SMT_TYPE = "The type of the software module identified by its key.";
    public static final String SMT_VENDOR = "The software vendor of the entity.";
    public static final String SMT_VERSION = "The version of the software module type.";
    public static final String SMT_KEY = "The key of the software module type.";
    public static final String SMT_MAX_ASSIGNMENTS = "Software modules of that type can be assigned at this maximum number (e.g. operating system only once).";

    // software module
    public static final String SM_TYPE = "The software module type " + ApiModelPropertiesGeneric.ENDING;
    public static final String SM_TYPE_NAME = "The software module type name " + ApiModelPropertiesGeneric.ENDING;
    public static final String ENCRYPTED = "Encryption flag, used to identify that artifacts should be encrypted upon upload.";
    public static final String ARTIFACT_HASHES = "Hashes of the artifact.";
    public static final String ARTIFACT_SIZE = "Size of the artifact.";
    public static final String ARTIFACT_PROVIDED_FILE = "Binary of file.";
    public static final String ARTIFACT_PROVIDED_FILENAME = "Filename of the artifact.";
    public static final String ARTIFACT_HASHES_SHA1 = "SHA1 hash of the artifact.";
    public static final String ARTIFACT_HASHES_MD5 = "MD5 hash of the artifact.";
    public static final String ARTIFACT_HASHES_SHA256 = "SHA256 hash of the artifact.";

    public static final String ARTIFACT_DOWNLOAD_LINK = "Download link of the artifact.";

    public static final String ARTIFACT_LIST = "List of artifacts of given software module.";

    // Distribution Set
    public static final String DS_OS = "Operating system or firmware software module - DEPRECATED (use modules).";
    public static final String DS_RUNTIME = "Runtime software module (e.g. JVM) - DEPRECATED (use modules).";
    public static final String DS_APPLICATION = "Application software module (e.g. OSGi container) - DEPRECATED (use modules).";
    public static final String DS_MODULES = "Software modules (e.g. OSGi bundles, runtimes).";
    public static final String DS_REQUIRED_STEP = "True if DS is a required migration step for another DS. As a result the DS's assignment will not be cancelled when another DS is assigned (note: updatable only if DS is not yet assigned to a target)";
    public static final String DS_ASSIGNED_TARGETS = "Targets that have this distribution set assigned.";
    public static final String DS_INSTALLED_TARGETS = "Targets that have this distribution set installed.";
    public static final String DS_LIST = "List of distribution sets.";
    public static final String DS_TAG_LIST = "List of distribution set tags";
    public static final String DS_NEW_ASSIGNED_TARGETS = "Targets that now have this distribution set assigned.";
    public static final String DS_ALREADY_ASSIGNED_TARGETS = "Targets that had this distribution set already assigned (in \"offline\" case this includes targets that have arbitrary updates running)";
    public static final String DS_TOTAL_ASSIGNED_TARGETS = "Overall assigned as part of this request.";
    public static final String DS_ID = "Id of the distribution set.";
    public static final String DS_INVALIDATION_ACTION_CANCELATION_TYPE = "Type of cancelation for actions referring to the given distribution set.";
    public static final String DS_INVALIDATION_CANCEL_ROLLOUTS = "Defines if rollouts referring to this distribution set should be canceled.";

    // Target
    public static final String INSTALLED_AT = "Installation time of current installed DistributionSet.";

    public static final String LAST_REQUEST_AT = "Last time where the target polled the server, same as pollStatus.lastRequestAt.";
    // poll status
    public static final String POLL_LAST_REQUEST_AT = "Last time when the target polled the server.";
    public static final String POLL_NEXT_EXPECTED_REQUEST_AT = "Next expected time when the target polls the server.";
    public static final String POLL_STATUS = "Poll status of the target. In many scenarios that target will poll the update server on a regular basis to look for potential updates. If that poll does not happen it might imply that the target is offline.";
    public static final String POLL_OVERDUE = "Defines if the target poll time is overdue based on the next expected poll time plus the configured overdue poll time threshold.";

    // Target type
    public static final String TARGETTYPE_ID = "ID of the target type";
    public static final String TARGETTYPE_NAME = "Name of the target type";
    public static final String COMPATIBLE_DS_TYPES = "Array of distribution set types that are compatible to that target type";
    public static final String LINK_COMPATIBLE_DS_TYPES = "Link to the compatible distribution set types in this target type";

    // rollout
    public static final String ROLLOUT_FILTER_QUERY = "target filter query language expression";
    public static final String ROLLOUT_CONFIRMATION_REQUIRED = "(available with user consent flow active) if the confirmation is required for this rollout. Value will be used if confirmation options are missing in the rollout group definitions. Confirmation is required per default";
    public static final String ROLLOUT_GROUP_CONFIRMATION_REQUIRED = "(available with user consent flow active) if the confirmation is required for this rollout group. Confirmation is required per default.";
    public static final String ROLLOUT_GROUP_FILTER_QUERY = "target filter query language expression that selects a subset of targets which match the target filter of the Rollout";
    public static final String ROLLOUT_GROUP_TARGET_PERCENTAGE = "percentage of remaining and matching targets that should be added to this group";
    public static final String ROLLOUT_DS_ID = "the ID of distributionset of this rollout";
    public static final String ROLLOUT_TOTAL_TARGETS = "the total targets of a rollout";
    public static final String ROLLOUT_TOTAL_TARGETS_PER_STATUS = "the total targets per status";
    public static final String ROLLOUT_STATUS = "the status of this rollout";
    public static final String ROLLOUT_TYPE = "the type of this rollout";
    public static final String ROLLOUT_GROUP_STATUS = "the status of this rollout group";
    public static final String ROLLOUT_AMOUNT_GROUPS = "the amount of groups the rollout should split targets into";
    public static final String ROLLOUT_GROUPS = "the list of group definitions";
    public static final String ROLLOUT_SUCCESS_CONDITION = "the success condition which takes in place to evaluate if a rollout group is successful and so the next group can be started";
    public static final String ROLLOUT_SUCCESS_CONDITION_CONDITION = "the type of the condition";
    public static final String ROLLOUT_SUCCESS_CONDITION_EXP = "the expression according to the condition, e.g. the value of threshold in percentage";
    public static final String ROLLOUT_SUCCESS_ACTION = "the success action which takes in place to execute in case the success action is fulfilled";
    public static final String ROLLOUT_SUCCESS_ACTION_ACTION = "the success action to execute";
    public static final String ROLLOUT_SUCCESS_ACTION_EXP = "the expression for the success action";
    public static final String ROLLOUT_ERROR_CONDITION = "the error condition which takes in place to evaluate if a rollout group encounter errors";
    public static final String ROLLOUT_ERROR_CONDITION_CONDITION = "the type of the condition";
    public static final String ROLLOUT_ERROR_CONDITION_EXP = "the expression according to the condition, e.g. the value of threshold in percentage";
    public static final String ROLLOUT_ERROR_ACTION = "the error action which is executed if the error condition is fulfilled";
    public static final String ROLLOUT_ERROR_ACTION_ACTION = "the error action to execute";
    public static final String ROLLOUT_ERROR_ACTION_EXP = "the expression for the error action";
    public static final String ROLLOUT_LINKS_START_SYNC = "Link to start the rollout in sync mode";
    public static final String ROLLOUT_LINKS_START_ASYNC = "Link to start the rollout in async mode";
    public static final String ROLLOUT_LINKS_PAUSE = "Link to pause a running rollout";
    public static final String ROLLOUT_LINKS_TRIGGER_NEXT_GROUP = "Link for triggering next rollout group on a running rollout";
    public static final String ROLLOUT_LINKS_RESUME = "Link to resume a paused rollout";
    public static final String ROLLOUT_LINKS_APPROVE = "Link to approve a rollout";
    public static final String ROLLOUT_LINKS_DENY = "Link to deny a rollout";
    public static final String ROLLOUT_LINKS_GROUPS = "Link to retrieve the groups a rollout";
    public static final String ROLLOUT_START_ASYNC = "Start the rollout asynchronous";

    public static final String RESULTING_ACTIONS_WEIGHT = "Weight of the resulting Actions";

    public static final String UPDATE_STATUS = "Current update status of the target.";
    public static final String TARGET_ATTRIBUTES = "Target attributes.";

    public static final String TARGET_LIST = "List of provisioning targets.";

    public static final String TARGET_TAG_LIST = "List of target tags";

    public static final String TARGET_TYPE_LIST = "List of target types";

    public static final String SM_LIST = "List of software modules.";

    public static final String ROLLOUT_LIST = "list of rollouts";

    public static final String ACTION_TYPE = "Type of action.";

    public static final String ACTION_FORCE_TYPE = "Force type of the action that provides a hint if the controller should apply the action immediately or whenever possible.";

    public static final String ACTION_CONFIRMATION_REQUIRED = "(Available with user consent flow active) Defines, if the confirmation is required for an action. Confirmation is required per default.";

    public static final String ACTION_FORCE_TIME = "In case of timeforced mode the difference, measured in milliseconds, between the time the action should switch to forced and midnight, January 1, 1970 UTC.";

    public static final String ACTION_FORCED = "Set to forced in order to switch action to forced mode.";

    public static final String ACTION_STATUS_TYPE = "Type of the action status.";

    public static final String ACTION_STATUS_MESSAGES = "Messages related to the status.";

    public static final String ACTION_STATUS_REPORTED_AT = "Time at which the status was reported (server time).";

    public static final String ACTION_STATUS_CODE = "(Optional) Code provided by the device related to the status.";

    public static final String ACTION_LAST_STATUS_CODE = "(Optional) Code provided as part of the last status update that was sent by the device.";

    public static final String ACTION_STATUS_LIST = "List of action status.";

    public static final String ACTION_EXECUTION_STATUS = "Status of action.";

    public static final String ACTION_DETAIL_STATUS = "Detailed status of action.";

    public static final String ACTION_LIST = "List of actions.";

    public static final String ACTION_WEIGHT = "Weight of the action showing the importance of the update.";

    public static final String ACTION_ROLLOUT = "The ID of the rollout this action was created for.";

    public static final String ACTION_ROLLOUT_NAME = "The name of the rollout this action was created for.";

    public static final String IP_ADDRESS = "Last known IP address of the target. Only presented if IP address of the target itself is known (connected directly through DDI API).";

    public static final String ADDRESS = "The last known address URI of the target. Includes information of the target is connected either directly (DDI) through HTTP or indirectly (DMF) through amqp.";

    public static final String SECURITY_TOKEN = "Pre-Shared key that allows targets to authenticate at Direct Device Integration API if enabled in the tenant settings.";

    public static final String REQUEST_ATTRIBUTES = "Request re-transmission of target attributes.";

    public static final String AUTO_CONFIRM_ACTIVE = "Present if user consent flow active. Indicates if auto-confirm is active";

    public static final String META_DATA = "List of metadata.";

    public static final String META_DATA_KEY = "Metadata property key.";

    public static final String META_DATA_VALUE = "Metadata property value.";

    public static final String SM_META_DATA_TARGET_VISIBLE = "Metadata property is visible to targets as part of software update action.";

    public static final String AUTO_CONFIRM_STATE_ACTIVE = "Flag if auto confirm is active";

    public static final String AUTO_CONFIRM_STATE_INITIATOR = "(Optional) initiator set on activation";

    public static final String AUTO_CONFIRM_STATE_REMARK = "(Optional) remark set on activation";

    public static final String AUTO_CONFIRM_STATE_ACTIVATED_AT = "timestamp of the activation";

    public static final String AUTO_CONFIRM_STATE_REFERENCE_ACTIVATE_AUTO_CONFIRM = "reference link to activate auto confirm (present if not active)";

    public static final String AUTO_CONFIRM_STATE_REFERENCE_DEACTIVATE_AUTO_CONFIRM = "reference link to deactivate auto confirm (present if active)";

    public static final String AUTO_CONFIRM_ACTIVATE_INITIATOR = "individual value (e.g. username) stored as initiator and automatically used as confirmed user in future actions";
    public static final String AUTO_CONFIRM_ACTIVATE_REMARK = "individual value to attach a remark which will be persisted when automatically confirming future actions";

    public static final String SM_TYPE_KEY = "Key that can be interpreted by the target.";

    public static final String SM_MAX_ASSIGNMENTS = "Maximum number of assignments to a distribution set/target, e.g. only one firmware but multiple applications.";

    public static final String SM_TYPE_LIST = "List of software modules types.";

    public static final String DS_TYPE_KEY = "Functional key of the distribution set type.";

    public static final String DS_TYPE_LIST = "List of distribution set types.";

    public static final String DS_TYPE = "The type of the distribution set.";

    public static final String DS_TYPE_NAME = "The type name of the distribution set.";

    public static final String DS_COMPLETE = "True of the distribution set software module setup is complete as defined by the distribution set type.";

    public static final String DS_VALID = "True by default and false after the distribution set is invalidated by the user.";

    public static final String DS_TYPE_MANDATORY_MODULES = "Mandatory module type IDs.";

    public static final String DS_TYPE_OPTIONAL_MODULES = "Optional module type IDs.";

    public static final String MAINTENANCE_WINDOW = "Separation of download and install by defining a maintenance window for the installation.";
    public static final String MAINTENANCE_WINDOW_SCHEDULE = "Schedule for the maintenance window start in quartz cron notation, such as '0 15 10 * * ? 2018' for 10:15am every day during the year 2018.";
    public static final String MAINTENANCE_WINDOW_DURATION = "Duration of the window, such as '02:00:00' for 2 hours.";
    public static final String MAINTENANCE_WINDOW_TIMEZONE = "A time-zone offset from Greenwich/UTC, such as '+02:00'.";
    public static final String MAINTENANCE_WINDOW_NEXT_START_AT = "The time (timestamp UTC in milliseconds) of the next maintenance window start";

    // target filter query
    public static final String TARGET_FILTER_QUERY = "target filter query expression";
    public static final String TARGET_FILTER_QUERIES_LIST = "List of target filter queries.";
    public static final String TARGET_FILTER_QUERY_AUTO_ASSIGN_DS_ID = "Auto assign distribution set id";
    public static final String TARGET_FILTER_QUERY_LINK_AUTO_ASSIGN_DS = "Link to manage the auto assign distribution set";
    public static final String TARGET_FILTER_QUERY_PARAM_Q = "Name filter";

    // request parameter
    public static final String FORCETIME = "Forcetime in milliseconds.";
    public static final String FORCE = "Force as boolean.";
    public static final String ASSIGNMENT_WEIGHT = "Importance of the assignment.";
    public static final String ASSIGNMENT_CONFIRMATION_REQUIRED = "(Available with user consent flow active) Specifies if the confirmation by the device is required for this action.";
    public static final String ASSIGNMENT_TYPE = "The type of the assignment.";
    public static final String TARGET_ASSIGNED = "The number of targets that have been assigned as part of this operation.";
    public static final String TARGET_ASSIGNED_ALREADY = "The number of targets which already had been the assignment.";
    public static final String TARGET_ASSIGNED_TOTAL = "The total number of targets that are part of this operation.";

    public static final String ASSIGNED_TARGETS = "Assigned targets.";
    public static final String UN_ASSIGNED_TARGETS = "Unassigned targets.";
    public static final String LINKS_ASSIGNED_TARGETS = "Links to assigned targets.";

    public static final String ASSIGNED_DISTRIBUTION_SETS = "Assigned distribution sets.";
    public static final String UN_ASSIGNED_DISTRIBUTION_SETS = "Unassigned distribution sets.";
    public static final String LINKS_ASSIGNED_DISTRIBUTION_SETS = "Links to assigned distribution sets.";
    public static final Object OFFLINE_UPDATE = "Offline update (set param to true) that is only reported but not managed by the service, "
            + "e.g. defaults set in factory, manual updates or migrations from other update systems. A completed action is added to the history of the target(s)."
            + " Target is set to IN_SYNC state as both assigend and installed DS are set. "
            + "Note: only executed if the target has currently no running update.";

    // configuration
    public static final String CONFIG_LIST = "List of all available configuration parameter.";
    public static final String CONFIG_VALUE = "Current value of of configuration parameter.";
    public static final String CONFIG_GLOBAL = "true - if the current value is the global configuration value, false - if there is a tenant specific value configured.";
    public static final String CONFIG_PARAM = "The name of the configuration parameter.";

    public static final String DS_NEW_ASSIGNED_ACTIONS = "The newly created actions as a result of this assignment";

    public static final String REPRESENTATION_MODE = "The representation mode. Can be \"full\" or \"compact\". Defaults to \"compact\"";

}
