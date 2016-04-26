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

import org.eclipse.hawkbit.repository.model.DistributionSetIdName;
import org.eclipse.hawkbit.repository.model.SoftwareModuleIdName;
import org.eclipse.hawkbit.ui.common.ManagmentEntityState;
import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.VaadinSessionScope;

/**
 * Manage Distributions user state.
 *
 *
 */
@SpringComponent
@VaadinSessionScope
public class ManageDistUIState implements ManagmentEntityState<DistributionSetIdName>, Serializable {

    private static final long serialVersionUID = -7569047247017742928L;

    @Autowired
    private ManageDistFilters manageDistFilters;

    @Autowired
    private ManageSoftwareModuleFilters softwareModuleFilters;

    private final Map<DistributionSetIdName, HashSet<SoftwareModuleIdName>> assignedList = new HashMap<>();

    private final Set<DistributionSetIdName> deletedDistributionList = new HashSet<>();

    private Set<DistributionSetIdName> selectedDistributions = new HashSet<>();

    private DistributionSetIdName lastSelectedDistribution;

    private Set<Long> selectedSoftwareModules = Collections.emptySet();

    private Set<String> selectedDeleteDistSetTypes = new HashSet<>();

    private Set<String> selectedDeleteSWModuleTypes = new HashSet<>();

    private Long selectedBaseSwModuleId;

    private boolean distTypeFilterClosed = Boolean.FALSE;

    private boolean swTypeFilterClosed = Boolean.FALSE;

    private final Map<Long, String> deleteSofwareModulesList = new HashMap<>();

    private boolean swModuleTableMaximized = Boolean.FALSE;

    private boolean dsTableMaximized = Boolean.FALSE;

    private final Map<String, SoftwareModuleIdName> assignedSoftwareModuleDetails = new HashMap<>();

    private final Map<DistributionSetIdName, HashMap<Long, HashSet<SoftwareModuleIdName>>> consolidatedDistSoftwarewList = new HashMap<>();

    private boolean noDataAvilableSwModule = Boolean.FALSE;

    private boolean noDataAvailableDist = Boolean.FALSE;

    /**
     * @return the manageDistFilters
     */
    public ManageDistFilters getManageDistFilters() {
        return manageDistFilters;
    }

    /**
     * @return the deletedDistributionList
     */
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

    /**
     * @return the slectedDistributions
     */
    public Optional<Set<DistributionSetIdName>> getSelectedDistributions() {
        return Optional.ofNullable(selectedDistributions);
    }

    /**
     * @return the lastSelectedDistribution
     */
    public Optional<DistributionSetIdName> getLastSelectedDistribution() {
        return Optional.ofNullable(lastSelectedDistribution);
    }

    @Override
    public void setLastSelectedEntity(final DistributionSetIdName value) {
        this.lastSelectedDistribution = value;
    }

    public void setSelectedEnitities(final Set<DistributionSetIdName> values) {
        selectedDistributions = values;
    }

    /**
     * @return the softwareModuleFilters
     */
    public ManageSoftwareModuleFilters getSoftwareModuleFilters() {
        return softwareModuleFilters;
    }

    /**
     * @return the selectedSoftwareModules
     */
    public Set<Long> getSelectedSoftwareModules() {
        return selectedSoftwareModules;
    }

    /**
     * @return the selectedBaseSwModuleId
     */
    public Optional<Long> getSelectedBaseSwModuleId() {
        return Optional.ofNullable(selectedBaseSwModuleId);
    }

    /**
     * @param selectedBaseSwModuleId
     *            the selectedBaseSwModuleId to set
     */
    public void setSelectedBaseSwModuleId(final Long selectedBaseSwModuleId) {
        this.selectedBaseSwModuleId = selectedBaseSwModuleId;
    }

