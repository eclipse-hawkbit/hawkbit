/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.utils;

import com.vaadin.ui.themes.ValoTheme;

/**
 * RepositoryConstants required for Label.
 * 
 *
 * 
 *
 *
 */
public final class SPUILabelDefinitions {

    /**
     * Style - Message.
     */
    public static final String SP_LABEL_MESSAGE_STYLE = "count-msg-box";
    /**
     * Message hint text style.
     */
    public static final String SP_MESSAGE_HINT_TEXT = "message-hint-text";
    /**
     * Style - Success.
     */
    public static final String SP_NOTIFICATION_SUCCESS_MESSAGE_STYLE = ValoTheme.NOTIFICATION_SUCCESS + " "
            + ValoTheme.NOTIFICATION_TRAY;
    /**
     * Style - Error.
     */
    public static final String SP_NOTIFICATION_ERROR_MESSAGE_STYLE = ValoTheme.NOTIFICATION_ERROR + " "
            + ValoTheme.NOTIFICATION_TRAY;

    /**
     * Style - Warning.
     */
    public static final String SP_NOTIFICATION_WARNING_MESSAGE_STYLE = ValoTheme.NOTIFICATION_WARNING + " "
            + ValoTheme.NOTIFICATION_TRAY;
    /**
     * Delay - Notification.
     */
    public static final int SP_DELAY = 1000;
    /**
     * Cannot be deleted message.
     */
    public static final String CANNOT_BE_DELETED_MESSAGE = "Cannot be deleted";

    /**
     * Only one distribution set can be assigned message.
     */
    public static final String ONLY_ONE_DS_CAN_BE_ASSIGNED = "Only one distribution set can be assigned";

    /**
     * Only one distribution set can be dropped.
     */
    public static final String ONLY_ONE_DS_CAN_BE_DROPPED = "Only one distribution set can be dropped";

    /**
     * Cannot be deleted message.
     */
    public static final String ACTION_NOT_ALLOWED = "Action not allowed";
    /**
     * Target already assigned or installed.
     */
    public static final String TARGET_ALREADY_ASSIGNED = "Some target(s) are already assigned or installed.Will be ignored";

    /**
     * Target already assigned or installed.
     */
    public static final String TARGET_ALREADY_ASSIGNED_PENDING_ACTION = "Some target(s) are already assigned.Pending for action";

    /**
     * Duplicate distribution delete message.
     */
    public static final String DUPLICATE_DISTRIBUTION_DELETE = "Distribution(s) already deleted.Pending for action";

    /**
     * Few distribution already deleted message.
     */
    public static final String FEW_DISTRIBUTION_ALREADY_DELETED = "Few distribution(s) are already deleted.Pending for action";

    /**
     * NAME.
     */
    public static final String VAR_NAME = "name";
    /**
     * NAME and VERSION.
     */
    public static final String VAR_NAME_VERSION = "nameVersion";
    /**
     * Color.
     */
    public static final String VAR_COLOR = "colour";
    /**
     * Assigned.
     */
    public static final String VAR_ASSIGNED = "assigned";
    /**
     * Softwarew type.
     */
    public static final String VAR_SOFT_TYPE = "type";
    /**
     * Softwarew type ID.
     */
    public static final String VAR_SOFT_TYPE_ID = "typeId";
    /**
     * CreatedAt.
     */
    public static final String VAR_CREATED_AT = "createdAt";
    /**
     * CONT ID.
     */
    public static final String VAR_CONT_ID = "controllerId";
    /**
     * CONT ID AND NAME = ItemId.
     */
    public static final String VAR_CONT_ID_NAME = "targetIdName";
    /**
     * Distribution set ID and Name= ItemId.
     */
    public static final String VAR_DIST_ID_NAME = "distributionSetIdName";

    /**
     * Distribution set ID.
     */
    public static final String DIST_ID = "distId";

    /**
     * ID.
     */
    public static final String VAR_ID = "id";

    /**
     * Key.
     */
    public static final String VAR_KEY = "key";
    /**
     * DESC.
     */
    public static final String VAR_DESC = "description";

    /**
     * VERSION.
     */
    public static final String VAR_VERSION = "version";

    /**
     * VERSION.
     */
    public static final String PIN_COLUMN = "pin";

    /**
     * VENDOR.
     */
    public static final String VAR_VENDOR = "vendor";
    /**
     * Created By.
     */
    public static final String VAR_CREATED_BY = "createdByUser";
    /**
     * Created date.
     */
    public static final String VAR_CREATED_DATE = "createdDate";
    /**
     * Last modified by.
     */
    public static final String VAR_LAST_MODIFIED_BY = "modifiedByUser";
    /**
     * Last modified by.
     */
    public static final String VAR_MODIFIED_BY = "lastModifiedBy";

