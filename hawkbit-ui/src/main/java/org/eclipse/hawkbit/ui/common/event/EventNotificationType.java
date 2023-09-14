/**
 * Copyright (c) 2020 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ui.common.event;

/**
 * Types of event notification that can be displayed in the UI
 */
public enum EventNotificationType {
    // Target
    TARGET_CREATED("event.notifcation.target.created", "event.notifcation.targets.created"), TARGET_DELETED(
            "event.notifcation.target.deleted", "event.notifcation.targets.deleted"),
    // Distribution set
    DISTRIBUTIONSET_CREATED("event.notifcation.distributionset.created",
            "event.notifcation.distributionsets.created"), DISTRIBUTIONSET_DELETED(
                    "event.notifcation.distributionset.deleted", "event.notifcation.distributionsets.deleted"),
    // Software module
    SOFTWAREMODULE_CREATED("event.notifcation.softwaremodule.created",
            "event.notifcation.softwaremodules.created"), SOFTWAREMODULE_DELETED(
                    "event.notifcation.softwaremodule.deleted", "event.notifcation.softwaremodules.deleted");

    private final String notificationMessageKeySing;
    private final String notificationMessageKeyPlur;

    EventNotificationType(final String notificationMessageKeySing, final String notificationMessageKeyPlur) {
        this.notificationMessageKeySing = notificationMessageKeySing;
        this.notificationMessageKeyPlur = notificationMessageKeyPlur;
    }

    public String getNotificationMessageKeySing() {
        return notificationMessageKeySing;
    }

    public String getNotificationMessageKeyPlur() {
        return notificationMessageKeyPlur;
    }

}
