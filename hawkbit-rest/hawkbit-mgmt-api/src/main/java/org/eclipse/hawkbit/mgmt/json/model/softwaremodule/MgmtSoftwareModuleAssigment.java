/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.json.model.softwaremodule;

import org.eclipse.hawkbit.mgmt.json.model.MgmtId;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Request Body of SoftwareModule for assignment operations (ID only).
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MgmtSoftwareModuleAssigment extends MgmtId {

}