    /**
     * Last modified date.
     */
    public static final String VAR_LAST_MODIFIED_DATE = "lastModifiedDate";

    /**
     * Last modified date.
     */
    public static final String VAR_MODIFIED_DATE = "modifiedDate";
    /**
     * Poll Status.
     */
    public static final String VAR_POLL_STATUS_TOOL_TIP = "pollStatusToolTip";
    /**
     * Is distribution complete.
     */
    public static final String VAR_IS_DISTRIBUTION_COMPLETE = "isComplete";
    /**
     * ASSIGNED DISTRIBUTION ID.
     */
    public static final String ASSIGNED_DISTRIBUTION_ID = "assignedDistributionSet.id";
    /**
     * AUTO ASSIGN DISTRIBUTION SET ID
     */
    public static final String AUTO_ASSIGN_DISTRIBUTION_SET = "autoAssignDistributionSet";
    /**
     * ASSIGNED DISTRIBUTION Name & Version.
     */
    public static final String ASSIGNED_DISTRIBUTION_NAME_VER = "assignedDistNameVersion";

    /**
     * INSTALLED DISTRIBUTION ID.
     */
    public static final String INSTALLED_DISTRIBUTION_ID = "installedDistributionSet.id";

    /**
     * INSTALLED DISTRIBUTION Name & Version.
     */
    public static final String INSTALLED_DISTRIBUTION_NAME_VER = "installedDistNameVersion";

    /**
     * Name description label.
     */
    public static final String NAME_DESCRIPTION_LABEL = "nameDescLabel";
    /**
     * Name description label.
     */
    public static final String LAST_QUERY_DATE = "lastTargetQuery";
    /**
     * Target status.
     */
    public static final String VAR_TARGET_STATUS = "updateStatus";

    /**
     * Duplicate target delete message.
     */
    public static final String DUPLICATE_TARGET_DELETE = "Target(s) already deleted.Pending for action";

    /**
     * Few target already deleted message.
     */
    public static final String FEW_TARGET_ALREADY_DELETED = "Few Target(s) are already deleted.Pending for action";

    /**
     * Discard all label.
     */
    public static final String DISCARD_ALL = "Discard All";
    /**
     * Delete all label.
     */
    public static final String DELETE_ALL = "Delete All";
    /**
     * Discard label.
     */
    public static final String DISCARD = "Discard";
    /**
     * Delete Software label.
     */
    public static final String DELETE_SOFTWARE_ARTIFACT = "Delete Sofware";

    /**
     * Delete Custom Filter.
     */
    public static final String DELETE_CUSTOM_FILTER = "Delete Custom Filter";

    /**
     * Update Custom Filter.
     */
    public static final String UPDATE_CUSTOM_FILTER = "Update Custom Filter";

    /**
     * Yes label.
     */
    public static final String YES = "Yes";

    /**
     * No label.
     */
    public static final String NO = "No";

    /**
     * JVM label.
     */
    public static final String JVM_LABEL = "Runtime";

    /**
     * Agent hub label.
     */
    public static final String AGENT_HUB_LABEL = "Application";

    /**
     * OS label.
     */
    public static final String OS_LABEL = "OS";

    /**
     * Missing mandatory details message.
     */
    public static final String MISSING_MANDATORY_MSG = "Mandatory details are missing";

    /**
     * Missing Jvm.
     */
    public static final String MISSING_JVM = "Please select Runtime";
    /**
     * Missing Agent hub.
     */
    public static final String MISSING_AH = "Please select Application";
    /**
     * Missing Os.
     */
    public static final String MISSING_OS = "Please select OS";

    /**
     * Missing tag name.
     */
    public static final String MISSING_TAG_NAME = "Please enter the Tag name";

    /**
     * Missing tag name.
     */
    public static final String MISSING_TYPE_NAME_KEY = "Please enter the Type name and Key";

    /**
     * HTML space.
     */
    public static final String HTML_SPACE = "&nbsp;";

    /**
     * No data available.
     */
    public static final String NO_DATA_AVAIALABLE = "No data available";
    /**
     * Type.
     */
    public static final String TYPE = "Filter by type";

    /**
     * Dist Type.
     */
    public static final String DISTTYPE = "Filter by Disttype";

    /**
     * Name.
     */
    public static final String NAME = "name";

    /**
     * Upload - process button caption.
     */
    public static final String PROCESS = "Process";
    /**
     * Upload - no action button caption.
     */
    public static final String NO_ACTION = "No Action";

    /**
     * Files selected for upload.
     */
    public static final String UPLOAD_FILES_DROPPED = " files are selected for upload";

    /**
     * Single file selected for upload.
     */
    public static final String UPLOAD_FILE_DROPPED = " file is selected for upload";

