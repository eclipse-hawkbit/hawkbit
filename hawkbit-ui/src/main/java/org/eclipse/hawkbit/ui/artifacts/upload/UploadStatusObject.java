/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts.upload;

import org.eclipse.hawkbit.repository.model.SoftwareModule;

/**
 * 
 * Holds uploaded file status.Used to display the details in upload status
 * popup.
 *
 */
public class UploadStatusObject {
    private String status;
    private Double progress;
    private String filename;
    private String reason;
    private SoftwareModule selectedSoftwareModule;

    public UploadStatusObject(final String status, final Double progress, final String fileName, final String reason,
            final SoftwareModule selectedSoftwareModule) {
        this(fileName,selectedSoftwareModule);
        this.status = status;
        this.progress = progress;
        this.reason = reason;
    }

    public UploadStatusObject(String fileName, SoftwareModule selectedSoftwareModule) {
        this.filename = fileName;
        this.selectedSoftwareModule = selectedSoftwareModule;
    }

    public SoftwareModule getSelectedSoftwareModule() {
        return selectedSoftwareModule;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Double getProgress() {
        return progress;
    }

    public void setProgress(Double progress) {
        this.progress = progress;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == null || obj == null) {
            return false;
        }
        if (obj instanceof UploadStatusObject && this.getFilename() == ((UploadStatusObject) obj).getFilename()) {
            return true;
        }
        return false;
    }

}
