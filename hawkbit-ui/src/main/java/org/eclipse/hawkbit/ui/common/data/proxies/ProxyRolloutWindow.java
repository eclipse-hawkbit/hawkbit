/**
 * Copyright (c) 2020 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ui.common.data.proxies;

import java.io.Serializable;
import java.util.List;

import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.Rollout.ApprovalDecision;
import org.eclipse.hawkbit.ui.rollout.window.components.AutoStartOptionGroupLayout;
import org.eclipse.hawkbit.ui.rollout.window.components.AutoStartOptionGroupLayout.AutoStartOption;

/**
 * Proxy entity representing rollout popup window bean.
 */
public class ProxyRolloutWindow implements Serializable {
    private static final long serialVersionUID = 1L;

    private ProxyRolloutForm rolloutForm;
    private ProxySimpleRolloutGroupsDefinition simpleGroupsDefinition;
    private transient List<ProxyAdvancedRolloutGroup> advancedRolloutGroupDefinitions;
    private GroupDefinitionMode groupDefinitionMode;
    private ProxyRolloutApproval rolloutApproval;

    /**
     * Constructor
     */
    public ProxyRolloutWindow() {
        this.rolloutForm = new ProxyRolloutForm();
        this.simpleGroupsDefinition = new ProxySimpleRolloutGroupsDefinition();
        this.rolloutApproval = new ProxyRolloutApproval();
    }

    /**
     * Constructor for ProxyRolloutWindow
     *
     * @param rollout
     *            ProxyRollout
     */
    public ProxyRolloutWindow(final ProxyRollout rollout) {
        this();

        setId(rollout.getId());
        setName(rollout.getName());
        setDescription(rollout.getDescription());
        setActionType(rollout.getActionType());
        setStartAt(rollout.getStartAt());
        setForcedTime(rollout.getForcedTime());
        setTargetFilterQuery(rollout.getTargetFilterQuery());
        setDistributionSetInfo(rollout.getDsInfo());
        setNumberOfGroups(rollout.getNumberOfGroups());
    }

    /**
     * Gets the rollout form id
     *
     * @return id
     */
    public Long getId() {
        return rolloutForm.getId();
    }

    /**
     * Sets the form id
     *
     * @param id
     *            rollout form id
     */
    public void setId(final Long id) {
        rolloutForm.setId(id);
    }

    /**
     * Gets the action type
     *
     * @return rollout action type
     */
    public ActionType getActionType() {
        return rolloutForm.getActionType();
    }

    /**
     * Sets the actionType
     *
     * @param actionType
     *            rollout action type
     */
    public void setActionType(final ActionType actionType) {
        rolloutForm.setActionType(actionType);
    }

    /**
     * @return the numberOfGroups
     */
    public Integer getNumberOfGroups() {
        return simpleGroupsDefinition.getNumberOfGroups();
    }

    /**
     * @param numberOfGroups
     *            the numberOfGroups to set
     */
    public void setNumberOfGroups(final Integer numberOfGroups) {
        simpleGroupsDefinition.setNumberOfGroups(numberOfGroups);
    }

    /**
     * Gets the rollout form name
     *
     * @return form name
     */
    public String getName() {
        return rolloutForm.getName();
    }

    /**
     * Sets the form name
     *
     * @param name
     *            rollout form name
     */
    public void setName(final String name) {
        rolloutForm.setName(name);
    }

    /**
     * Gets the rollout form description
     *
     * @return form description
     */
    public String getDescription() {
        return rolloutForm.getDescription();
    }

    /**
     * Sets the form description
     *
     * @param description
     *            rollout form description
     */
    public void setDescription(final String description) {
        rolloutForm.setDescription(description);
    }

    /**
     * Gets the rollout form forced time
     *
     * @return form forced time
     */
    public Long getForcedTime() {
        return rolloutForm.getForcedTime();
    }

    /**
     * Sets the form forcedTime
     *
     * @param forcedTime
     *            Rollout form forced time
     */
    public void setForcedTime(final Long forcedTime) {
        rolloutForm.setForcedTime(forcedTime);
    }

