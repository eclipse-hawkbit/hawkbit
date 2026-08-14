/**
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ui.view;

// java:S1214 - implementations of Constants interface extends other classes, so if make this class we shall go for static imports
// which is not not better
@SuppressWarnings("java:S1214")
public interface Constants {

    // properties
    String ID = "Id";
    String ADDRESS = "Address";
    String NAME = "Name";
    String DESCRIPTION = "Description";
    String VERSION = "Version";
    String VENDOR = "Vendor";
    String TYPE = "Type";
    String GROUP = "Group";
    String CREATED_BY = "Created by";
    String CREATED_AT = "Created at";
    String LAST_MODIFIED_BY = "Last modified by";
    String LAST_MODIFIED_AT = "Last modified at";
    String LAST_POLL = "Last Poll";
    String SECURITY_TOKEN = "Security Token";
    String ATTRIBUTES = "Attributes";
    String COMPLETE = "Complete";
    String LOCKED = "Locked";
    String DELETED = "Deleted";
    String ENCRYPTED = "Encrypted";
    String VALID = "Valid";
    String REQUIRED_MIGRATION_STEP = "Required Migration Step";
    String WEIGHT = "Weight";

    // target
    String UPDATE_STATUS = "Update Status";
    String LAST_CONTROLLER_REQUEST_AT = "Last Controller Request At";
    String INSTALLED_AT = "Installed At";
    String IP_ADDRESS = "IP Address";
    String TARGET_TYPE = "Target Type";
    String REQUEST_ATTRIBUTES = "Request Attributes";
    String AUTO_CONFIRM_ACTIVE = "Auto Confirm Active";
    String NEXT_EXPECTED_POLL = "Next Expected Poll";
    String OVERDUE = "Overdue";

    // rollout
    String GROUPS = "Groups";
    String GROUP_COUNT = "Group Count";
    String TARGET_COUNT = "Target Count";
    String STATS = "Stats";
    String STATUS = "Status";
    String ACTIONS = "Actions";

    String TOTAL_TARGETS = "Total Targets";
    String TOTAL_GROUPS = "Total Groups";
    String FORCE_TIME = "Force Time";
    String APPROVAL_REMARK = "Approval Remark";
    String APPROVAL_DECIDED_BY = "Approval Decided By";
    String CONFIRMATION_REQUIRED = "Confirmation Required";

    // create rollout
    String TARGET_FILTER = "Target Filter";
    String DISTRIBUTION_SET = "Distribution Set";
    String ACTION_TYPE = "Action Type";
    String START_AT = "Start At";
    String SOFT = "Soft";
    String FORCED = "Forced";
    String DOWNLOAD_ONLY = "Download Only";
    String START_TYPE = "Start Type";
    String MANUAL = "Manual";
    String AUTO = "Auto";
    String DYNAMIC = "Dynamic";

    // dialog
    String CANCEL = "Cancel";
    String CANCEL_ESC = "Cancel (Esc)";

    String CREATED_AT_DESC = "createdAt:desc";

    String NAME_ASC = "name:asc";
    String NAME_DESC = "name:desc";

    String NOT_AVAILABLE_NULL = "n/a (null)";
}