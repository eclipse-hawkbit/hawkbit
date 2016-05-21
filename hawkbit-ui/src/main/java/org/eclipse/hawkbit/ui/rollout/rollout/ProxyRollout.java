/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.rollout.rollout;

import java.util.Set;

import org.eclipse.hawkbit.repository.jpa.model.JpaRollout;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.ui.customrenderers.client.renderers.RolloutRendererData;

import com.vaadin.server.FontAwesome;

/**
 * Proxy rollout with custom properties.
 *
 */
public class ProxyRollout extends JpaRollout {

    private static final long serialVersionUID = 4539849939617681918L;

    private String distributionSetNameVersion;

    private String createdDate;

    private String modifiedDate;

    private Long numberOfGroups;

    private Boolean isActionRecieved = Boolean.FALSE;

    private Boolean isRequiredMigrationStep = Boolean.FALSE;

    private String totalTargetsCount;

    private RolloutRendererData rolloutRendererData;

    private String discription;

    private String type;

    private Set<SoftwareModule> swModules;

    /**
     * @return the isRequiredMigrationStep
     */

    public Boolean getIsRequiredMigrationStep() {
        return isRequiredMigrationStep;
    }

    /**
     * @param isRequiredMigrationStep
     *            the isRequiredMigrationStep to set
     */

    public void setIsRequiredMigrationStep(final Boolean isRequiredMigrationStep) {
        this.isRequiredMigrationStep = isRequiredMigrationStep;
    }

    /**
     * @return the discription
     */

    public String getDiscription() {
        return discription;
    }

    /**
     * @param discription
     *            the discription to set
     */

    public void setDiscription(final String discription) {
        this.discription = discription;
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @param type
     *            the type to set
     */

    public void setType(final String type) {
        this.type = type;
    }

    /**
     * 
     * @return the Set of Software modules
     */
    public Set<SoftwareModule> getSwModules() {
        return swModules;
    }

    /**
     * @param swModules
     *            Set<SoftwareModule> to set
     */
    public void setSwModules(final Set<SoftwareModule> swModules) {
        this.swModules = swModules;
    }

    public RolloutRendererData getRolloutRendererData() {
        return rolloutRendererData;
    }

    public void setRolloutRendererData(final RolloutRendererData rendererData) {
        this.rolloutRendererData = rendererData;
    }

    /**
     * @return the distributionSetNameVersion
     */
    public String getDistributionSetNameVersion() {
        return distributionSetNameVersion;
    }

    /**
     * @param distributionSetNameVersion
     *            the distributionSetNameVersion to set
     */
    public void setDistributionSetNameVersion(final String distributionSetNameVersion) {
        this.distributionSetNameVersion = distributionSetNameVersion;
    }

    /**
     * @return the numberOfGroups
     */
    public Long getNumberOfGroups() {
        return numberOfGroups;
    }

    /**
     * @param numberOfGroups
     *            the numberOfGroups to set
     */
    public void setNumberOfGroups(final Long numberOfGroups) {
        this.numberOfGroups = numberOfGroups;
    }

    /**
     * @return the createdDate
     */
    public String getCreatedDate() {
        return createdDate;
    }

    /**
     * @param createdDate
     *            the createdDate to set
     */
    public void setCreatedDate(final String createdDate) {
        this.createdDate = createdDate;
    }

    /**
     * @return the modifiedDate
     */
    public String getModifiedDate() {
        return modifiedDate;
    }

    /**
     * @param modifiedDate
     *            the modifiedDate to set
     */
    public void setModifiedDate(final String modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    /**
     * @return the isActionRecieved
     */
    public Boolean getIsActionRecieved() {
        return isActionRecieved;
    }

    /**
     * @param isActionRecieved
     *            the isActionRecieved to set
     */
    public void setIsActionRecieved(final Boolean isActionRecieved) {
        this.isActionRecieved = isActionRecieved;
    }

    /**
     * @return the totalTargetsCount
     */
    public String getTotalTargetsCount() {
        return totalTargetsCount;
    }

    /**
     * @param totalTargetsCount
     *            the totalTargetsCount to set
     */
    public void setTotalTargetsCount(final String totalTargetsCount) {
        this.totalTargetsCount = totalTargetsCount;
    }

    public String getAction() {
        return FontAwesome.CIRCLE_O.getHtml();
    }

}
