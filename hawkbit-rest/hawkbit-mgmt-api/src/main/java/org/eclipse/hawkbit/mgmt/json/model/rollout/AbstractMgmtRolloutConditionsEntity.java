/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.json.model.rollout;

import org.eclipse.hawkbit.mgmt.json.model.MgmtNamedEntity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Model for defining Conditions and Actions
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class AbstractMgmtRolloutConditionsEntity extends MgmtNamedEntity {

    private MgmtRolloutCondition successCondition;
    private MgmtRolloutSuccessAction successAction;
    private MgmtRolloutCondition errorCondition;
    private MgmtRolloutErrorAction errorAction;

    public MgmtRolloutCondition getSuccessCondition() {
        return successCondition;
    }

    public void setSuccessCondition(final MgmtRolloutCondition successCondition) {
        this.successCondition = successCondition;
    }

    public MgmtRolloutSuccessAction getSuccessAction() {
        return successAction;
    }

    public void setSuccessAction(final MgmtRolloutSuccessAction successAction) {
        this.successAction = successAction;
    }

    public MgmtRolloutCondition getErrorCondition() {
        return errorCondition;
    }

    public void setErrorCondition(final MgmtRolloutCondition errorCondition) {
        this.errorCondition = errorCondition;
    }

    public MgmtRolloutErrorAction getErrorAction() {
        return errorAction;
    }

    public void setErrorAction(final MgmtRolloutErrorAction errorAction) {
        this.errorAction = errorAction;
    }

}
