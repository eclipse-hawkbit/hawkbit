/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.data.proxies;

import java.net.URI;

import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;

/**
 * Proxy for {@link Target}.
 */
public class ProxyTarget extends ProxyNamedEntity {

    private static final long serialVersionUID = 1L;

    private String controllerId;

    private URI address;

    private Long lastTargetQuery;

    private Long installationDate;

    private TargetUpdateStatus updateStatus = TargetUpdateStatus.UNKNOWN;

    private String pollStatusToolTip;

    private Status status;

    private String securityToken;

    private boolean isRequestAttributes;

    /**
     * Gets the controllerId
     *
     * @return controllerId
     */
    public String getControllerId() {
        return controllerId;
    }

    /**
     * Sets the controllerId
     *
     * @param controllerId
     *         Target controllerId
     */
    public void setControllerId(final String controllerId) {
        this.controllerId = controllerId;
    }

    /**
     * Gets the address under which the target can be reached
     *
     * @return address
     */
    public URI getAddress() {
        return address;
    }

    /**
     * Sets the address
     *
     * @param address
     *         Address under which the target can be reached
     */
    public void setAddress(final URI address) {
        this.address = address;
    }

    /**
     * Gets the time in milliseconds when the target is last polled or queried
     *
     * @return address
     */
    public Long getLastTargetQuery() {
        return lastTargetQuery;
    }

    /**
     * Sets the lastTargetQuery time
     *
     * @param lastTargetQuery
     *         Time in milliseconds when the target is last polled or queried
     */
    public void setLastTargetQuery(final Long lastTargetQuery) {
        this.lastTargetQuery = lastTargetQuery;
    }

    public Long getInstallationDate() {
        return installationDate;
    }

    /**
     * Sets the installationDate
     *
     * @param installationDate
     *         Target installationDate
     */
    public void setInstallationDate(final Long installationDate) {
        this.installationDate = installationDate;
    }

    /**
     * Gets the updateStatus
     *
     * @return updateStatus
     */
    public TargetUpdateStatus getUpdateStatus() {
        return updateStatus;
    }

    /**
     * Sets the updateStatus
     *
     * @param updateStatus
     *         Target updateStatus
     */
    public void setUpdateStatus(final TargetUpdateStatus updateStatus) {
        this.updateStatus = updateStatus;
    }

    /**
     * Gets the pollStatusToolTip
     *
     * @return pollStatusToolTip
     */
    public String getPollStatusToolTip() {
        return pollStatusToolTip;
    }

    /**
     * Sets the pollStatusToolTip
     *
     * @param pollStatusToolTip
     *         Target pollStatusToolTip
     */
    public void setPollStatusToolTip(final String pollStatusToolTip) {
        this.pollStatusToolTip = pollStatusToolTip;
    }

    /**
     * Gets the action status as reported by the controller
     *
     * @return status
     */
    public Status getStatus() {
        return status;
    }

    /**
     * Sets the status
     *
     * @param status
     *         Action status as reported by the controller
     */
    public void setStatus(final Status status) {
        this.status = status;
    }

    /**
     * Gets the securityToken
     *
     * @return securityToken
     */
    public String getSecurityToken() {
        return securityToken;
    }

    /**
     * Sets the securityToken
     *
     * @param securityToken
     *         Target securityToken
     */
    public void setSecurityToken(final String securityToken) {
        this.securityToken = securityToken;
    }

    /**
     * Flag that indicates if the target has requestAttributes.
     *
     * @return <code>true</code> if the target has requestAttributes, otherwise
     *         <code>false</code>
     */
    public boolean isRequestAttributes() {
        return isRequestAttributes;
    }

    /**
     * Sets the flag that indicates if the target has requestAttributes.
     *
     * @param isRequestAttributes
     *            <code>true</code> if the target has requestAttributes, otherwise
     *            <code>false</code>
     */
    public void setRequestAttributes(final boolean isRequestAttributes) {
        this.isRequestAttributes = isRequestAttributes;
    }
}
