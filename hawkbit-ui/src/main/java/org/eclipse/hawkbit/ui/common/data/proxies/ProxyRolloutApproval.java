/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.data.proxies;

import java.io.Serializable;

import org.eclipse.hawkbit.repository.model.Rollout.ApprovalDecision;

/**
 * Proxy for rollout approval definition.
 */
public class ProxyRolloutApproval implements Serializable {

    private static final long serialVersionUID = 1L;

    private String approvalRemark;
    private ApprovalDecision approvalDecision;

    /**
     * Gets the approvalRemark
     *
     * @return approvalRemark
     */
    public String getApprovalRemark() {
        return approvalRemark;
    }

    /**
     * Sets the approvalRemark
     *
     * @param approvalRemark
     *          Remark for approval
     */
    public void setApprovalRemark(final String approvalRemark) {
        this.approvalRemark = approvalRemark;
    }

    /**
     * Gets the approvalDecision
     *
     * @return approvalDecision
     */
    public ApprovalDecision getApprovalDecision() {
        return approvalDecision;
    }

    /**
     * Sets the approvalDecision
     *
     * @param approvalDecision
     *           Approve or deny Decision
     */
    public void setApprovalDecision(final ApprovalDecision approvalDecision) {
        this.approvalDecision = approvalDecision;
    }
}
