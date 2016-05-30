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
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtActionType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Model for request containing a rollout body e.g. in a POST request of
 * creating a rollout via REST API.
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MgmtRolloutRestRequestBody extends MgmtNamedEntity {

    private String targetFilterQuery;
    private long distributionSetId;

    private int amountGroups = 1;

    private MgmtRolloutCondition successCondition = new MgmtRolloutCondition();
    private MgmtRolloutSuccessAction successAction = new MgmtRolloutSuccessAction();
    private MgmtRolloutCondition errorCondition = null;
    private MgmtRolloutErrorAction errorAction = null;

    private Long forcetime;

    private MgmtActionType type;

    /**
     * @return the finishCondition
     */
    public MgmtRolloutCondition getSuccessCondition() {
        return successCondition;
    }

    /**
     * @param successCondition
     *            the finishCondition to set
     */
    public void setSuccessCondition(final MgmtRolloutCondition successCondition) {
        this.successCondition = successCondition;
    }

    /**
     * @return the successAction
     */
    public MgmtRolloutSuccessAction getSuccessAction() {
        return successAction;
    }

    /**
     * @param successAction
     *            the successAction to set
     */
    public void setSuccessAction(final MgmtRolloutSuccessAction successAction) {
        this.successAction = successAction;
    }

    /**
     * @return the errorCondition
     */
    public MgmtRolloutCondition getErrorCondition() {
        return errorCondition;
    }

    /**
     * @param errorCondition
     *            the errorCondition to set
     */
    public void setErrorCondition(final MgmtRolloutCondition errorCondition) {
        this.errorCondition = errorCondition;
    }

    /**
     * @return the targetFilterQuery
     */
    public String getTargetFilterQuery() {
        return targetFilterQuery;
    }

    /**
     * @param targetFilterQuery
     *            the targetFilterQuery to set
     */
    public void setTargetFilterQuery(final String targetFilterQuery) {
        this.targetFilterQuery = targetFilterQuery;
    }

    /**
     * @return the distributionSetId
     */
    public long getDistributionSetId() {
        return distributionSetId;
    }

    /**
     * @param distributionSetId
     *            the distributionSetId to set
     */
    public void setDistributionSetId(final long distributionSetId) {
        this.distributionSetId = distributionSetId;
    }

    /**
     * @return the groupSize
     */
    public int getAmountGroups() {
        return amountGroups;
    }

    /**
     * @param groupSize
     *            the groupSize to set
     */
    public void setAmountGroups(final int groupSize) {
        this.amountGroups = groupSize;
    }

    /**
     * @return the forcetime
     */
    public Long getForcetime() {
        return forcetime;
    }

    /**
     * @param forcetime
     *            the forcetime to set
     */
    public void setForcetime(final Long forcetime) {
        this.forcetime = forcetime;
    }

    /**
     * @return the type
     */
    public MgmtActionType getType() {
        return type;
    }

    /**
     * @param type
     *            the type to set
     */
    public void setType(final MgmtActionType type) {
        this.type = type;
    }

    /**
     * @return the errorAction
     */
    public MgmtRolloutErrorAction getErrorAction() {
        return errorAction;
    }

    /**
     * @param errorAction
     *            the errorAction to set
     */
    public void setErrorAction(final MgmtRolloutErrorAction errorAction) {
        this.errorAction = errorAction;
    }

}
