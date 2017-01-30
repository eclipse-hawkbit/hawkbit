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

import org.eclipse.hawkbit.ui.common.ManagmentEntityState;
import org.eclipse.hawkbit.ui.common.entity.DistributionSetIdName;
import org.eclipse.hawkbit.ui.common.entity.TargetIdName;
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
public class ManagementUIState implements ManagmentEntityState<Long>, Serializable {

    private static final long serialVersionUID = 7301409196969723794L;

    private final transient Set<Object> expandParentActionRowId = new HashSet<>();

    private final DistributionTableFilters distributionTableFilters;

    private final TargetTableFilters targetTableFilters;

    private final Map<TargetIdName, DistributionSetIdName> assignedList = new HashMap<>();

    private final Set<DistributionSetIdName> deletedDistributionList = new HashSet<>();

    private final Set<TargetIdName> deletedTargetList = new HashSet<>();

    private Boolean targetTagLayoutVisible = Boolean.TRUE;

    private Boolean distTagLayoutVisible = Boolean.FALSE;

    private Long lastSelectedTargetId;
    private Set<Long> selectedTargetId = Collections.emptySet();

    private boolean targetTagFilterClosed;

    private boolean distTagFilterClosed = true;

    private Long targetsTruncated;

    private final AtomicLong targetsCountAll = new AtomicLong();

    private boolean dsTableMaximized;

    private Long lastSelectedDsIdName;

    private Set<Long> selectedDsIdName = Collections.emptySet();

    private boolean targetTableMaximized;

    private boolean actionHistoryMaximized;

    private boolean noDataAvilableTarget;

    private boolean noDataAvailableDistribution;

    private final Set<String> canceledTargetName = new HashSet<>();

    private boolean customFilterSelected;

    private boolean bulkUploadWindowMinimised;

    @Autowired
    ManagementUIState(final DistributionTableFilters distributionTableFilters,
            final TargetTableFilters targetTableFilters) {
        this.distributionTableFilters = distributionTableFilters;
        this.targetTableFilters = targetTableFilters;
    }

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
        customFilterSelected = isCustomFilterSelected;
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
        targetTagLayoutVisible = targetTagVisible;
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

    public Long getLastSelectedTargetId() {
        return lastSelectedTargetId;
    }

    public void setLastSelectedTargetId(final Long lastSelectedTargetId) {
        this.lastSelectedTargetId = lastSelectedTargetId;
    }

    public Optional<Set<Long>> getSelectedTargetId() {
        return Optional.ofNullable(selectedTargetId);
    }

    public void setSelectedTargetId(final Set<Long> selectedTargetId) {
        this.selectedTargetId = selectedTargetId;
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

    public boolean isDsTableMaximized() {
        return dsTableMaximized;
    }

    public void setDsTableMaximized(final boolean isDsTableMaximized) {
        this.dsTableMaximized = isDsTableMaximized;
    }

    public Long getLastSelectedDsIdName() {
        return lastSelectedDsIdName;
    }

    @Override
    public void setLastSelectedEntity(final Long value) {
        this.lastSelectedDsIdName = value;
    }

    @Override
    public void setSelectedEnitities(final Set<Long> values) {
        this.selectedDsIdName = values;

    }

    public Optional<Set<Long>> getSelectedDsIdName() {
        return Optional.ofNullable(selectedDsIdName);
    }

    /**
     * @return the isTargetTableMaximized
     */
    public boolean isTargetTableMaximized() {
        return targetTableMaximized;
    }

    /**
     * @param isTargetTableMaximized
     *            the isTargetTableMaximized to set
     */
    public void setTargetTableMaximized(final boolean isTargetTableMaximized) {
        this.targetTableMaximized = isTargetTableMaximized;
    }

    /**
     * @return the isActionHistoryMaximized
     */
    public boolean isActionHistoryMaximized() {
        return actionHistoryMaximized;
    }

    /**
     * @param isActionHistoryMaximized
     *            the isActionHistoryMaximized to set
     */
    public void setActionHistoryMaximized(final boolean isActionHistoryMaximized) {
        this.actionHistoryMaximized = isActionHistoryMaximized;
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
