/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.json.model.action;

import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtActionType;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A json annotated model for Action updates in RESTful API representation.
 */
@Data
@Accessors(chain = true)
@ToString
public class MgmtActionRequestBodyPut {

    @JsonProperty(value="forceType")
    private MgmtActionType actionType;
}