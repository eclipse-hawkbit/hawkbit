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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.hawkbit.repository.model.DistributionSetIdName;
import org.eclipse.hawkbit.repository.model.TargetIdName;
import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.VaadinSessionScope;

/**
 * User action on management UI.
 *
 *
 */
@VaadinSessionScope
@SpringComponent
public class ManagementUIState implements Serializable {

    private static final long serialVersionUID = 7301409196969723794L;

    @Autowired
    private DistributionTableFilters distributionTableFilters;

    @Autowired
    private TargetTableFilters targetTableFilters;

    private final Map<TargetIdName, DistributionSetIdName> assignedList = new HashMap<TargetIdName, DistributionSetIdName>();

    private final Set<DistributionSetIdName> deletedDistributionList = new HashSet<DistributionSetIdName>();

    private final Set<TargetIdName> deletedTargetList = new HashSet<TargetIdName>();

    private Boolean targetTagLayoutVisible = Boolean.TRUE;

    private Boolean distTagLayoutVisible = Boolean.FALSE;

    // Contains ID and NAme of last selected target
    private TargetIdName lastSelectedTargetIdName;
    // Contains list of ID and Names of all the selected Targets
    private Set<TargetIdName> selectedTargetIdName = Collections.emptySet();

    private boolean targetTagFilterClosed = false;

    private boolean distTagFilterClosed = true;

    private Long targetsTruncated = null;

    private final AtomicLong targetsCountAll = new AtomicLong();

    private boolean isDsTableMaximized = Boolean.FALSE;

    // Contains ID and NAme of last selected target
    private DistributionSetIdName lastSelectedDsIdName;
    // Contains list of ID and Names of all the selected Targets
    private Set<DistributionSetIdName> selectedDsIdName = Collections.emptySet();

    private boolean isTargetTableMaximized = Boolean.FALSE;

    private boolean isActionHistoryMaximized = Boolean.FALSE;

    private boolean noDataAvilableTarget = Boolean.FALSE;

    private boolean noDataAvailableDistribution = Boolean.FALSE;

    private final Set<String> canceledTargetName = new HashSet<String>();

    private final Set<Object> expandParentActionRowId = new HashSet<Object>();

    private boolean customFilterSelected;

    private boolean bulkUploadWindowMinimised;

    /**
     * @return the bulkUploadWindowMinimised
     */
    public boolean isBulkUploadWindowMinimised() {
        return bulkUploadWindowMinimised;
    }

    /**
     * @param bulkUploadWindowMinimised
     *            the bulkUploadWindowMinimised to set
     */
    public void setBulkUploadWindowMinimised(final boolean bulkUploadWindowMinimised) {
        this.bulkUploadWindowMinimised = bulkUploadWindowMinimised;
    }

    /**
     * @return the isCustomFilterSelected
     */
    public boolean isCustomFilterSelected() {
        return customFilterSelected;
    }

    /**
     * @param isCustomFilterSelected
     *            the isCustomFilterSelected to set
     */
    public void setCustomFilterSelected(final boolean isCustomFilterSelected) {
        this.customFilterSelected = isCustomFilterSelected;
    }

    public Set<Object> getExpandParentActionRowId() {
        return expandParentActionRowId;
    }

    public Set<String> getCanceledTargetName() {
        return canceledTargetName;
    }

    public void setDistTagLayoutVisible(final Boolean distTagLayoutVisible) {
        this.distTagLayoutVisible = distTagLayoutVisible;
    }

    public Boolean getDistTagLayoutVisible() {
        return distTagLayoutVisible;
    }

    public void setTargetTagLayoutVisible(final Boolean targetTagVisible) {
        this.targetTagLayoutVisible = targetTagVisible;
    }

    public Boolean getTargetTagLayoutVisible() {
        return targetTagLayoutVisible;
    }

    public TargetTableFilters getTargetTableFilters() {
        return targetTableFilters;
    }

    public DistributionTableFilters getDistributionTableFilters() {
        return distributionTableFilters;
    }

    public Map<TargetIdName, DistributionSetIdName> getAssignedList() {
        return assignedList;
    }

    public Set<DistributionSetIdName> getDeletedDistributionList() {
        return deletedDistributionList;
    }

    public Set<TargetIdName> getDeletedTargetList() {
        return deletedTargetList;
    }

    public TargetIdName getLastSelectedTargetIdName() {
        return lastSelectedTargetIdName;
    }

    public void setLastSelectedTargetIdName(final TargetIdName lastSelectedTargetIdName) {
        this.lastSelectedTargetIdName = lastSelectedTargetIdName;
    }

