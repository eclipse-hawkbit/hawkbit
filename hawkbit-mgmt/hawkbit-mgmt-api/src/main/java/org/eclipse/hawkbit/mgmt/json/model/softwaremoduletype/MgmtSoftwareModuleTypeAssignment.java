/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.json.model.softwaremoduletype;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.eclipse.hawkbit.mgmt.json.model.MgmtId;

/**
 * Request Body of SoftwareModuleType for assignment operations (ID only).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MgmtSoftwareModuleTypeAssignment extends MgmtId {}
