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
public class ManageDistUIState implements Serializable {

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

    private boolean isSwModuleTableMaximized = Boolean.FALSE;

    private boolean isDsTableMaximized = Boolean.FALSE;

    private final Map<String, SoftwareModuleIdName> assignedSoftwareModuleDetails = new HashMap<>();

    private final Map<DistributionSetIdName, HashMap<Long, HashSet<SoftwareModuleIdName>>> consolidatedDistSoftwarewList = new HashMap<>();

    private boolean noDataAvilableSwModule = Boolean.FALSE;

    private boolean noDataAvailableDist = Boolean.FALSE;

    /**
     * @return the manageDistFilters
     */
    public ManageDistFilters getManageDistFilters() {
        return this.manageDistFilters;
    }

    /**
     * @return the deletedDistributionList
     */
    public Set<DistributionSetIdName> getDeletedDistributionList() {
        return this.deletedDistributionList;
    }

    /**
     * Need HashSet because the Set have to be serializable
     *
     * @return the assignedList
     */
    public Map<DistributionSetIdName, HashSet<SoftwareModuleIdName>> getAssignedList() {
        return this.assignedList;
    }

    /**
     * @return the slectedDistributions
     */
    public Optional<Set<DistributionSetIdName>> getSelectedDistributions() {
        return this.selectedDistributions == null ? Optional.empty() : Optional.of(this.selectedDistributions);
    }

    /**
     * @return the lastSelectedDistribution
     */
    public Optional<DistributionSetIdName> getLastSelectedDistribution() {
        return this.lastSelectedDistribution == null ? Optional.empty() : Optional.of(this.lastSelectedDistribution);
    }

    /**
     * @param lastSelectedDistribution
     *            the lastSelectedDistribution to set
     */
    public void setLastSelectedDistribution(final DistributionSetIdName lastSelectedDistribution) {
        this.lastSelectedDistribution = lastSelectedDistribution;
    }

    public void setSelectedDistributions(final Set<DistributionSetIdName> slectedDistributions) {
        this.selectedDistributions = slectedDistributions;
    }

    /**
     * @return the softwareModuleFilters
     */
    public ManageSoftwareModuleFilters getSoftwareModuleFilters() {
        return this.softwareModuleFilters;
    }

    /**
     * @return the selectedSoftwareModules
     */
    public Set<Long> getSelectedSoftwareModules() {
        return this.selectedSoftwareModules;
    }

    /**
     * @return the selectedBaseSwModuleId
     */
    public Optional<Long> getSelectedBaseSwModuleId() {
        return this.selectedBaseSwModuleId != null ? Optional.of(this.selectedBaseSwModuleId) : Optional.empty();
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
        return this.distTypeFilterClosed;
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
        return this.swTypeFilterClosed;
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
        return this.deleteSofwareModulesList;
    }

    public Set<String> getSelectedDeleteDistSetTypes() {
        return this.selectedDeleteDistSetTypes;
    }

    public void setSelectedDeleteDistSetTypes(final Set<String> selectedDeleteDistSetTypes) {
        this.selectedDeleteDistSetTypes = selectedDeleteDistSetTypes;
    }

    public Set<String> getSelectedDeleteSWModuleTypes() {
        return this.selectedDeleteSWModuleTypes;
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
        return this.isDsTableMaximized;
    }

    /***
     * Set isDsModuleTableMaximized.
     *
     * @param isDsModuleTableMaximized
     */
    public void setDsTableMaximized(final boolean isDsModuleTableMaximized) {
        this.isDsTableMaximized = isDsModuleTableMaximized;
    }

    public Map<String, SoftwareModuleIdName> getAssignedSoftwareModuleDetails() {
        return this.assignedSoftwareModuleDetails;
    }

    /**
     * @return the isSwModuleTableMaximized
     */
    public boolean isSwModuleTableMaximized() {
        return this.isSwModuleTableMaximized;
    }

    /**
     * @param isSwModuleTableMaximized
     *            the isSwModuleTableMaximized to set
     */
    public void setSwModuleTableMaximized(final boolean isSwModuleTableMaximized) {
        this.isSwModuleTableMaximized = isSwModuleTableMaximized;
    }

    /**
     * @return the noDataAvilableSwModule
     */
    public boolean isNoDataAvilableSwModule() {
        return this.noDataAvilableSwModule;
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
        return this.noDataAvailableDist;
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
        return this.consolidatedDistSoftwarewList;
    }

}
