/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.dmf.amqp.api;

/**
 * The event topics for the message headers.
 */
public enum EventTopic {

    /**
     * Topic when sending and receiving a update status.
     */
    UPDATE_ACTION_STATUS,

    /**
     * Topic when sending and receiving a download and install task.
     */
    DOWNLOAD_AND_INSTALL,

    /**
     * Topic when sending and receiving a cancel download task.
     */
    CANCEL_DOWNLOAD,

    /**
     * Topic when updating device attributes.
     */
    UPDATE_ATTRIBUTES,

    /**
     * Topic when updating auto-confirmation state.
     */
    UPDATE_AUTO_CONFIRM,

    /**
     * Topic when sending a download only task, skipping the install.
     */
    DOWNLOAD,

    /**
     * Topic when an update of device attributes is requested.
     */
    REQUEST_ATTRIBUTES_UPDATE,

    /**
     * Topic to send multiple actions to the device.
     */
    MULTI_ACTION,

    /**
     * Topic when sending a download only action to multiple devices.
     */
    BATCH_DOWNLOAD,

    /**
     * Topic when sending a download and install action to multiple devices.
     */
    BATCH_DOWNLOAD_AND_INSTALL,

    /**
     * Topic when confirmation of an action is requested.
     */
    CONFIRM
}
