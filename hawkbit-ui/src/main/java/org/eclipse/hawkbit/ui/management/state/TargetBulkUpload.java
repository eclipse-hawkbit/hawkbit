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
 * 
 */
public class TargetBulkUpload implements Serializable {

    private static final long serialVersionUID = -7697723122460382323L;

    private Long dsNameAndVersion;

    private String description;

    private int sucessfulUploadCount;

    private int failedUploadCount;

    private float progressBarCurrentValue;

    private final List<String> assignedTagNames = new ArrayList<>();

    private final List<String> targetsCreated = new ArrayList<>();

    /**
     * @return the targetsCreated
     */
    public List<String> getTargetsCreated() {
        return targetsCreated;
    }

    /**
     * @return the assignedTagIds
     */
    public List<String> getAssignedTagNames() {
        return assignedTagNames;
    }

    /**
     * @return the dsNameAndVersion
     */
    public Long getDsNameAndVersion() {
        return dsNameAndVersion;
    }

    /**
     * @return the progressBarCurrentValue
     */
    public float getProgressBarCurrentValue() {
        return progressBarCurrentValue;
    }

    /**
     * @param progressBarCurrentValue
     *            the progressBarCurrentValue to set
     */
    public void setProgressBarCurrentValue(final float progressBarCurrentValue) {
        this.progressBarCurrentValue = progressBarCurrentValue;
    }

    /**
     * @param dsNameAndVersion
     *            the dsNameAndVersion to set
     */
    public void setDsNameAndVersion(final Long dsNameAndVersion) {
        this.dsNameAndVersion = dsNameAndVersion;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description
     *            the description to set
     */
    public void setDescription(final String description) {
        this.description = description;
    }

    /**
     * @return the sucessfulUploadCount
     */
    public int getSucessfulUploadCount() {
        return sucessfulUploadCount;
    }

    /**
     * @param sucessfulUploadCount
     *            the sucessfulUploadCount to set
     */
    public void setSucessfulUploadCount(final int sucessfulUploadCount) {
        this.sucessfulUploadCount = sucessfulUploadCount;
    }

    /**
     * @return the failedUploadCount
     */
    public int getFailedUploadCount() {
        return failedUploadCount;
    }

    /**
     * @param failedUploadCount
     *            the failedUploadCount to set
     */
    public void setFailedUploadCount(final int failedUploadCount) {
        this.failedUploadCount = failedUploadCount;
    }

}
