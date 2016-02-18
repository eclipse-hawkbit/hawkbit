/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.rollout;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.hawkbit.repository.model.Rollout;

import com.vaadin.server.FontAwesome;
import com.vaadin.ui.Label;

/**
 * Proxy rollout with suctome properties.
 *
 */
public class ProxyRollout extends Rollout {

    private static final long serialVersionUID = 4539849939617681918L;

    private String distributionSetNameVersion;

    private String createdDate;

    private String modifiedDate;

    private String createdBy;

    private String lastModifiedBy;

    private Long numberOfGroups;

    private Boolean isActionRecieved = Boolean.FALSE;

    private String totalTargetsCount;
    
    //TODO remove this
    private DistributionBarDetails distributionBarDetails ;
    
    //TODO remove this
    private Map<String,Long> statusTotalCountMap = new HashMap<>();
    

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
     * @return the createdBy
     */
    public String getCreatedBy() {
        return createdBy;
    }

    /**
     * @param createdBy
     *            the createdBy to set
     */
    public void setCreatedBy(final String createdBy) {
        this.createdBy = createdBy;
    }

    /**
     * @return the lastModifiedBy
     */
    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    /**
     * @param lastModifiedBy
     *            the lastModifiedBy to set
     */
    public void setLastModifiedBy(final String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
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
    
    public Map<String, Long> getStatusTotalCountMap() {
		return statusTotalCountMap;
	}
    
    public void setStatusTotalCountMap(Map<String, Long> statusTotalCountMap) {
		this.statusTotalCountMap = statusTotalCountMap;
	}
    
    public DistributionBarDetails getDistributionBarDetails() {
		return distributionBarDetails;
	}
    
    public void setDistributionBarDetails(DistributionBarDetails distributionBarDetails) {
		this.distributionBarDetails = distributionBarDetails;
	}

    public String getAction() { 
        return FontAwesome.CIRCLE_O.getHtml();
    }
    
    
    
}
