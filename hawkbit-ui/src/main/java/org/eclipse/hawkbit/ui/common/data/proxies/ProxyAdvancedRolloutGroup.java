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

import org.eclipse.hawkbit.ui.common.data.aware.TargetFilterQueryAware;

/**
 * Proxy for advanced rollout group row.
 */
public class ProxyAdvancedRolloutGroup implements Serializable, TargetFilterQueryAware {

    private static final long serialVersionUID = 1L;

    private String groupName;
    private String targetFilterQuery;
    private Long targetFilterId;
    private Float targetPercentage;
    private String triggerThresholdPercentage;
    private String errorThresholdPercentage;
    private Long targetsCount;

    /**
     * Gets the name of the group
     *
     * @return groupName
     */
    public String getGroupName() {
        return groupName;
    }

    /**
     * Sets the groupName
     *
     * @param groupName
     *          name of the group
     */
    public void setGroupName(final String groupName) {
        this.groupName = groupName;
    }

    @Override
    public String getTargetFilterQuery() {
        return targetFilterQuery;
    }

    @Override
    public void setTargetFilterQuery(final String targetFilterQuery) {
        this.targetFilterQuery = targetFilterQuery;
    }

    @Override
    public Long getTargetFilterId() {
        return targetFilterId;
    }

    @Override
    public void setTargetFilterId(final Long targetFilterId) {
        this.targetFilterId = targetFilterId;
    }

    /**
     * Gets the percentage of the target
     *
     * @return targetPercentage
     */
    public Float getTargetPercentage() {
        return targetPercentage;
    }

    /**
     * Sets the targetPercentage
     *
     * @param targetPercentage
     *          percentage of the target
     */
    public void setTargetPercentage(final Float targetPercentage) {
        this.targetPercentage = targetPercentage;
    }

    /**
     * Gets the percentage of the TriggerThreshold
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
     *          percentage of the triggerThreshold
     */
    public void setTriggerThresholdPercentage(final String triggerThresholdPercentage) {
        this.triggerThresholdPercentage = triggerThresholdPercentage;
    }

    /**
     * Gets the percentage of the errorThreshold
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
     *          percentage of the errorThreshold
     */
    public void setErrorThresholdPercentage(final String errorThresholdPercentage) {
        this.errorThresholdPercentage = errorThresholdPercentage;
    }

    public Long getTargetsCount() {
        return targetsCount;
    }

    public void setTargetsCount(final Long targetsCount) {
        this.targetsCount = targetsCount;
    }
}
