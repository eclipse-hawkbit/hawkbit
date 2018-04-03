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
 * Represents the data of a row in the {@link UploadStatusInfoWindow}.
 */
public class UploadStatusInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private final FileUploadId fileUploadId;

    private String status;
    private Double progress;
    private String reason;


    UploadStatusInfo(final FileUploadId fileUploadId) {
        this.fileUploadId = fileUploadId;
    }

    public SoftwareModule getSelectedSoftwareModule() {
        return fileUploadId.getSoftwareModule();
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
        return fileUploadId.getFilename();
    }

    public String getReason() {
        return reason;
    }

    public void setReason(final String reason) {
        this.reason = reason;
    }

    @Override
    public int hashCode() {
        return fileUploadId.hashCode();
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
        final UploadStatusInfo other = (UploadStatusInfo) obj;
        return fileUploadId.equals(other.fileUploadId);
    }

}
