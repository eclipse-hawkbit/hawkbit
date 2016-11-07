/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts.event;

import java.io.Serializable;

import org.eclipse.hawkbit.repository.model.SoftwareModule;

/**
 * 
 * Holds file and upload status details.Meta data sent with upload events.
 *
 */
public class UploadFileStatus implements Serializable {

    private static final long serialVersionUID = -3599629192216760811L;

    private final String fileName;

    private long contentLength;

    private long bytesRead;

    private String failureReason;

    private SoftwareModule softwareModule;

    /**
     * constructor for UploadFileStatus
     * 
     * @param fileName
     *            name of the file to be uploaded
     */
    public UploadFileStatus(final String fileName) {
        this.fileName = fileName;
    }

    /**
     * constructor for UploadFileStatus
     * 
     * @param fileName
     *            name of the file to be uploaded
     * @param bytesRead
     *            number of bytes
     * @param contentLength
     *            length of the content (stream)
     * @param softwareModule
     *            softwareModule
     */
    public UploadFileStatus(final String fileName, final long bytesRead, final long contentLength,
            final SoftwareModule softwareModule) {
        this.fileName = fileName;
        this.contentLength = contentLength;
        this.bytesRead = bytesRead;
        this.softwareModule = softwareModule;
    }

    /**
     * constructor for UploadFileStatus
     * 
     * @param fileName
     *            name of the file to be uploaded
     * @param failureReason
     *            reason of failure
     * @param selectedSw
     *            the selected softwareModule
     */
    public UploadFileStatus(final String fileName, final String failureReason, final SoftwareModule selectedSw) {
        this.failureReason = failureReason;
        this.fileName = fileName;
        this.softwareModule = selectedSw;
    }

    public String getFileName() {
        return fileName;
    }

    public long getContentLength() {
        return contentLength;
    }

    public long getBytesRead() {
        return bytesRead;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public SoftwareModule getSoftwareModule() {
        return softwareModule;
    }
}