    public Optional<Set<TargetIdName>> getSelectedTargetIdName() {
        return selectedTargetIdName == null ? Optional.empty() : Optional.of(selectedTargetIdName);
    }

    public void setSelectedTargetIdName(final Set<TargetIdName> selectedTargetIdName) {
        this.selectedTargetIdName = selectedTargetIdName;
    }

    /**
     * @return the targetTagFilterClosed
     */
    public boolean isTargetTagFilterClosed() {
        return targetTagFilterClosed;
    }

    /**
     * @param targetTagFilterClosed
     *            the targetTagFilterClosed to set
     */
    public void setTargetTagFilterClosed(final boolean targetTagFilterClosed) {
        this.targetTagFilterClosed = targetTagFilterClosed;
    }

    /**
     * @return the distTagFilterClosed
     */
    public boolean isDistTagFilterClosed() {
        return distTagFilterClosed;
    }

    /**
     * @param distTagFilterClosed
     *            the distTagFilterClosed to set
     */
    public void setDistTagFilterClosed(final boolean distTagFilterClosed) {
        this.distTagFilterClosed = distTagFilterClosed;
    }

    /**
     * @return the targetsTruncated
     */
    public Long getTargetsTruncated() {
        return targetsTruncated;
    }

    /**
     * @param targetsTruncated
     *            the targetsTruncated to set
     */
    public void setTargetsTruncated(final Long targetsTruncated) {
        this.targetsTruncated = targetsTruncated;
    }

    /**
     * @return the targetsCountAll
     */
    public long getTargetsCountAll() {
        return targetsCountAll.get();
    }

    /**
     * @param targetsCountAll
     *            the targetsCountAll to set
     */
    public void setTargetsCountAll(final long targetsCountAll) {
        this.targetsCountAll.set(targetsCountAll);
    }

    /**
     * increments the targets all counter.
     */
    public void incrementTargetsCountAll() {
        this.targetsCountAll.incrementAndGet();
    }

    /**
     * decrement the targets all counter.
     */
    public void decrementTargetsCountAll() {
        final long decrementAndGet = this.targetsCountAll.decrementAndGet();
        if (decrementAndGet < 0) {
            this.targetsCountAll.set(0);
        }
    }

    public boolean isDsTableMaximized() {
        return isDsTableMaximized;
    }

    public void setDsTableMaximized(final boolean isDsTableMaximized) {
        this.isDsTableMaximized = isDsTableMaximized;
    }

    public DistributionSetIdName getLastSelectedDsIdName() {
        return lastSelectedDsIdName;
    }

    public void setLastSelectedDsIdName(final DistributionSetIdName lastSelectedDsIdName) {
        this.lastSelectedDsIdName = lastSelectedDsIdName;
    }

    public void setSelectedDsIdName(final Set<DistributionSetIdName> selectedDsIdName) {
        this.selectedDsIdName = selectedDsIdName;
    }

    public Optional<Set<DistributionSetIdName>> getSelectedDsIdName() {
        return selectedDsIdName == null ? Optional.empty() : Optional.of(selectedDsIdName);
    }

    /**
     * @return the isTargetTableMaximized
     */
    public boolean isTargetTableMaximized() {
        return isTargetTableMaximized;
    }

    /**
     * @param isTargetTableMaximized
     *            the isTargetTableMaximized to set
     */
    public void setTargetTableMaximized(final boolean isTargetTableMaximized) {
        this.isTargetTableMaximized = isTargetTableMaximized;
    }

    /**
     * @return the isActionHistoryMaximized
     */
    public boolean isActionHistoryMaximized() {
        return isActionHistoryMaximized;
    }

    /**
     * @param isActionHistoryMaximized
     *            the isActionHistoryMaximized to set
     */
    public void setActionHistoryMaximized(final boolean isActionHistoryMaximized) {
        this.isActionHistoryMaximized = isActionHistoryMaximized;
    }

    /**
     * @return the noDataAvilableTarget
     */
    public boolean isNoDataAvilableTarget() {
        return noDataAvilableTarget;
    }

    /**
     * @param noDataAvilableTarget
     *            the noDataAvilableTarget to set
     */
    public void setNoDataAvilableTarget(final boolean noDataAvilableTarget) {
        this.noDataAvilableTarget = noDataAvilableTarget;
    }

    /**
     * @return the noDataAvailableDistribution
     */
    public boolean isNoDataAvailableDistribution() {
        return noDataAvailableDistribution;
    }

    /**
     * @param noDataAvailableDistribution
     *            the noDataAvailableDistribution to set
     */
    public void setNoDataAvailableDistribution(final boolean noDataAvailableDistribution) {
        this.noDataAvailableDistribution = noDataAvailableDistribution;
    }
}
