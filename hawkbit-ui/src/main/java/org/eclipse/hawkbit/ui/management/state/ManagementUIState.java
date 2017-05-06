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

import org.eclipse.hawkbit.ui.common.ManagementEntityState;
import org.eclipse.hawkbit.ui.common.entity.DistributionSetIdName;
import org.eclipse.hawkbit.ui.common.entity.TargetIdName;
import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.VaadinSessionScope;

/**
 * User action on management UI.
 */
@VaadinSessionScope
@SpringComponent
public class ManagementUIState implements ManagementEntityState, Serializable {

    private static final long serialVersionUID = 1L;

    private final transient Set<Object> expandParentActionRowId = new HashSet<>();

    private final DistributionTableFilters distributionTableFilters;

    private final TargetTableFilters targetTableFilters;

    private final Map<TargetIdName, DistributionSetIdName> assignedList = new HashMap<>();

    private final Set<DistributionSetIdName> deletedDistributionList = new HashSet<>();

    private final Set<TargetIdName> deletedTargetList = new HashSet<>();

    private Boolean targetTagLayoutVisible = Boolean.TRUE;

    private Boolean distTagLayoutVisible = Boolean.FALSE;

    private transient Optional<Long> lastSelectedTargetId = Optional.empty();

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

    public boolean isBulkUploadWindowMinimised() {
        return bulkUploadWindowMinimised;
    }

    public void setBulkUploadWindowMinimised(final boolean bulkUploadWindowMinimised) {
        this.bulkUploadWindowMinimised = bulkUploadWindowMinimised;
    }

    public boolean isCustomFilterSelected() {
        return customFilterSelected;
    }

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

    public Optional<Long> getLastSelectedTargetId() {
        return lastSelectedTargetId;
    }

    public void setLastSelectedTargetId(final Long lastSelectedTargetId) {
        this.lastSelectedTargetId = Optional.ofNullable(lastSelectedTargetId);
    }

    public Set<Long> getSelectedTargetId() {
        return selectedTargetId;
    }

    public void setSelectedTargetId(final Set<Long> selectedTargetId) {
        this.selectedTargetId = selectedTargetId;
    }

    public boolean isTargetTagFilterClosed() {
        return targetTagFilterClosed;
    }

    public void setTargetTagFilterClosed(final boolean targetTagFilterClosed) {
        this.targetTagFilterClosed = targetTagFilterClosed;
    }

    public boolean isDistTagFilterClosed() {
        return distTagFilterClosed;
    }

    public void setDistTagFilterClosed(final boolean distTagFilterClosed) {
        this.distTagFilterClosed = distTagFilterClosed;
    }

    public Long getTargetsTruncated() {
        return targetsTruncated;
    }

    public void setTargetsTruncated(final Long targetsTruncated) {
        this.targetsTruncated = targetsTruncated;
    }

    public long getTargetsCountAll() {
        return targetsCountAll.get();
    }

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
    public void setLastSelectedEntityId(final Long value) {
        this.lastSelectedDsIdName = value;
    }

    @Override
    public void setSelectedEnitities(final Set<Long> values) {
        this.selectedDsIdName = values;
    }

    public Set<Long> getSelectedDsIdName() {
        return selectedDsIdName;
    }

    public boolean isTargetTableMaximized() {
        return targetTableMaximized;
    }

    public void setTargetTableMaximized(final boolean isTargetTableMaximized) {
        this.targetTableMaximized = isTargetTableMaximized;
    }

    public boolean isActionHistoryMaximized() {
        return actionHistoryMaximized;
    }

    public void setActionHistoryMaximized(final boolean isActionHistoryMaximized) {
        this.actionHistoryMaximized = isActionHistoryMaximized;
    }

    public boolean isNoDataAvilableTarget() {
        return noDataAvilableTarget;
    }

    public void setNoDataAvilableTarget(final boolean noDataAvilableTarget) {
        this.noDataAvilableTarget = noDataAvilableTarget;
    }

    public boolean isNoDataAvailableDistribution() {
        return noDataAvailableDistribution;
    }

    public void setNoDataAvailableDistribution(final boolean noDataAvailableDistribution) {
        this.noDataAvailableDistribution = noDataAvailableDistribution;
    }
}
