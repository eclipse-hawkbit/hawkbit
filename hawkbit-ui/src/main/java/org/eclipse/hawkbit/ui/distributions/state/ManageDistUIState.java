/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.distributions.state;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.eclipse.hawkbit.ui.common.ManagementEntityState;
import org.eclipse.hawkbit.ui.common.entity.DistributionSetIdName;
import org.eclipse.hawkbit.ui.common.entity.SoftwareModuleIdName;
import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.VaadinSessionScope;

/**
 * Manage Distributions user state.
 */
@SpringComponent
@VaadinSessionScope
public class ManageDistUIState implements ManagementEntityState, Serializable {

    private static final long serialVersionUID = 1L;

    private final ManageDistFilters manageDistFilters;

    private final ManageSoftwareModuleFilters softwareModuleFilters;

    private final Map<DistributionSetIdName, HashSet<SoftwareModuleIdName>> assignedList = new HashMap<>();

    private final Set<DistributionSetIdName> deletedDistributionList = new HashSet<>();

    private Set<Long> selectedDistributions = new HashSet<>();

    private transient Optional<Long> lastSelectedDistribution = Optional.empty();

    private transient Optional<Long> lastSelectedSoftwareModule = Optional.empty();

    private Set<Long> selectedSoftwareModules = Collections.emptySet();

    private final Set<String> selectedDeleteDistSetTypes = new HashSet<>();

    private Set<String> selectedDeleteSWModuleTypes = new HashSet<>();

    private boolean distTypeFilterClosed;

    private boolean swTypeFilterClosed;

    private final Map<Long, String> deleteSofwareModulesList = new HashMap<>();

    private boolean swModuleTableMaximized;

    private boolean dsTableMaximized;

    private final Map<String, SoftwareModuleIdName> assignedSoftwareModuleDetails = new HashMap<>();

    private final Map<DistributionSetIdName, HashMap<Long, HashSet<SoftwareModuleIdName>>> consolidatedDistSoftwarewList = new HashMap<>();

    private boolean noDataAvilableSwModule;

    private boolean noDataAvailableDist;

    @Autowired
    ManageDistUIState(final ManageDistFilters manageDistFilters,
            final ManageSoftwareModuleFilters softwareModuleFilters) {
        this.manageDistFilters = manageDistFilters;
        this.softwareModuleFilters = softwareModuleFilters;
    }

    public ManageDistFilters getManageDistFilters() {
        return manageDistFilters;
    }

    public Set<DistributionSetIdName> getDeletedDistributionList() {
        return deletedDistributionList;
    }

    /**
     * Need HashSet because the Set have to be serializable
     *
     * @return the assignedList
     */
    public Map<DistributionSetIdName, HashSet<SoftwareModuleIdName>> getAssignedList() {
        return assignedList;
    }

    public Set<Long> getSelectedDistributions() {
        return selectedDistributions;
    }

    public Optional<Long> getLastSelectedDistribution() {
        return lastSelectedDistribution;
    }

    @Override
    public void setLastSelectedEntityId(final Long value) {
        this.lastSelectedDistribution = Optional.ofNullable(value);
    }

    @Override
    public void setSelectedEnitities(final Set<Long> values) {
        selectedDistributions = values;
    }

    public ManageSoftwareModuleFilters getSoftwareModuleFilters() {
        return softwareModuleFilters;
    }

    public Optional<Long> getLastSelectedSoftwareModule() {
        return lastSelectedSoftwareModule;
    }

    public void setLastSelectedSoftwareModule(final Long value) {
        this.lastSelectedSoftwareModule = Optional.ofNullable(value);
    }

    public Set<Long> getSelectedSoftwareModules() {
        return selectedSoftwareModules;
    }

    public void setSelectedSoftwareModules(final Set<Long> selectedSoftwareModules) {
        this.selectedSoftwareModules = selectedSoftwareModules;
    }

    public boolean isDistTypeFilterClosed() {
        return distTypeFilterClosed;
    }

    public void setDistTypeFilterClosed(final boolean distTypeFilterClosed) {
        this.distTypeFilterClosed = distTypeFilterClosed;
    }

    public boolean isSwTypeFilterClosed() {
        return swTypeFilterClosed;
    }

    public void setSwTypeFilterClosed(final boolean swTypeFilterClosed) {
        this.swTypeFilterClosed = swTypeFilterClosed;
    }

    public Map<Long, String> getDeleteSofwareModulesList() {
        return deleteSofwareModulesList;
    }

    public Set<String> getSelectedDeleteDistSetTypes() {
        return selectedDeleteDistSetTypes;
    }

    public Set<String> getSelectedDeleteSWModuleTypes() {
        return selectedDeleteSWModuleTypes;
    }

    public void setSelectedDeleteSWModuleTypes(final Set<String> selectedDeleteSWModuleTypes) {
        this.selectedDeleteSWModuleTypes = selectedDeleteSWModuleTypes;
    }

    public boolean isDsTableMaximized() {
        return dsTableMaximized;
    }

    public void setDsTableMaximized(final boolean dsModuleTableMaximized) {
        dsTableMaximized = dsModuleTableMaximized;
    }

    public Map<String, SoftwareModuleIdName> getAssignedSoftwareModuleDetails() {
        return assignedSoftwareModuleDetails;
    }

    public boolean isSwModuleTableMaximized() {
        return swModuleTableMaximized;
    }

    public void setSwModuleTableMaximized(final boolean swModuleTableMaximized) {
        this.swModuleTableMaximized = swModuleTableMaximized;
    }

    public boolean isNoDataAvilableSwModule() {
        return noDataAvilableSwModule;
    }

    public void setNoDataAvilableSwModule(final boolean noDataAvilableSwModule) {
        this.noDataAvilableSwModule = noDataAvilableSwModule;
    }

    public boolean isNoDataAvailableDist() {
        return noDataAvailableDist;
    }

    public void setNoDataAvailableDist(final boolean noDataAvailableDist) {
        this.noDataAvailableDist = noDataAvailableDist;
    }

    public Map<DistributionSetIdName, HashMap<Long, HashSet<SoftwareModuleIdName>>> getConsolidatedDistSoftwarewList() {
        return consolidatedDistSoftwarewList;
    }

}
