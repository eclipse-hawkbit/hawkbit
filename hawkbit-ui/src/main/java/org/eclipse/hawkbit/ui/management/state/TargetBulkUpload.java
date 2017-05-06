/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.state;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Hold details for target bulk upload window.
 */
public class TargetBulkUpload implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long dsNameAndVersion;

    private String description;

    private int sucessfulUploadCount;

    private int failedUploadCount;

    private float progressBarCurrentValue;

    private final List<String> assignedTagNames = new ArrayList<>();

    private final List<String> targetsCreated = new ArrayList<>();

    public List<String> getTargetsCreated() {
        return targetsCreated;
    }

    public List<String> getAssignedTagNames() {
        return assignedTagNames;
    }

    public Long getDsNameAndVersion() {
        return dsNameAndVersion;
    }

    public float getProgressBarCurrentValue() {
        return progressBarCurrentValue;
    }

    public void setProgressBarCurrentValue(final float progressBarCurrentValue) {
        this.progressBarCurrentValue = progressBarCurrentValue;
    }

    public void setDsNameAndVersion(final Long dsNameAndVersion) {
        this.dsNameAndVersion = dsNameAndVersion;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public int getSucessfulUploadCount() {
        return sucessfulUploadCount;
    }

    public void setSucessfulUploadCount(final int sucessfulUploadCount) {
        this.sucessfulUploadCount = sucessfulUploadCount;
    }

    public int getFailedUploadCount() {
        return failedUploadCount;
    }

    public void setFailedUploadCount(final int failedUploadCount) {
        this.failedUploadCount = failedUploadCount;
    }

}
