/**
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ui.simple.view;

// java:S1214 - implementations of Constants interface extends other classes, so if make this class we shall go for static imports
//              which is not not better
@SuppressWarnings("java:S1214")
public interface Constants {

    // properties
    String ID = "Id";
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

    // rollout
    String GROUP_COUNT = "Group Count";
    String TARGET_COUNT = "Target Count";
    String STATS = "Stats";
    String STATUS = "Status";
    String ACTIONS = "Actions";

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

    String NAME_ASC = "name:asc";

    String NOT_AVAILABLE_NULL = "n/a (null)";
}