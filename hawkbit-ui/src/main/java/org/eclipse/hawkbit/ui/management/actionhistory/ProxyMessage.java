/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.actionhistory;

import java.io.Serializable;

import org.eclipse.hawkbit.repository.model.ActionStatus;

/**
 * Proxy for an entry of {@link ActionStatus#getMessages()}
 */
public class ProxyMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String PXY_MSG_ID = "id";
    public static final String PXY_MSG_VALUE = "message";

    private String id;
    private String message;

    /**
     * Get id for the entry.
     *
     * @return id for the entry.
     */
    public String getId() {
        return id;
    }

    /**
     * Set the id for the entry.
     *
     * @param id
     *            of the message entry.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Get message value.
     *
     * @return message value
     */
    public String getMessage() {
        return message;
    }

    /**
     * Set message value.
     *
     * @param message
     *            value
     */
    public void setMessage(String message) {
        this.message = message;
    }
}