    /**
     * Gets the approvalRemark
     *
     * @return approvalRemark
     */
    public String getApprovalRemark() {
        return rolloutApproval.getApprovalRemark();
    }

    /**
     * Sets the approvalRemark
     *
     * @param approvalRemark
     *            Remark for approval
     */
    public void setApprovalRemark(final String approvalRemark) {
        rolloutApproval.setApprovalRemark(approvalRemark);
    }

    /**
     * Gets the time rollout start time
     *
     * @return startAt
     */
    public Long getStartAt() {
        return rolloutForm.getStartAt();
    }

    /**
     * Sets the start time
     *
     * @param startAt
     *            time rollout start time
     */
    public void setStartAt(final Long startAt) {
        rolloutForm.setStartAt(startAt);
    }

    /**
     * @return Rollout form targetFilter Info
     */
    public ProxyTargetFilterQueryInfo getTargetFilterInfo() {
        return rolloutForm.getTargetFilterQueryInfo();
    }

    /**
     * Sets the targetFilter Info
     *
     * @param tfqInfo
     *            Info of rollout form targetFilter
     */
    public void setTargetFilterInfo(final ProxyTargetFilterQueryInfo tfqInfo) {
        rolloutForm.setTargetFilterQueryInfo(tfqInfo);
    }

    /**
     * Gets the rollout form targetFilterQuery
     *
     * @return targetFilterQuery
     */
    public String getTargetFilterQuery() {
        return rolloutForm.getTargetFilterQuery();
    }

    /**
     * Sets the targetFilterQuery
     *
     * @param targetFilterQuery
     *            Rollout form target filter query
     */
    public void setTargetFilterQuery(final String targetFilterQuery) {
        rolloutForm.setTargetFilterQuery(targetFilterQuery);
    }

    /**
     * Gets the Id of rollout form distribution set
     *
     * @return distributionSetId
     */
    public Long getDistributionSetId() {
        return rolloutForm.getDistributionSetInfo() != null ? rolloutForm.getDistributionSetInfo().getId() : null;
    }

    /**
     * Sets the distribution set info
     *
     * @param dsInfo
     *            Info of rollout form distribution set
     */
    public void setDistributionSetInfo(final ProxyDistributionSetInfo dsInfo) {
        rolloutForm.setDistributionSetInfo(dsInfo);
    }

    /**
     * Gets the triggerThresholdPercentage
     *
     * @return triggerThresholdPercentage
     */
    public String getTriggerThresholdPercentage() {
        return simpleGroupsDefinition.getTriggerThresholdPercentage();
    }

    /**
     * Sets the triggerThresholdPercentage
     *
     * @param triggerThresholdPercentage
     *            triggerThresholdPercentage value of rollout simple group
     */
    public void setTriggerThresholdPercentage(final String triggerThresholdPercentage) {
        simpleGroupsDefinition.setTriggerThresholdPercentage(triggerThresholdPercentage);
    }

    /**
     * Gets the errorThresholdPercentage
     *
     * @return errorThresholdPercentage
     */
    public String getErrorThresholdPercentage() {
        return simpleGroupsDefinition.getErrorThresholdPercentage();
    }

    public boolean isConfirmationRequired() {
        return simpleGroupsDefinition.isConfirmationRequired();
    }

    /**
     * Sets the errorThresholdPercentage
     *
     * @param errorThresholdPercentage
     *            errorThresholdPercentage value of rollout simple group
     */
    public void setErrorThresholdPercentage(final String errorThresholdPercentage) {
        simpleGroupsDefinition.setErrorThresholdPercentage(errorThresholdPercentage);
    }

    /**
     * Gets the rollout approval decision
     *
     * @return approvalDecision
     */
    public ApprovalDecision getApprovalDecision() {
        return rolloutApproval.getApprovalDecision();
    }

    /**
     * Sets the rollout approvalDecision
     *
     * @param approvalDecision
     *            Rollout decesion approval or deny
     */
    public void setApprovalDecision(final ApprovalDecision approvalDecision) {
        rolloutApproval.setApprovalDecision(approvalDecision);
    }

