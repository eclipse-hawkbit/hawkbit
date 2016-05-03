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
    private ResponseStatus responseStatus = ResponseStatus.SUCCESSFUL;
    private final List<String> statusMessages = new ArrayList<>();

    public UpdateStatus(final ResponseStatus responseStatus) {
        this.responseStatus = responseStatus;
    }

    public UpdateStatus(final ResponseStatus responseStatus, final String message) {
        this(responseStatus);
        statusMessages.add(message);
    }

    public ResponseStatus getResponseStatus() {
        return responseStatus;
    }

    public void setResponseStatus(final ResponseStatus responseStatus) {
        this.responseStatus = responseStatus;
    }

    public List<String> getStatusMessages() {
        return statusMessages;
    }

    /**
     * The status to response to the hawkBit update server if an simulated
     * update process should be respond with successful or failure update.
     */
    public enum ResponseStatus {
        /**
         * updated has been successful and response the successful update.
         */
        SUCCESSFUL,
        /**
         * updated has been not successful and response the error update.
         */
        ERROR;
    }

}