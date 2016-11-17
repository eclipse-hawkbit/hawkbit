/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.model;

import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupErrorAction;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupErrorCondition;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupSuccessAction;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupSuccessCondition;

/**
 * Object which holds all {@link RolloutGroup} conditions together which can
 * easily built.
 */
public class RolloutGroupConditions {
    private RolloutGroupSuccessCondition successCondition;
    private String successConditionExp;
    private RolloutGroupSuccessAction successAction;
    private String successActionExp;
    private RolloutGroupErrorCondition errorCondition;
    private String errorConditionExp;
    private RolloutGroupErrorAction errorAction;
    private String errorActionExp;

    public RolloutGroupSuccessCondition getSuccessCondition() {
        return successCondition;
    }

    public void setSuccessCondition(final RolloutGroupSuccessCondition finishCondition) {
        successCondition = finishCondition;
    }

    public String getSuccessConditionExp() {
        return successConditionExp;
    }

    public void setSuccessConditionExp(final String finishConditionExp) {
        successConditionExp = finishConditionExp;
    }

    public RolloutGroupSuccessAction getSuccessAction() {
        return successAction;
    }

    public void setSuccessAction(final RolloutGroupSuccessAction successAction) {
        this.successAction = successAction;
    }

    public String getSuccessActionExp() {
        return successActionExp;
    }

    public void setSuccessActionExp(final String successActionExp) {
        this.successActionExp = successActionExp;
    }

    public RolloutGroupErrorCondition getErrorCondition() {
        return errorCondition;
    }

    public void setErrorCondition(final RolloutGroupErrorCondition errorCondition) {
        this.errorCondition = errorCondition;
    }

    public String getErrorConditionExp() {
        return errorConditionExp;
    }

    public void setErrorConditionExp(final String errorConditionExp) {
        this.errorConditionExp = errorConditionExp;
    }

    public RolloutGroupErrorAction getErrorAction() {
        return errorAction;
    }

    public void setErrorAction(final RolloutGroupErrorAction errorAction) {
        this.errorAction = errorAction;
    }

    public String getErrorActionExp() {
        return errorActionExp;
    }

    public void setErrorActionExp(final String errorActionExp) {
        this.errorActionExp = errorActionExp;
    }
}
