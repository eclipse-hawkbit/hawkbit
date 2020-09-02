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
    private Long distributionSetId;
    private Long targetFilterId;
    private String targetFilterQuery;
    private String description;
    private ActionType actionType;
    private Long forcedTime;
    private AutoStartOption autoStartOption;
    private Long startAt;

    /**
     * Gets the rollout form id
     * @return id
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the form id
     *
     * @param id
     *         rollout form id
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
    public Long getDistributionSetId() {
        return distributionSetId;
    }

    @Override
    public void setDistributionSetId(final Long distributionSetId) {
        this.distributionSetId = distributionSetId;
    }

    @Override
    public Long getTargetFilterId() {
        return targetFilterId;
    }

    @Override
    public void setTargetFilterId(final Long targetFilterId) {
        this.targetFilterId = targetFilterId;
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
