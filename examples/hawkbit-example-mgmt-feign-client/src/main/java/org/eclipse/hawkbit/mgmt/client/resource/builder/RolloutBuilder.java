/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.client.resource.builder;

import java.util.List;

import org.eclipse.hawkbit.mgmt.json.model.rollout.MgmtRolloutCondition;
import org.eclipse.hawkbit.mgmt.json.model.rollout.MgmtRolloutCondition.Condition;
import org.eclipse.hawkbit.mgmt.json.model.rollout.MgmtRolloutRestRequestBody;
import org.eclipse.hawkbit.mgmt.json.model.rolloutgroup.MgmtRolloutGroup;

/**
 * 
 * Builder pattern for building {@link MgmtRolloutRestRequestBody}.
 *
 */
// Exception squid:S1701 - builder pattern
@SuppressWarnings({ "squid:S1701" })
public class RolloutBuilder {

    private String name;
    private int groupSize;
    private String targetFilterQuery;
    private long distributionSetId;
    private String successThreshold;
    private String errorThreshold;
    private String description;
    private List<MgmtRolloutGroup> semiAutomaticGroups;

    /**
     * @param name
     *            the name of the rollout
     * @return the builder itself
     */
    public RolloutBuilder name(final String name) {
        this.name = name;
        return this;
    }

    /**
     * @param semiAutomaticGroups
     *            as alternative to full automatic, i.e. {@link #groupSize(int)}
     * @return the builder itself
     */
    public RolloutBuilder semiAutomaticGroups(final List<MgmtRolloutGroup> semiAutomaticGroups) {
        this.semiAutomaticGroups = semiAutomaticGroups;
        return this;
    }

    /**
     * @param groupSize
     *            the amount of groups the rollout should be split into
     * @return the builder itself
     */
    public RolloutBuilder groupSize(final int groupSize) {
        this.groupSize = groupSize;
        return this;
    }

    /**
     * @param targetFilterQuery
     *            the FIQL query language to filter targets to contain in the
     *            rollout
     * @return the builder itself
     */
    public RolloutBuilder targetFilterQuery(final String targetFilterQuery) {
        this.targetFilterQuery = targetFilterQuery;
        return this;
    }

    /**
     * @param description
     *            the description
     * @return the builder itself
     */
    public RolloutBuilder description(final String description) {
        this.description = description;
        return this;
    }

    /**
     * @param distributionSetId
     *            the ID of the distribution set to assign to the target in the
     *            rollout
     * @return the builder itself
     */
    public RolloutBuilder distributionSetId(final long distributionSetId) {
        this.distributionSetId = distributionSetId;
        return this;
    }

    /**
     * @param successThreshold
     *            the threshold to be used to indicate if a deployment group is
     *            successful, to trigger the success action
     * @return the builder itself
     */
    public RolloutBuilder successThreshold(final String successThreshold) {
        this.successThreshold = successThreshold;
        return this;
    }

    /**
     * @param errorThreshold
     *            the threshold to be used to indicate if a deployment group is
     *            failing, to trigger the error action
     * @return the builder itself
     */
    public RolloutBuilder errorThreshold(final String errorThreshold) {
        this.errorThreshold = errorThreshold;
        return this;
    }

    /**
     * Builds the rollout rest body to creating a rollout.
     * 
     * @return the rest request body for creating a rollout
     */
    public MgmtRolloutRestRequestBody build() {
        return doBuild();
    }

    private MgmtRolloutRestRequestBody doBuild() {
        final MgmtRolloutRestRequestBody body = new MgmtRolloutRestRequestBody();
        body.setName(name);
        body.setAmountGroups(groupSize);
        body.setTargetFilterQuery(targetFilterQuery);
        body.setDistributionSetId(distributionSetId);
        body.setDescription(description);
        body.setSuccessCondition(new MgmtRolloutCondition(Condition.THRESHOLD, successThreshold));
        body.setErrorCondition(new MgmtRolloutCondition(Condition.THRESHOLD, errorThreshold));
        body.setGroups(semiAutomaticGroups);
        return body;
    }

}
