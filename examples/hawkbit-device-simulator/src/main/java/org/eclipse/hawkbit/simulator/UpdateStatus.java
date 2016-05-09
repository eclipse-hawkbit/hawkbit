/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.simulator;

import java.util.ArrayList;
import java.util.List;

/**
 * Update status of the simulated update.
 *
 */
public class UpdateStatus {
    private ResponseStatus responseStatus;
    private List<String> statusMessages;

    /**
     * Constructor.
     * 
     * @param responseStatus
     *            of the update
     */
    public UpdateStatus(final ResponseStatus responseStatus) {
        this.responseStatus = responseStatus;
    }

    /**
     * Constructor including status message.
     * 
     * @param responseStatus
     *            of the update
     * @param message
     *            of the update status
     */
    public UpdateStatus(final ResponseStatus responseStatus, final String message) {
        this(responseStatus);
        statusMessages = new ArrayList<>();
        statusMessages.add(message);
    }

    public ResponseStatus getResponseStatus() {
        return responseStatus;
    }

    public void setResponseStatus(final ResponseStatus responseStatus) {
        this.responseStatus = responseStatus;
    }

    public List<String> getStatusMessages() {
        if (statusMessages == null) {
            statusMessages = new ArrayList<>();
        }

        return statusMessages;
    }

    /**
     * The status to response to the hawkBit update server if an simulated
     * update process should be respond with successful or failure update.
     */
    public enum ResponseStatus {
        /**
         * Update has been successful and response the successful update.
         */
        SUCCESSFUL,

        /**
         * Update has been not successful and response the error update.
         */
        ERROR;
    }

}