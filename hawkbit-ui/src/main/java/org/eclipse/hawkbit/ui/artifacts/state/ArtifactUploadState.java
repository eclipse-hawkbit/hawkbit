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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.ui.artifacts.upload.UploadStatusObject;
import org.eclipse.hawkbit.ui.common.ManagmentEntityState;
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
public class ArtifactUploadState implements ManagmentEntityState<Long>, Serializable {

    private static final long serialVersionUID = 8273440375917450859L;

    @Autowired
    private SoftwareModuleFilters softwareModuleFilters;

    private final Map<Long, String> deleteSofwareModules = new HashMap<>();

    private final Set<CustomFile> fileSelected = new HashSet<>();

    private Long selectedBaseSwModuleId;

    private SoftwareModule selectedBaseSoftwareModule;

    private final Map<String, SoftwareModule> baseSwModuleList = new HashMap<>();

    private Set<Long> selectedSoftwareModules = Collections.emptySet();

    private boolean swTypeFilterClosed = Boolean.FALSE;

    private boolean swModuleTableMaximized = Boolean.FALSE;

    private boolean artifactDetailsMaximized = Boolean.FALSE;

    private final Set<String> selectedDeleteSWModuleTypes = new HashSet<>();

    private boolean noDataAvilableSoftwareModule = Boolean.FALSE;
    
    private boolean isStatusPopupMinimized  = Boolean.FALSE;
    
    private boolean isUploadCompleted = Boolean.FALSE;
    
    private List<UploadStatusObject> uploadedFileStatusList = new ArrayList<>();
    
    private final AtomicInteger numberOfFileUploadsExpected = new AtomicInteger();

    private final AtomicInteger numberOfFilesActuallyUpload = new AtomicInteger();
    
    private final AtomicInteger numberOfFileUploadsFailed = new AtomicInteger();

    public AtomicInteger getNumberOfFileUploadsFailed() {
        return numberOfFileUploadsFailed;
    }
    
    public AtomicInteger getNumberOfFilesActuallyUpload() {
        return numberOfFilesActuallyUpload;
    }
    
    public AtomicInteger getNumberOfFileUploadsExpected() {
        return numberOfFileUploadsExpected;
    }
    
    
    public List<UploadStatusObject> getUploadedFileStatusList() {
        return uploadedFileStatusList;
    }
    
    public void setUploadedFileStatusList(List<UploadStatusObject> uploadedFileStatusList) {
        this.uploadedFileStatusList = uploadedFileStatusList;
    }
    
    public boolean isUploadCompleted() {
        return isUploadCompleted;
    }
    
    public void setUploadCompleted(boolean isUploadCompleted) {
        this.isUploadCompleted = isUploadCompleted;
    }

    
    public void setStatusPopupMinimized(boolean isStatusPopupMinimized) {
		this.isStatusPopupMinimized = isStatusPopupMinimized;
	}
    
    public boolean isStatusPopupMinimized() {
		return isStatusPopupMinimized;
	}




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
        return Optional.ofNullable(selectedBaseSwModuleId);
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

    @Override
    public void setLastSelectedEntity(final Long value) {
        this.selectedBaseSwModuleId = value;

    }

    @Override
    public void setSelectedEnitities(final Set<Long> values) {
        this.selectedSoftwareModules = values;
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
        return swModuleTableMaximized;
    }

    /**
     * @param isSwModuleTableMaximized
     *            the isSwModuleTableMaximized to set
     */
    public void setSwModuleTableMaximized(final boolean swModuleTableMaximized) {
        this.swModuleTableMaximized = swModuleTableMaximized;
    }

    public Set<String> getSelectedDeleteSWModuleTypes() {
        return selectedDeleteSWModuleTypes;
    }

    /**
     * @return the isArtifactDetailsMaximized
     */
    public boolean isArtifactDetailsMaximized() {
        return artifactDetailsMaximized;
    }

    /**
     * @param isArtifactDetailsMaximized
     *            the isArtifactDetailsMaximized to set
     */
    public void setArtifactDetailsMaximized(final boolean artifactDetailsMaximized) {
        this.artifactDetailsMaximized = artifactDetailsMaximized;
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

    public Optional<SoftwareModule> getSelectedBaseSoftwareModule() {
        return Optional.ofNullable(selectedBaseSoftwareModule);
    }

    public void setSelectedBaseSoftwareModule(final SoftwareModule selectedBaseSoftwareModule) {
        this.selectedBaseSoftwareModule = selectedBaseSoftwareModule;
    }
}
