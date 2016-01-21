/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.state;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;

import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.VaadinSessionScope;

/**
 * Holds the filter parameters for target table.
 *
 *
 */

@SpringComponent
@VaadinSessionScope
public class TargetFilterParameters implements Serializable {
    private static final long serialVersionUID = 3929484222793744883L;

    private String searchText;

    private final List<TargetUpdateStatus> status = new ArrayList<TargetUpdateStatus>();

    private List<String> targetTags = new ArrayList<String>();

    private Long distributionSetId;

    private Long pinnedDistId;

    /**
     * Get search Text.
     * 
     * @return string as search text
     */
    public String getSearchText() {
        return searchText;
    }

    /**
     * Set Search Text.
     * 
     * @param searchText
     *            as string
     */
    public void setSearchText(final String searchText) {
        this.searchText = searchText;
    }

    /**
     * Get update status.
     * 
     * @return TargetUpdateStatus
     */
    public List<TargetUpdateStatus> getStatus() {
        return status;
    }

    /**
     * Adds Target update status.
     * 
     * @param status
     *            as Target update
     */
    public void addStatus(final TargetUpdateStatus status) {
        this.status.add(status);
    }

    /**
     * Remoces Target update status.
     * 
     * @param status
     *            as Target update
     */
    public void removeStatus(final TargetUpdateStatus status) {
        this.status.remove(status);
    }

    /**
     * Get Target Tags.
     * 
     * @return List of Tags
     */
    public List<String> getTargetTags() {
        return targetTags;
    }

    /**
     * Set Target Tags.
     * 
     * @param targetTags
     *            as tags
     */
    public void setTargetTags(final List<String> targetTags) {
        this.targetTags = targetTags;
    }

    /**
     * Get Distribution ID.
     * 
     * @return Dist ID
     */
    public Long getDistributionSetId() {
        return distributionSetId;
    }

    /**
     * Set distribution ID.
     * 
     * @param distributionSetId
     *            as ID
     */
    public void setDistributionSetId(final Long distributionSetId) {
        this.distributionSetId = distributionSetId;
    }

    /**
     * @return Returns the pinnedDistId.
     */
    public Long getPinnedDistId() {
        return pinnedDistId;
    }

    /**
     * @param pinnedDistId
     *            The pinned distribution set Id to set.
     */
    public void setPinnedDistId(final Long pinnedDistId) {
        this.pinnedDistId = pinnedDistId;
    }

}
