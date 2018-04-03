/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts.event;

import org.eclipse.hawkbit.ui.artifacts.upload.FileUploadProgress;

/**
 * 
 * Holds the upload file status.
 *
 */
public class UploadStatusEvent {

    /**
     * TenantAwareEvent type definition of events during the artifact upload
     * life-cycle from receiving the upload until the process end.
     */
    public enum UploadStatusEventType {

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
        UPLOAD_FINISHED,

        /**
         * Upload of one file was aborted by the user.
         */
        UPLOAD_ABORTED_BY_USER
    }

    private final UploadStatusEventType uploadStatusEventType;

    private FileUploadProgress fileUploadProgress;

    /**
     * Constructor.
     * 
     * @param eventType
     *            the type of the event
     * @param fileUploadProgress
     *            the upload status of this event
     */
    public UploadStatusEvent(final UploadStatusEventType eventType, final FileUploadProgress fileUploadProgress) {
        this.uploadStatusEventType = eventType;
        this.fileUploadProgress = fileUploadProgress;
    }

    public FileUploadProgress getFileUploadProgress() {
        return fileUploadProgress;
    }

    public void setFileUploadProgress(final FileUploadProgress uploadStatus) {
        this.fileUploadProgress = uploadStatus;
    }

    public UploadStatusEventType getUploadStatusEventType() {
        return uploadStatusEventType;
    }

}
