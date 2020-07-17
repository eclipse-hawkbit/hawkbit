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

/**
 * Proxy for simple rollout group definition.
 */
public class ProxySimpleRolloutGroupsDefinition implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer numberOfGroups;
    private String triggerThresholdPercentage;
    private String errorThresholdPercentage;

    /**
     * Gets the count of simple rollout Groups
     *
     * @return numberOfGroups
     */
    public Integer getNumberOfGroups() {
        return numberOfGroups;
    }

    /**
     * Sets the numberOfGroups
     *
     * @param numberOfGroups
     *           count of simple rollout Groups
     */
    public void setNumberOfGroups(final Integer numberOfGroups) {
        this.numberOfGroups = numberOfGroups;
    }

    /**
     * Gets the percentage of triggerThreshold
     *
     * @return triggerThresholdPercentage
     */
    public String getTriggerThresholdPercentage() {
        return triggerThresholdPercentage;
    }

    /**
     * Sets the triggerThresholdPercentage
     *
     * @param triggerThresholdPercentage
     *          percentage of triggerThreshold
     */
    public void setTriggerThresholdPercentage(final String triggerThresholdPercentage) {
        this.triggerThresholdPercentage = triggerThresholdPercentage;
    }

    /**
     * Gets the percentage of errorThreshold
     *
     * @return errorThresholdPercentage
     */
    public String getErrorThresholdPercentage() {
        return errorThresholdPercentage;
    }

    /**
     * Sets the errorThresholdPercentage
     *
     * @param errorThresholdPercentage
     *          percentage of errorThreshold
     */
    public void setErrorThresholdPercentage(final String errorThresholdPercentage) {
        this.errorThresholdPercentage = errorThresholdPercentage;
    }

}
