/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.data.proxies;

import java.security.SecureRandom;

import org.eclipse.hawkbit.repository.model.ActionStatus;

/**
 * Proxy for an entry of {@link ActionStatus#getMessages()}
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
