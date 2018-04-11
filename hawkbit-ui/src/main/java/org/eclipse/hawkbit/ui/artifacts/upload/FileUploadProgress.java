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

/**
 * Holds file and upload progress details that are sent with upload events.
 */
public class FileUploadProgress implements Serializable {

    private static final long serialVersionUID = 1L;

    private final FileUploadId fileUploadId;

    private long contentLength;

    private long bytesRead;

    private String failureReason;

    private String filePath;

    private String mimeType;

    public FileUploadProgress(final FileUploadId fileUploadId) {
        this.fileUploadId = fileUploadId;
    }

    public FileUploadProgress(final FileUploadId fileUploadId, final long bytesRead, final long contentLength,
            final String mimeType,
            final String filePath) {
        this.fileUploadId = fileUploadId;
        this.contentLength = contentLength;
        this.bytesRead = bytesRead;
        this.mimeType = mimeType;
        this.filePath = filePath;
    }

    public FileUploadProgress(final FileUploadId fileUploadId, final long bytesRead, final long contentLength) {
        this.fileUploadId = fileUploadId;
        this.contentLength = contentLength;
        this.bytesRead = bytesRead;
    }

    public FileUploadProgress(final FileUploadId fileUploadId, final String failureReason) {
        this.fileUploadId = fileUploadId;
        this.failureReason = failureReason;
    }

    public FileUploadId getFileUploadId() {
        return fileUploadId;
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

    public String getFilePath() {
        return filePath;
    }

    public void setMimeType(final String mimeType) {
        this.mimeType = mimeType;
    }

    public String getMimeType() {
        return mimeType;
    }
}
