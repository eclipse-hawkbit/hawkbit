/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ui.common.data.proxies;

import java.security.SecureRandom;

import org.eclipse.hawkbit.repository.model.ActionStatus;

/**
 * Proxy for an entry of {@link ActionStatus} message.
 */
public class ProxyMessage extends ProxyIdentifiableEntity {
    private static final long serialVersionUID = 1L;

    private String message;

    /**
     * Constructor for ProxyMessage
     */
    public ProxyMessage() {
        super(new SecureRandom().nextLong());
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
    public void setMessage(final String message) {
        this.message = message;
    }
}