    /**
     * Gets the auto start option
     *
     * @return Rollout auto start options
     */
    public AutoStartOption getAutoStartOption() {
        return rolloutForm.getStartOption();
    }

    /**
     * Sets the autoStartOption
     *
     * @param autoStartOption
     *            Rollout auto start options
     */
    public void setAutoStartOption(final AutoStartOption autoStartOption) {
        rolloutForm.setStartOption(autoStartOption);
    }

    /**
     * Gets the rollout form
     *
     * @return rolloutForm
     */
    public ProxyRolloutForm getRolloutForm() {
        return rolloutForm;
    }

    /**
     * Sets the rolloutForm
     *
     * @param rolloutForm
     *            Rollout form
     */
    public void setRolloutForm(final ProxyRolloutForm rolloutForm) {
        this.rolloutForm = rolloutForm;
    }

    /**
     * Gets the rollout simpleGroupsDefinition
     *
     * @return ProxySimpleRolloutGroupsDefinition
     */
    public ProxySimpleRolloutGroupsDefinition getSimpleGroupsDefinition() {
        return simpleGroupsDefinition;
    }

    /**
     * Sets the rollout simpleRolloutGroupsDefinition
     *
     * @param simpleGroupsDefinition
     *            ProxySimpleRolloutGroupsDefinition
     */
    public void setSimpleGroupsDefinition(final ProxySimpleRolloutGroupsDefinition simpleGroupsDefinition) {
        this.simpleGroupsDefinition = simpleGroupsDefinition;
    }

    /**
     * @return Rollout start time in milliseconds
     */
    public Long getStartAtByOption() {
        switch (getAutoStartOption()) {
        case AUTO_START:
            return System.currentTimeMillis();
        case SCHEDULED:
            return getStartAt();
        case MANUAL:
        default:
            return null;
        }
    }

    /**
     * @return Rollout auto start option
     */
    public AutoStartOption getOptionByStartAt() {
        if (getStartAt() == null) {
            return AutoStartOptionGroupLayout.AutoStartOption.MANUAL;
        } else if (getStartAt() < System.currentTimeMillis()) {
            return AutoStartOptionGroupLayout.AutoStartOption.AUTO_START;
        } else {
            return AutoStartOptionGroupLayout.AutoStartOption.SCHEDULED;
        }
    }

    /**
     * Gets the Rollout group definition mode
     *
     * @return groupDefinitionMode
     */
    public GroupDefinitionMode getGroupDefinitionMode() {
        return groupDefinitionMode;
    }

    /**
     * Sets the groupDefinitionMode
     *
     * @param groupDefinitionMode
     *            Rollout group definition mode
     */
    public void setGroupDefinitionMode(final GroupDefinitionMode groupDefinitionMode) {
        this.groupDefinitionMode = groupDefinitionMode;
    }

    /**
     * Gest the List of rolloutGroupDefinitions
     *
     * @return advancedRolloutGroupDefinitions
     */
    public List<ProxyAdvancedRolloutGroup> getAdvancedRolloutGroupDefinitions() {
        return advancedRolloutGroupDefinitions;
    }

    /**
     * Sets the advancedRolloutGroupDefinitions
     *
     * @param advancedRolloutGroupDefinitions
     *            List of rolloutGroupDefinitions
     */
    public void setAdvancedRolloutGroupDefinitions(
            final List<ProxyAdvancedRolloutGroup> advancedRolloutGroupDefinitions) {
        this.advancedRolloutGroupDefinitions = advancedRolloutGroupDefinitions;
    }

    /**
     * Gets the rollout approval
     *
     * @return rolloutApproval
     */
    public ProxyRolloutApproval getRolloutApproval() {
        return rolloutApproval;
    }

    /**
     * Sets the rolloutApproval
     *
     * @param rolloutApproval
     *            Rollout approval
     */
    public void setRolloutApproval(final ProxyRolloutApproval rolloutApproval) {
        this.rolloutApproval = rolloutApproval;
    }

    /**
     * Rollout group definition modes
     */
    public enum GroupDefinitionMode {
        SIMPLE, ADVANCED;
    }
}