    /**
     * Upload - upload button caption.
     */
    public static final String UPLOAD = "Upload";

    /**
     * Upload - upload button caption.
     */
    public static final String CANCEL = "Cancel";

    /**
     * No file selected for upload.
     */
    public static final String NO_FILE_SELECTED = "No file selecetd for upload";

    /**
     * Upload results - label.
     */
    public static final String UPLOAD_RESULT = "Upload result";

    /**
     * Upload - close button caption.
     */
    public static final String CLOSE = "Close";

    /**
     * Success.
     */
    public static final String SUCCESS = "Success";

    /**
     * Failed.
     */
    public static final String FAILED = "Failed";

    /**
     * Name and description label.
     */
    public static final String NAME_VERSION = "nameAndVersion";

    /**
     * Submit button label.
     */
    public static final String SUBMIT = "Submit";

    /**
     * Artifact Details Icon.
     */
    public static final String ARTIFACT_ICON = "artifactDtls";

    /**
     * Confirmation popup - Delete all button caption.
     */
    public static final String BUTTON_DELETE_ALL = "button.delete.all";

    /**
     * Confirmation popup - Discard all button caption.
     */
    public static final String BUTTON_DISCARD_ALL = "button.discard.all";

    /**
     * Created User.
     */
    public static final String VAR_CREATED_USER = "createdBy";

    /**
     * Create Filter.
     */
    public static final String VAR_CREATE_FILTER = "Create Filter";

    /**
     * Target filter query - target table status icon column name.
     */
    public static final String STATUS_ICON = "statusIcon";

    /**
     * Create custom target filter header - query text field length.
     */
    public static final int TARGET_FILTER_QUERY_TEXT_FIELD_LENGTH = 1024;

    /**
     * Status - column property.
     */
    public static final String VAR_STATUS = "status";

    /**
     * Target filter query - column property.
     */
    public static final String VAR_TARGETFILTERQUERY = "targetFilterQuery";

    /**
     * Distribution name and version - column property.
     */
    public static final String VAR_DIST_NAME_VERSION = "distributionSetNameVersion";

    /**
     * Number of groups in rollout- column property.
     */
    public static final String VAR_NUMBER_OF_GROUPS = "numberOfGroups";

    /**
     * Delete label.
     */
    public static final String DELETE = "Delete";
    /**
     * Rollout name link's description.
     */
    public static final String SHOW_ROLLOUT_GROUP_DETAILS = "show group details";
    /**
     * Rollout action button description.
     */
    public static final String ACTION = "Action";
    /**
     * Rollout pause button name.
     */
    public static final String PAUSE = "Pause";

    /**
     * Rollout resume button name.
     */
    public static final String RESUME = "Resume";
    /**
     * Rollout and rollout group property - count of not started targets.
     */
    public static final String VAR_COUNT_TARGETS_NOT_STARTED = "notStartedTargetsCount";

    /**
     * Rollout and rollout group property - count of running targets.
     */
    public static final String VAR_COUNT_TARGETS_RUNNING = "runningTargetsCount";

    /**
     * Rollout and rollout group property - count of scheduled targets.
     */
    public static final String VAR_COUNT_TARGETS_SCHEDULED = "scheduledTargetsCount";

    /**
     * Rollout and rollout group property - count of targets in error.
     */
    public static final String VAR_COUNT_TARGETS_ERROR = "errorTargetsCount";

    /**
     * Rollout and rollout group property - count of finished targets.
     */
    public static final String VAR_COUNT_TARGETS_FINISHED = "finishedTargetsCount";

    /**
     * Rollout and rollout group property - count of targets cancelled targets.
     */
    public static final String VAR_COUNT_TARGETS_CANCELLED = "cancelledTargetsCount";

    /**
     * Total target coulmn property name.
     */
    public static final String VAR_TOTAL_TARGETS = "totalTargetsCount";

    /**
     * Total target count status coulmn property name.
     */
    public static final String VAR_TOTAL_TARGETS_COUNT_STATUS = "totalTargetCountStatus";

    /**
     * Rollout group started date column property.
     */
    public static final String ROLLOUT_GROUP_ERROR_THRESHOLD = "errorConditionExp";

    /**
     * Rollout group started date column property.
     */
    public static final String ROLLOUT_GROUP_THRESHOLD = "successConditionExp";

    /**
     * Rollout group installed percentage column property.
     */
    public static final String ROLLOUT_GROUP_INSTALLED_PERCENTAGE = "finishedPercentage";

    /**
     * Add metadata icon.
     */
    public static final String METADATA_ICON = "metadataDls";

    /**
     * Constructor.
     */
    private SPUILabelDefinitions() {

    }
}
