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
 * Holds file and upload progress details.
 */
public class FileUploadProgress implements Serializable {

    /**
     * Status of a file upload.
     */
    public enum FileUploadStatus {

        /**
         * An upload for a file has been started.
         */
        UPLOAD_STARTED,

        /**
         * Progress changed for one file upload.
         */
        UPLOAD_IN_PROGRESS,

        /**
         * Upload of one file failed.
         */
        UPLOAD_FAILED,

        /**
         * One file upload succeeded.
         */
        UPLOAD_SUCCESSFUL,

        /**
         * One file upload finished ()
         */
        UPLOAD_FINISHED
    }

    private static final long serialVersionUID = 1L;

    private final FileUploadId fileUploadId;

    private long contentLength;

    private long bytesRead;

    private String failureReason;

    private String filePath;

    private final FileUploadStatus fileUploadStatus;

    /**
     * Creates a new {@link FileUploadProgress} instance.
     * 
     * @param fileUploadId
     *            the {@link FileUploadId} to which this progress information
     *            belongs.
     * @param fileUploadStatus
     *            the {@link FileUploadStatus} of this progress
     */
    public FileUploadProgress(final FileUploadId fileUploadId, final FileUploadStatus fileUploadStatus) {
        this.fileUploadId = fileUploadId;
        this.fileUploadStatus = fileUploadStatus;
    }

    /**
     * Creates a new {@link FileUploadProgress} instance.
     * 
     * @param fileUploadId
     *            the {@link FileUploadId} to which this progress information
     *            belongs.
     * @param fileUploadStatus
     *            the {@link FileUploadStatus} of this progress
     * @param bytesRead
     *            number of bytes read
     * @param contentLength
     *            size of the file in bytes
     */
    FileUploadProgress(final FileUploadId fileUploadId, final FileUploadStatus fileUploadStatus, final long bytesRead,
            final long contentLength) {
        this.fileUploadId = fileUploadId;
        this.fileUploadStatus = fileUploadStatus;
        this.contentLength = contentLength;
        this.bytesRead = bytesRead;
    }

    /**
     * Creates a new {@link FileUploadProgress} instance.
     * 
     * @param fileUploadId
     *            the {@link FileUploadId} to which this progress information
     *            belongs.
     * @param fileUploadStatus
     *            the {@link FileUploadStatus} of this progress
     * @param bytesRead
     *            number of bytes read
     * @param contentLength
     *            size of the file in bytes
     * @param filePath
     *            the path of the file
     */
    FileUploadProgress(final FileUploadId fileUploadId, final FileUploadStatus fileUploadStatus, final long bytesRead,
            final long contentLength, final String filePath) {
        this.fileUploadId = fileUploadId;
        this.fileUploadStatus = fileUploadStatus;
        this.contentLength = contentLength;
        this.bytesRead = bytesRead;
        this.filePath = filePath;
    }

    /**
     * Creates a new {@link FileUploadProgress} instance.
     * 
     * @param fileUploadId
     *            the {@link FileUploadId} to which this progress information
     *            belongs.
     * @param fileUploadStatus
     *            the {@link FileUploadStatus} of this progress
     * @param failureReason
     *            the reason of the failed upload
     */
    FileUploadProgress(final FileUploadId fileUploadId, final FileUploadStatus fileUploadStatus,
            final String failureReason) {
        this.fileUploadId = fileUploadId;
        this.fileUploadStatus = fileUploadStatus;
        this.failureReason = failureReason;
    }

    /**
     * Getter for file upload ID
     *
     * @return FileUploadId
     */
    public FileUploadId getFileUploadId() {
        return fileUploadId;
    }

    /**
     * Getter for file content length
     *
     * @return long
     */
    public long getContentLength() {
        return contentLength;
    }

    /**
     * Getter for bytes read of the file
     *
     * @return long
     */
    public long getBytesRead() {
        return bytesRead;
    }

    /**
     * Getter for failed reason for upload
     * @return String
     */
    public String getFailureReason() {
        return failureReason;
    }

    /**
     * Getter for file path
     *
     * @return String
     */
    public String getFilePath() {
        return filePath;
    }

    /**
     * Get Status of a file upload
     *
     * @return FileUploadStatus
     */
    public FileUploadStatus getFileUploadStatus() {
        return fileUploadStatus;
    }
}
