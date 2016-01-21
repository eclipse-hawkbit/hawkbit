/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.rest.resource.model.softwaremoduletype;

import org.eclipse.hawkbit.rest.resource.model.IdRest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.annotations.ApiModel;

/**
 * Request Body of SoftwareModuleType for assignment operations (ID only).
 *
 *
 *
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel("Software Module Type ID")
public class SoftwareModuleTypeAssigmentRest extends IdRest {

}
