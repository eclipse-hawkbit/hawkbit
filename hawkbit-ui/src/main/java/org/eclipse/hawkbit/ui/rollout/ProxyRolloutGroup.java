/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.rollout;

import org.eclipse.hawkbit.repository.model.RolloutGroup;

/**
 * Proxy rollout group with suctome properties.
 *
 */
public class ProxyRolloutGroup extends RolloutGroup {

    private static final long serialVersionUID = -2745056813306692356L;

    private String createdDate;

    private String modifiedDate;

    private String createdBy;

    private String lastModifiedBy;

    private String finishedPercentage;

    /**
     * @return the createdDate
     */
    public String getCreatedDate() {
        return createdDate;
    }

    /**
     * @param createdDate
     *            the createdDate to set
     */
    public void setCreatedDate(final String createdDate) {
        this.createdDate = createdDate;
    }

    /**
     * @return the modifiedDate
     */
    public String getModifiedDate() {
        return modifiedDate;
    }

    /**
     * @param modifiedDate
     *            the modifiedDate to set
     */
    public void setModifiedDate(final String modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    /**
     * @return the createdBy
     */
    public String getCreatedBy() {
        return createdBy;
    }

    /**
     * @param createdBy
     *            the createdBy to set
     */
    public void setCreatedBy(final String createdBy) {
        this.createdBy = createdBy;
    }

    /**
     * @return the lastModifiedBy
     */
    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    /**
     * @param lastModifiedBy
     *            the lastModifiedBy to set
     */
    public void setLastModifiedBy(final String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    /**
     * @return the finishedPercentage
     */
    public String getFinishedPercentage() {
        return finishedPercentage;
    }

    /**
     * @param finishedPercentage
     *            the finishedPercentage to set
     */
    public void setFinishedPercentage(final String finishedPercentage) {
        this.finishedPercentage = finishedPercentage;
    }

}
