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

/**
 * 
 * Holds file and upload status details.Meta data sent with upload events.
 *
 */
public class UploadFileStatus implements Serializable {

    private static final long serialVersionUID = -3599629192216760811L;

    private String fileName;

    private long contentLength;

    private long bytesRead;

    private String failureReason;

    public UploadFileStatus(String fileName) {
        this.fileName = fileName;
    }

    public UploadFileStatus(String fileName, long bytesRead, long contentLength) {
        this.fileName = fileName;
        this.contentLength = contentLength;
        this.bytesRead = bytesRead;
    }

    public UploadFileStatus(String fileName, String failureReason) {
        this.failureReason = failureReason;
        this.fileName = fileName;
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
}
