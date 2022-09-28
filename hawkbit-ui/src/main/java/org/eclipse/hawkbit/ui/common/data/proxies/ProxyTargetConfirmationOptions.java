/**
 * Copyright (c) 2022 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.data.proxies;

import org.eclipse.hawkbit.repository.model.AutoConfirmationStatus;

/**
 * Proxy for target confirmation options.
 */
public class ProxyTargetConfirmationOptions {

    private boolean isAutoConfirmationEnabled;
    private final String controllerId;
    private final String initiatedSystemUser;
    private final Long activatedAt;
    private String initiator;
    private String remark;

    /**
     * dummy constructor needed for ProxyTargetConfirmationOptions getValue
     */
    public ProxyTargetConfirmationOptions() {
        this.controllerId = null;
        this.initiatedSystemUser = null;
        this.activatedAt = null;
    }

    /**
     * Constructor for ProxyTargetAttributesDetails
     *
     * @param controllerId
     *            Target attribute controller id
     * @param isAutoConfirmationEnabled
     *            flag if enabled
     * @param initiator
     *            who initiated the auto confirmation
     * @param remark
     *            optional remark
     */
    private ProxyTargetConfirmationOptions(final boolean isAutoConfirmationEnabled, final String controllerId,
                                           final String initiator, final String initiatedSystemUser, final Long activatedAt, final String remark) {
        this.isAutoConfirmationEnabled = isAutoConfirmationEnabled;
        this.controllerId = controllerId;
        this.initiator = initiator;
        this.initiatedSystemUser = initiatedSystemUser;
        this.activatedAt = activatedAt;
        this.remark = remark;
    }

    public static ProxyTargetConfirmationOptions disabled(final String controllerId) {
        return new ProxyTargetConfirmationOptions(false, controllerId, null, null, null, null);
    }

    public static ProxyTargetConfirmationOptions active(final AutoConfirmationStatus status) {
        return new ProxyTargetConfirmationOptions(true, status.getTarget().getControllerId(), status.getInitiator(),
                status.getCreatedBy(), status.getActivatedAt(), status.getRemark());
    }

    /**
     * @return target attributes detail controllerId
     */
    public String getControllerId() {
        return controllerId;
    }

    public boolean isAutoConfirmationEnabled() {
        return isAutoConfirmationEnabled;
    }

    public String getInitiator() {
        return initiator;
    }

    public void setInitiator(final String initiator) {
        this.initiator = initiator;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(final String remark) {
        this.remark = remark;
    }

    public Long getActivatedAt() {
        return activatedAt;
    }

    public String getInitiatedSystemUser() {
        return initiatedSystemUser;
    }

}
