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

import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.ActionStatus;

/**
 * Proxy for {@link ActionStatus}
 */
public class ProxyActionStatus extends ProxyIdentifiableEntity {
    private static final long serialVersionUID = 1L;

    public static final String PXY_AS_STATUS = "status";
    public static final String PXY_AS_CREATED_AT = "createdAt";
    public static final String PXY_AS_ID = "id";

    private Action.Status status;
    private Long createdAt;

    /**
     * Gets the status literal.
     *
     * @return status literal
     */
    public Action.Status getStatus() {
        return status;
    }

    /**
     * Sets the status literal.
     *
     * @param status
     *            literal
     */
    public void setStatus(final Action.Status status) {
        this.status = status;
    }

    /**
     * Get raw long-value for createdAt-date.
     *
     * @return raw long-value for createdAt-date
     */
    public Long getCreatedAt() {
        return createdAt;
    }

    /**
     * Set raw long-value for createdAt-date.
     *
     * @param createdAt
     *            raw long-value for createdAt-date
     */
    public void setCreatedAt(final Long createdAt) {
        this.createdAt = createdAt;
    }
}
