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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * JSON representation of download and update request.
 */
@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DmfDownloadAndUpdateRequest extends DmfActionRequest {

    private final String targetSecurityToken;
    private final List<DmfSoftwareModule> softwareModules;

    @JsonCreator
    public DmfDownloadAndUpdateRequest(
            @JsonProperty("actionId") final Long actionId,
            @JsonProperty("targetSecurityToken") final String targetSecurityToken,
            @JsonProperty("softwareModules") final List<DmfSoftwareModule> softwareModules) {
        super(actionId);
        this.targetSecurityToken = targetSecurityToken;
        this.softwareModules = softwareModules == null ? Collections.emptyList() : Collections.unmodifiableList(softwareModules);
    }
}