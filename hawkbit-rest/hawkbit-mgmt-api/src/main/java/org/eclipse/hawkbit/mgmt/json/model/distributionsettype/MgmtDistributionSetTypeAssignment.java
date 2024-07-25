/**
 * Copyright (c) 2021 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.json.model.distributionsettype;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.experimental.Accessors;
import org.eclipse.hawkbit.mgmt.json.model.MgmtId;

/**
 * Request Body of DistributionSetType for assignment operations (ID only).
 */
@Accessors(chain = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MgmtDistributionSetTypeAssignment extends MgmtId {
}
