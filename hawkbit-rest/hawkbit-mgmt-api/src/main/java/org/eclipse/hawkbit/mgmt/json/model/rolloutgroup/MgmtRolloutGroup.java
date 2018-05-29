/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.json.model.rolloutgroup;

import org.eclipse.hawkbit.mgmt.json.model.rollout.AbstractMgmtRolloutConditionsEntity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Model for defining the Attributes of a Rollout Group
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MgmtRolloutGroup extends AbstractMgmtRolloutConditionsEntity {

    private String targetFilterQuery;
    private Float targetPercentage;

    public String getTargetFilterQuery() {
        return targetFilterQuery;
    }

    public void setTargetFilterQuery(final String targetFilterQuery) {
        this.targetFilterQuery = targetFilterQuery;
    }

    public Float getTargetPercentage() {
        return targetPercentage;
    }

    public void setTargetPercentage(Float targetPercentage) {
        this.targetPercentage = targetPercentage;
    }
}