    /**
     * @param selectedSoftwareModules
     *            the selectedSoftwareModules to set
     */
    public void setSelectedSoftwareModules(final Set<Long> selectedSoftwareModules) {
        this.selectedSoftwareModules = selectedSoftwareModules;
    }

    /**
     * @return the distTypeFilterClosed
     */
    public boolean isDistTypeFilterClosed() {
        return distTypeFilterClosed;
    }

    /**
     * @param distTypeFilterClosed
     *            the distTypeFilterClosed to set
     */
    public void setDistTypeFilterClosed(final boolean distTypeFilterClosed) {
        this.distTypeFilterClosed = distTypeFilterClosed;
    }

    /**
     * @return the swTypeFilterClosed
     */
    public boolean isSwTypeFilterClosed() {
        return swTypeFilterClosed;
    }

    /**
     * @param swTypeFilterClosed
     *            the swTypeFilterClosed to set
     */
    public void setSwTypeFilterClosed(final boolean swTypeFilterClosed) {
        this.swTypeFilterClosed = swTypeFilterClosed;
    }

    /**
     * @return the deleteSofwareModulesList
     */
    public Map<Long, String> getDeleteSofwareModulesList() {
        return deleteSofwareModulesList;
    }

    public Set<String> getSelectedDeleteDistSetTypes() {
        return selectedDeleteDistSetTypes;
    }

    public void setSelectedDeleteDistSetTypes(final Set<String> selectedDeleteDistSetTypes) {
        this.selectedDeleteDistSetTypes = selectedDeleteDistSetTypes;
    }

    public Set<String> getSelectedDeleteSWModuleTypes() {
        return selectedDeleteSWModuleTypes;
    }

    public void setSelectedDeleteSWModuleTypes(final Set<String> selectedDeleteSWModuleTypes) {
        this.selectedDeleteSWModuleTypes = selectedDeleteSWModuleTypes;
    }

    /**
     * Get isSwModuleTableMaximized.
     *
     * @return boolean
     */
    public boolean isDsTableMaximized() {
        return dsTableMaximized;
    }

    /***
     * Set isDsModuleTableMaximized.
     *
     * @param isDsModuleTableMaximized
     */
    public void setDsTableMaximized(final boolean dsModuleTableMaximized) {
        dsTableMaximized = dsModuleTableMaximized;
    }

    public Map<String, SoftwareModuleIdName> getAssignedSoftwareModuleDetails() {
        return assignedSoftwareModuleDetails;
    }

    /**
     * @return the isSwModuleTableMaximized
     */
    public boolean isSwModuleTableMaximized() {
        return swModuleTableMaximized;
    }

    /**
     * @param isSwModuleTableMaximized
     *            the isSwModuleTableMaximized to set
     */
    public void setSwModuleTableMaximized(final boolean swModuleTableMaximized) {
        this.swModuleTableMaximized = swModuleTableMaximized;
    }

    /**
     * @return the noDataAvilableSwModule
     */
    public boolean isNoDataAvilableSwModule() {
        return noDataAvilableSwModule;
    }

    /**
     * @param noDataAvilableSwModule
     *            the noDataAvilableSwModule to set
     */
    public void setNoDataAvilableSwModule(final boolean noDataAvilableSwModule) {
        this.noDataAvilableSwModule = noDataAvilableSwModule;
    }

    /**
     * @return the noDataAvailableDist
     */
    public boolean isNoDataAvailableDist() {
        return noDataAvailableDist;
    }

    /**
     * @param noDilableDist
     *            the noDataAvailableDist to set
     */
    public void setNoDataAvailableDist(final boolean noDataAvailableDist) {
        this.noDataAvailableDist = noDataAvailableDist;
    }

    /**
     * Need HashSet because the Set have to be serializable
     *
     * @return map
     */
    public Map<DistributionSetIdName, HashMap<Long, HashSet<SoftwareModuleIdName>>> getConsolidatedDistSoftwarewList() {
        return consolidatedDistSoftwarewList;
    }

}
