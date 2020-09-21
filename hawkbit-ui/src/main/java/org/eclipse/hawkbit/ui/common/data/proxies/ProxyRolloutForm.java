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

import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.ui.common.data.aware.ActionTypeAware;
import org.eclipse.hawkbit.ui.common.data.aware.DescriptionAware;
import org.eclipse.hawkbit.ui.common.data.aware.DsIdAware;
import org.eclipse.hawkbit.ui.common.data.aware.NameAware;
import org.eclipse.hawkbit.ui.common.data.aware.StartOptionAware;
import org.eclipse.hawkbit.ui.common.data.aware.TargetFilterQueryAware;
import org.eclipse.hawkbit.ui.rollout.window.components.AutoStartOptionGroupLayout.AutoStartOption;

/**
 * Proxy entity representing rollout form layout bean.
 */
public class ProxyRolloutForm implements Serializable, NameAware, DsIdAware, TargetFilterQueryAware, DescriptionAware,
        ActionTypeAware, StartOptionAware {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private ProxyTargetFilterQueryInfo targetFilterInfo;
    private ProxyDistributionSetInfo dsInfo;
    private String description;
    private ActionType actionType;
    private Long forcedTime;
    private AutoStartOption autoStartOption;
    private Long startAt;

    /**
     * Gets the rollout form id
     * 
     * @return id
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the form id
     *
     * @param id
     *            rollout form id
     */
    public void setId(final Long id) {
        this.id = id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(final String name) {
        this.name = name;
    }

    @Override
    public ProxyTargetFilterQueryInfo getTargetFilterQueryInfo() {
        return targetFilterInfo;
    }

    @Override
    public void setTargetFilterQueryInfo(final ProxyTargetFilterQueryInfo tfqInfo) {
        this.targetFilterInfo = tfqInfo;
    }

    public String getTargetFilterQuery() {
        return targetFilterInfo != null ? targetFilterInfo.getQuery() : null;
    }

    public void setTargetFilterQuery(final String targetFilterQuery) {
        if (targetFilterInfo != null) {
            targetFilterInfo.setQuery(targetFilterQuery);
        } else {
            targetFilterInfo = new ProxyTargetFilterQueryInfo(null, null, targetFilterQuery);
        }
    }

    @Override
    public ProxyDistributionSetInfo getDistributionSetInfo() {
        return dsInfo;
    }

    @Override
    public void setDistributionSetInfo(final ProxyDistributionSetInfo dsInfo) {
        this.dsInfo = dsInfo;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(final String description) {
        this.description = description;
    }

    @Override
    public ActionType getActionType() {
        return actionType;
    }

    @Override
    public void setActionType(final ActionType actionType) {
        this.actionType = actionType;
    }

    @Override
    public Long getForcedTime() {
        return forcedTime;
    }

    @Override
    public void setForcedTime(final Long forcedTime) {
        this.forcedTime = forcedTime;
    }

    @Override
    public AutoStartOption getStartOption() {
        return autoStartOption;
    }

    @Override
    public void setStartOption(final AutoStartOption autoStartOption) {
        this.autoStartOption = autoStartOption;
    }

    @Override
    public Long getStartAt() {
        return startAt;
    }

    @Override
    public void setStartAt(final Long startAt) {
        this.startAt = startAt;
    }
}
