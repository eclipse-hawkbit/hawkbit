/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
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

/**
 * JSON representation of download and update request.
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DmfDownloadAndUpdateRequest extends DmfActionRequest {

    @Getter
    @JsonProperty
    private String targetSecurityToken;

    @JsonProperty
    private List<DmfSoftwareModule> softwareModules;

    public void setTargetSecurityToken(final String targetSecurityToken) {
        this.targetSecurityToken = targetSecurityToken;
    }

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
}