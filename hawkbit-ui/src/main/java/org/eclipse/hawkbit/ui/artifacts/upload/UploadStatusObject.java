/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts.upload;

import java.io.Serializable;

import org.eclipse.hawkbit.repository.model.SoftwareModule;

/**
 * 
 * Holds uploaded file status.Used to display the details in upload status
 * popup.
 *
 */
public class UploadStatusObject implements Serializable {
    private static final long serialVersionUID = 1L;

    private String status;
    private Double progress;
    private String filename;
    private String reason;
    private final SoftwareModule selectedSoftwareModule;

    UploadStatusObject(final String fileName, final SoftwareModule selectedSoftwareModule) {
        this.filename = fileName;
        this.selectedSoftwareModule = selectedSoftwareModule;
    }

    public SoftwareModule getSelectedSoftwareModule() {
        return selectedSoftwareModule;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public Double getProgress() {
        return progress;
    }

    public void setProgress(final Double progress) {
        this.progress = progress;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(final String filename) {
        this.filename = filename;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(final String reason) {
        this.reason = reason;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((filename == null) ? 0 : filename.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final UploadStatusObject other = (UploadStatusObject) obj;
        if (filename == null) {
            if (other.filename != null) {
                return false;
            }
        } else if (!filename.equals(other.filename)) {
            return false;
        }
        return true;
    }

}
