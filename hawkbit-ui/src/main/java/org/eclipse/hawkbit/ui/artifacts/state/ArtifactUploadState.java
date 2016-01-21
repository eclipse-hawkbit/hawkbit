/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts.state;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.VaadinSessionScope;

/**
 * User status of Artifact upload.
 *
 *
 */
@VaadinSessionScope
@SpringComponent
public class ArtifactUploadState implements Serializable {

    private static final long serialVersionUID = 8273440375917450859L;

    @Autowired
    private SoftwareModuleFilters softwareModuleFilters;

    private final Map<Long, String> deleteSofwareModules = new HashMap<>();

    private final Set<CustomFile> fileSelected = new HashSet<CustomFile>();

    private Long selectedBaseSwModuleId;

    private SoftwareModule selectedBaseSoftwareModule;

    private final Map<String, SoftwareModule> baseSwModuleList = new HashMap<String, SoftwareModule>();

    private Set<Long> selectedSoftwareModules = Collections.emptySet();

    private boolean swTypeFilterClosed = Boolean.FALSE;

    private boolean isSwModuleTableMaximized = Boolean.FALSE;

    private boolean isArtifactDetailsMaximized = Boolean.FALSE;

    private final Set<String> selectedDeleteSWModuleTypes = new HashSet<>();

    private boolean noDataAvilableSoftwareModule = Boolean.FALSE;

    /**
     * Set software.
     * 
     * @return
     */
    public SoftwareModuleFilters getSoftwareModuleFilters() {
        return softwareModuleFilters;
    }

    /**
     * @return the selectedSofwareModules
     */
    public Map<Long, String> getDeleteSofwareModules() {
        return deleteSofwareModules;
    }

    /**
     * @return the fileSelected
     */
    public Set<CustomFile> getFileSelected() {
        return fileSelected;
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
     * @return the selectedBaseSoftwareModule
     */
    public Optional<SoftwareModule> getSelectedBaseSoftwareModule() {
        return this.selectedBaseSoftwareModule == null ? Optional.empty()
                : Optional.of(this.selectedBaseSoftwareModule);
    }

    /**
     * @param selectedBaseSoftwareModule
     *            the selectedBaseSoftwareModule to set
     */
    public void setSelectedBaseSoftwareModule(final SoftwareModule selectedBaseSoftwareModule) {
        this.selectedBaseSoftwareModule = selectedBaseSoftwareModule;
    }

    /**
     * @return the baseSwModuleList
     */
    public Map<String, SoftwareModule> getBaseSwModuleList() {
        return baseSwModuleList;
    }

    /**
     * @return the selectedSoftwareModules
     */
    public Set<Long> getSelectedSoftwareModules() {
        return selectedSoftwareModules;
    }

    /**
     * @param selectedSoftwareModules
     *            the selectedSoftwareModules to set
     */
    public void setSelectedSoftwareModules(final Set<Long> selectedSoftwareModules) {
        this.selectedSoftwareModules = selectedSoftwareModules;
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
     * @return the isSwModuleTableMaximized
     */
    public boolean isSwModuleTableMaximized() {
        return isSwModuleTableMaximized;
    }

    /**
     * @param isSwModuleTableMaximized
     *            the isSwModuleTableMaximized to set
     */
    public void setSwModuleTableMaximized(final boolean isSwModuleTableMaximized) {
        this.isSwModuleTableMaximized = isSwModuleTableMaximized;
    }

    public Set<String> getSelectedDeleteSWModuleTypes() {
        return selectedDeleteSWModuleTypes;
    }

    /**
     * @return the isArtifactDetailsMaximized
     */
    public boolean isArtifactDetailsMaximized() {
        return isArtifactDetailsMaximized;
    }

    /**
     * @param isArtifactDetailsMaximized
     *            the isArtifactDetailsMaximized to set
     */
    public void setArtifactDetailsMaximized(final boolean isArtifactDetailsMaximized) {
        this.isArtifactDetailsMaximized = isArtifactDetailsMaximized;
    }

    /**
     * @return the noDataAvilableSoftwareModule
     */
    public boolean isNoDataAvilableSoftwareModule() {
        return noDataAvilableSoftwareModule;
    }

    /**
     * @param noDataAvilableSoftwareModule
     *            the noDataAvilableSoftwareModule to set
     */
    public void setNoDataAvilableSoftwareModule(final boolean noDataAvilableSoftwareModule) {
        this.noDataAvilableSoftwareModule = noDataAvilableSoftwareModule;
    }

}
