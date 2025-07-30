/**
 * Copyright (c) 2022 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.dmf.json.model;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * JSON representation of batch download and update request.
 */
@Data
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DmfBatchDownloadAndUpdateRequest {

    private final Long timestamp;
    private final List<DmfTarget> targets;
    private final List<DmfSoftwareModule> softwareModules;

    public  DmfBatchDownloadAndUpdateRequest(
            @JsonProperty("timestamp") final Long timestamp,
            @JsonProperty("targets") final List<DmfTarget> targets,
            @JsonProperty("softwareModules") final List<DmfSoftwareModule> softwareModules) {
        this.timestamp = timestamp;
        this.targets = targets == null ? Collections.emptyList() : targets;
        this.softwareModules = softwareModules == null ? Collections.emptyList() : Collections.unmodifiableList(softwareModules);
    }
}