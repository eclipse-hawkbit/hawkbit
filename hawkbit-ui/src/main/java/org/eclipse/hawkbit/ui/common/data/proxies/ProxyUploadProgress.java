/**
 * Copyright (c) 2020 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ui.common.data.proxies;

import java.security.SecureRandom;

import org.eclipse.hawkbit.ui.artifacts.upload.FileUploadId;

/**
 * Proxy for UploadProgressGrid.
 */
public class ProxyUploadProgress extends ProxyIdentifiableEntity {
    private static final long serialVersionUID = 1L;

    private FileUploadId fileUploadId;
    private ProgressSatus status;
    private double progress;
    private String reason;

    /**
     * Constructor for ProxyUploadProgress
     */
    public ProxyUploadProgress() {
        setId(new SecureRandom().nextLong());
    }

    /**
     * Gets the file upload status
     *
     * @return status
     */
    public ProgressSatus getStatus() {
        return status;
    }

    /**
     * Sets the status
     *
     * @param status
     *         file upload status
     */
    public void setStatus(final ProgressSatus status) {
        this.status = status;
    }

    /**
     * Gets the file upload progress
     *
     * @return progress
     */
    public double getProgress() {
        return progress;
    }

    /**
     * Sets the progress
     *
     * @param progress
     *         file upload progress
     */
    public void setProgress(final double progress) {
        this.progress = progress;
    }

    /**
     * Gets the file upload reason
     *
     * @return reason
     */
    public String getReason() {
        return reason;
    }

    /**
     * Sets the reason
     *
     * @param reason
     *         file upload reason
     */
    public void setReason(final String reason) {
        this.reason = reason;
    }

    /**
     * Gets the fileUploadId
     *
     * @return fileUploadId
     */
    public FileUploadId getFileUploadId() {
        return fileUploadId;
    }

    /**
     * Sets the fileUploadId
     *
     * @param fileUploadId
     *         Id of fileUpload process
     */
    public void setFileUploadId(final FileUploadId fileUploadId) {
        this.fileUploadId = fileUploadId;
    }

    /**
     * Status of the file upload process
     */
    public enum ProgressSatus {
        INPROGRESS, FINISHED, FAILED;
    }
}
