/**
 * Copyright (c) 2021 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.json.model.distributionset;

import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * A json annotated rest model for invalidate DistributionSet requests.
 */
@Data
@Accessors(chain = true)
public class MgmtInvalidateDistributionSetRequestBody {

    @NotNull
    @Schema(description = "Type of cancelation for actions referring to the given distribution set")
    private MgmtCancelationType actionCancelationType;
}