/**
 * Copyright (c) 2018 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.rest.ddi.documentation;

/**
 * Model properties for the DDI API documentation.
 */
final class DdiApiModelProperties {

    // Direct Device Integration API
    static final String CONTROLLER_ID = "id of the controller";

    static final String TARGET_STATUS = "target action status";

    static final String TARGET_EXEC_STATUS = "status of the action execution";

    static final String TARGET_RESULT_VALUE = "result of the action execution";

    static final String TARGET_RESULT_DETAILS = "List of details message information";

    static final String TARGET_RESULT_FINISHED = "defined status of the result";

    static final String TARGET_RESULT_PROGRESS = "progress assumption of the device";

    static final String TARGET_PROGRESS_CNT = "current progress level";

    static final String TARGET_PROGRESS_OF = "assumption concerning max progress level";

    static final String ACTION_ID = "id of the action";

    static final String CANCEL_ACTION = "action that needs to be canceled";

    static final String ACTION_ID_CANCELED = "id of the action that needs to be canceled (typically identical to id field on the cancel action itself)";

    static final String ARTIFACT_HTTPS_HASHES_MD5SUM_LINK = "HTTPs Download resource for MD5SUM file is an optional auto generated artifact that is especially useful for "
            + "Linux based devices on order to check artifact consistency after download by using the md5sum "
            + "command line tool. The MD5 and SHA1 are in addition available as metadata in the deployment command itself.";

    static final String ARTIFACT_HTTP_HASHES_MD5SUM_LINK = "HTTP Download resource for MD5SUM file is an optional auto generated artifact that is especially useful for "
            + "Linux based devices on order to check artifact consistency after download by using the md5sum "
            + "command line tool. The MD5 and SHA1 are in addition available as metadata in the deployment command itself. "
            + "(note: anonymous download needs to be enabled on the service account for non-TLS access)";

    static final String ARTIFACT_HTTPS_DOWNLOAD_LINK_BY_CONTROLLER = "HTTPs Download resource for artifacts. The resource supports partial download "
            + "as specified by RFC7233 (range requests). Keep in mind that the target "
            + "needs to have the artifact assigned in order to be granted permission to download.";

    static final String ARTIFACT_HTTP_DOWNLOAD_LINK_BY_CONTROLLER = "HTTP Download resource for artifacts. The resource supports partial download "
            + "as specified by RFC7233 (range requests). Keep in mind that the target "
            + "needs to have the artifact assigned in order to be granted permission to download. "
            + "(note: anonymous download needs to be enabled on the service account for non-TLS access)";

    static final String CHUNK_TYPE = "Type of the chunk, e.g. firmware, bundle, app. In update server mapped to Software Module Type.";

    static final String SOFTWARE_MODUL_TYPE = "type of the software module, e.g. firmware, bundle, app";

    static final String SOFTWARE_MODULE_VERSION = "version of the software module";

    static final String SOFTWARE_MODULE_NAME = "name of the software module";

    static final String SOFTWARE_MODULE_ARTIFACT_LINKS = "artifact links of the software module";

    static final String SOFTWARE_MODUL_ID = "id of the software module";

    static final String CHUNK_VERSION = "software version of the chunk";

    static final String CHUNK_NAME = "name of the chunk";

    static final String CHUNK_META_DATA = "meta data of the respective software module that has been marked with 'target visible'";

    static final String CHUNK_META_DATA_KEY = "key of meta data entry";

    static final String CHUNK_META_DATA_VALUE = "value of meta data entry";

    static final String ARTIFACTS = "list of artifacts";

    static final String TARGET_CONFIGURATION = "target configuration setup by the server";

    static final String TARGET_POLL_TIME = "suggested sleep time between polls";

    static final String TARGET_OPEN_ACTIONS = "Open Actions that the server has for the target";

    static final String TARGET_SLEEP = "sleep time in HH:MM:SS notation";

    static final String DEPLOYMENT = "Detailed deployment operation";

    static final String CANCEL = "Detailed cancel operation of a deployment.";

    static final String HANDLING_DOWNLOAD = "handling for the download part of the provisioning process ('skip': do not download yet, 'attempt': server asks to download, 'forced': server requests immediate download)";

    static final String HANDLING_UPDATE = "handling for the update part of the provisioning process ('skip': do not update yet, 'attempt': server asks to update, 'forced': server requests immediate update)";

    static final String MAINTENANCE_WINDOW = "separation of download and installation by defining a maintenance window for the installation. Status shows if currently in a window.";

    static final String CHUNK = "Software chunks of an update. In server mapped by Software Module.";

    static final String SOFTWARE_MODUL = "software modules of an update";

    static final String ARTIFACT = "artifact modules of an update";

    static final String FILENAME = "file name of artifact";

    static final String TARGET_CONFIG_DATA = "Link which is provided whenever the provisioning target or device is supposed "
                    + "to push its configuration data (aka. \"controller attributes\") to the server. Only shown for the initial "
                    + "configuration, after a successful update action, or if requested explicitly (e.g. via the management UI).";

    static final String ARTIFACT_HASHES_SHA1 = "SHA1 hash of the artifact in Base 16 format";
    static final String ARTIFACT_HASHES_MD5 = "MD5 hash of the artifact";
    static final String ARTIFACT_HASHES_SHA256 = "SHA-256 hash of the artifact in Base 16 format";

    static final String ARTIFACT_SIZE = "size of the artifact";

    static final String ACTION_HISTORY = "Optional GET parameter to retrieve a given number of messages which are previously provided by the device. "
            + "Useful if the devices sent state information to the feedback channel and never stored them locally.";

    static final String ACTION_HISTORY_RESP = "Current deployment state.";

    static final String ACTION_HISTORY_RESP_STATUS = "Status of the deployment based on previous feedback by the device.";

    static final String ACTION_HISTORY_RESP_MESSAGES = "Messages are previously sent to the feedback channel in LIFO order by the device.";

    static final String UPDATE_MODE = "Optional parameter to specify the update mode that should be applied when updating target attributes. "
            + "Valid values are 'merge', 'replace', and 'remove'. Defaults to 'merge'.";

}
