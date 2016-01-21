/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.rest.resource.model.doc;

/**
 * Constants for API documentation.
 *
 *
 *
 *
 */
public final class ApiModelProperties {
    private static final String ENDING = "  of the entity";

    // generic
    public static final String ITEM_ID = "the id" + ENDING;

    public static final String NAME = "the name" + ENDING;
    public static final String DESCRPTION = "the description" + ENDING;

    public static final String CREATED_BY = "entity was originally created by (IM UUID)";

    public static final String CREATED_AT = "entity was originally created at";
    public static final String LAST_MODIFIED_BY = "entity was last modified by (IM UUID)";
    public static final String LAST_MODIFIED_AT = "entity was last modified at";
    public static final String SIZE = "current page size";
    public static final String TOTAL_ELEMENTS = "total number of elements";

    // Versioned entity
    public static final String VERSION = "software version";

    // software module
    public static final String SOFTWARE_MODULE_TYPE = "the software module type " + ENDING;

    public static final String VENDOR = "the software vendor " + ENDING;
    public static final String ARTIFACT_HASHES = "hashes of the artifact";
    public static final String ARTIFACT_SIZE = "size of the artifact";
    public static final String ARTIFACT_PROVIDED_FILENAME = "filename of the artifact";
    public static final String ARTIFACT_HASHES_SHA1 = "SHA1 hash of the artifact";
    public static final String ARTIFACT_HASHES_MD5 = "MD5 hash of the artifact";
    public static final String DS_OS = "operating system or firmware software module - DEPRECATED (use modules)";
    public static final String DS_RUNTIME = "runtime software module (e.g. JVM) - DEPRECATED (use modules)";
    public static final String DS_APPLICATION = "application software module (e.g. OSGi container) - DEPRECATED (use modules)";
    public static final String DS_MODULES = "software modules (e.g. OSGi bundles, runtimes)";
    public static final String DS_REQUIRED_STEP = "is a required migration step";

    // Target
    public static final String INSTALLED_AT = "installation time of current installed DistributionSet";

    public static final String LAST_REQUEST_AT = "last time where the target polled the server, same as pollStatus.lastRequestAt";
    // poll status
    public static final String POLL_LAST_REQUEST_AT = "last time where the target polled the server";
    public static final String POLL_NEXT_EXPECTED_REQUEST_AT = "next expected time where the target polls the server";
    public static final String POLL_OVERDUE = "defines if the target poll time is overdued based on the next expected poll time plus the configured overdue poll time threshold";

    public static final String UPDATE_STATUS = "current update status";
    public static final String TARGET_ATTRIBUTES = "target attributes";

    public static final String TARGET_LIST = "list of targets";

    public static final String SM_LIST = "list of software modules";

    public static final String ACTION_TYPE = "type of action";

    public static final String ACTION_STATUS_TYPE = "type of the action status";

    public static final String ACTION_STATUS_MESSAGES = "messages related to the status";

    public static final String ACTION_STATUS_REPORTED_AT = "time at which the status was reported (server time)";

    public static final String ACTION_STATUS_LIST = "list of action status";

    public static final String ACTION_EXECUTION_STATUS = "status of action";

    public static final String ACTION_LIST = "list if actions";

    public static final String POLL_STATUS = "status of the poll time of the target";

    public static final String IP_ADDRESS = "last known ip-address of the target";

    public static final String ADDRESS = "last known address of the target";

    public static final String SECURITY_TOKEN = "the security token of this target which can be used to authenticate the target if enabled";

    public static final String META_DATA = "Metadata";

    public static final String SM_TYPE_KEY = "key that can be interpreted by the target";

    public static final String SM_MAX_ASSIGNMENTS = "maximum number of assigments to a distribution set/target, e.g. only one firmware but multiple applications";

    public static final String SM_TYPE_LIST = "list of software modules types";

    // Direct Device Integration API
    public static final String TARGET_TIME = "time on the target device";

    public static final String TARGET_STATUS = "SP target action status";

    public static final String TARGET_EXEC_STATUS = "status of the action execution";

    public static final String TARGET_RESULT_VALUE = "result of the action execution";

    public static final String TARGET_RESULT_DETAILS = "List of details message information";

    public static final String TARGET_RESULT_FINISHED = "defined status of the result";

    public static final String TARGET_RESULT_PROGRESS = "progress assumption of the device";

    public static final String TARGET_PROGRESS_CNT = "current progress level";

    public static final String TARGET_PROGRESS_OF = "asumption concerning max progress level";

    public static final String ACTION_ID = "id of the action";

    public static final String CANCEL_ACTION = "action that needs to be canceled";

    public static final String CHUNK_TYPE = "type of the chunk, e.g. firmware, bundle, app";

    public static final String SOFTWARE_MODUL_TYPE = "type of the software module, e.g. firmware, bundle, app";

    public static final String SOFTWARE_MODULE_VERSION = "version of the software module";

    public static final String SOFTWARE_MODULE_NAME = "name of the software module";

    public static final String SOFTWARE_MODULE_ARTIFACT_LINKS = "artifact links of the software module";

    public static final String SOFTWARE_MODUL_ID = "id of the software module";

    public static final String CHUNK_VERSION = "software version of the chunk";

    public static final String CHUNK_NAME = "name of the chunk";

    public static final String ARTIFACTS = "list of artifacts";

    public static final String TARGET_CONFIGURATION = "target configuration setup by the server";

    public static final String TARGET_POLL_TIME = "suggested sleep time between polls";

    public static final String TARGET_SLEEP = "sleep time in HH:MM:SS notation";

    public static final String DEPLOYMENT = "detailed deployment operation";

    public static final String HANDLING_DOWNLOAD = "handling for the download part of the provisioning process";

    public static final String HANDLING_UPDATE = "handling for the update part of the provisioning process";

    public static final String CHUNK = "software chunk of an update";

    public static final String SOFTWARE_MODUL = "software moduls of an update";

    public static final String ARTIFACT = "artifact moduls of an update";

    public static final String TARGET_CONFIG_DATA = "configuration data as key/value list";

    public static final String DS_TYPE_KEY = "funtional key of the distribution set type";

    public static final String DS_TYPE_LIST = "list of distribution set types";

    public static final String DS_TYPE = "the type of the distribution set";

    public static final String DS_COMPLETE = "true of the distribution set software module setup is complete by means as defined by the distribution set type";

    public static final String DS_TYPE_MANDATORY_MODULES = "mandatory module type IDs";

    public static final String DS_TYPE_OPTIONAL_MODULES = "optional module type IDs";

    private ApiModelProperties() {
        // utility class
    }

}
