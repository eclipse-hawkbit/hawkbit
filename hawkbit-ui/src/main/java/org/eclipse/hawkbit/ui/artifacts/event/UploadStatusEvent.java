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

    public enum UploadStatusEventType {
        UPLOAD_FAILED, UPLOAD_IN_PROGRESS, UPLOAD_STARTED, UPLOAD_FINISHED, UPLOAD_SUCCESSFUL, UPLOAD_STREAMING_FAILED, UPLOAD_STREAMING_FINISHED
    }

    private UploadStatusEventType uploadProgressEventType;

    private UploadFileStatus uploadStatus;

    public UploadStatusEvent(UploadStatusEventType eventType, UploadFileStatus entity) {
        this.uploadProgressEventType = eventType;
        this.uploadStatus = entity;
    }

    public UploadFileStatus getUploadStatus() {
        return uploadStatus;
    }

    public void setUploadStatus(UploadFileStatus uploadStatus) {
        this.uploadStatus = uploadStatus;
    }

    public UploadStatusEventType getUploadProgressEventType() {
        return uploadProgressEventType;
    }

}
