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
 * Artifact upload status.
 *
 *
 *
 *
 *
 */
public class UploadStatus implements Serializable {

    private static final long serialVersionUID = 8500552533390925782L;

    private String uploadResult;

    private String reason;

    private String fileName;

    private String baseSwModuleName;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getBaseSwModuleName() {
        return baseSwModuleName;
    }

    public void setBaseSwModuleName(String baseSwModuleName) {
        this.baseSwModuleName = baseSwModuleName;
    }

    public String getUploadResult() {
        return uploadResult;
    }

    public void setUploadResult(String uploadResult) {
        this.uploadResult = uploadResult;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

}
