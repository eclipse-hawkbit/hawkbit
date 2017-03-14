/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts.event;

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
        RECEIVE_UPLOAD, UPLOAD_FAILED, UPLOAD_IN_PROGRESS, UPLOAD_STARTED, UPLOAD_FINISHED, UPLOAD_SUCCESSFUL, UPLOAD_STREAMING_FAILED, UPLOAD_STREAMING_FINISHED, ABORT_UPLOAD
    }

    private final UploadStatusEventType uploadProgressEventType;

    private UploadFileStatus uploadStatus;

    /**
     * Constructor.
     * 
     * @param eventType
     *            the type of the event
     * @param uploadStatus
     *            the upload status of this event
     */
    public UploadStatusEvent(final UploadStatusEventType eventType, final UploadFileStatus uploadStatus) {
        this.uploadProgressEventType = eventType;
        this.uploadStatus = uploadStatus;
    }

    public UploadFileStatus getUploadStatus() {
        return uploadStatus;
    }

    public void setUploadStatus(final UploadFileStatus uploadStatus) {
        this.uploadStatus = uploadStatus;
    }

    public UploadStatusEventType getUploadProgressEventType() {
        return uploadProgressEventType;
    }

}
