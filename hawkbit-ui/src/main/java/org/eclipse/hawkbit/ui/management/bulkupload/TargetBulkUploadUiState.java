/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.bulkupload;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Target bulk upload ui state
 */
public class TargetBulkUploadUiState implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean isInProgress;

    private Long dsId;
    private final Map<Long, String> tagIdsWithNameToAssign = new HashMap<>();
    private String description;

    /**
     * @return true whe upload in progress else false
     */
    public boolean isInProgress() {
        return isInProgress;
    }

    /**
     * Sets the upload progress
     *
     * @param isInProgress
     *          boolean
     */
    public void setInProgress(final boolean isInProgress) {
        this.isInProgress = isInProgress;
    }

    /**
     * @return Distribution set id
     */
    public Long getDsId() {
        return dsId;
    }

    /**
     * Sets the distribution set id
     *
     * @param dsId
     *          Id
     */
    public void setDsId(final Long dsId) {
        this.dsId = dsId;
    }

    /**
     * @return Tag ids with name to assign
     */
    public Map<Long, String> getTagIdsWithNameToAssign() {
        return tagIdsWithNameToAssign;
    }

    /**
     * Sets the Tag ids with name
     *
     * @param tagIdsWithNameToAssign
     *          Tag ids with name
     */
    public void setTagIdsWithNameToAssign(final Map<Long, String> tagIdsWithNameToAssign) {
        this.tagIdsWithNameToAssign.clear();
        this.tagIdsWithNameToAssign.putAll(tagIdsWithNameToAssign);
    }

    /**
     * @return description of target bulk upload
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description of target bulk upload
     *
     * @param description
     *          Description
     */
    public void setDescription(final String description) {
        this.description = description;
    }
}
