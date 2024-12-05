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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * JSON representation of batch download and update request.
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DmfBatchDownloadAndUpdateRequest {

    @Getter
    @Setter
    @JsonProperty
    private Long timestamp;

    @JsonProperty
    private List<DmfTarget> targets;

    @JsonProperty
    private List<DmfSoftwareModule> softwareModules;

    public List<DmfSoftwareModule> getSoftwareModules() {
        if (softwareModules == null) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableList(softwareModules);
    }

    /**
     * Add a Software module.
     *
     * @param createSoftwareModule the module
     */
    public void addSoftwareModule(final DmfSoftwareModule createSoftwareModule) {
        if (softwareModules == null) {
            softwareModules = new ArrayList<>();
        }

        softwareModules.add(createSoftwareModule);
    }

    public List<DmfTarget> getTargets() {
        if (targets == null) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableList(targets);
    }

    /**
     * Add a Target.
     *
     * @param target the target
     */
    public void addTarget(final DmfTarget target) {
        if (targets == null) {
            targets = new ArrayList<>();
        }

        targets.add(target);
    }

    /**
     * Add multiple Targets.
     *
     * @param targets the target
     */
    public void addTargets(final List<DmfTarget> targets) {
        if (this.targets == null) {
            this.targets = new ArrayList<>();
        }
        this.targets.addAll(targets);
    }
}