/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.autoconfigure.mgmt;

import org.eclipse.hawkbit.mgmt.rest.resource.MgmtApiConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Auto-Configuration for enabling the Management API REST-Resources.
 *
 */
@Configuration
@ConditionalOnClass(MgmtApiConfiguration.class)
@Import(MgmtApiConfiguration.class)
public class MgmtApiAutoConfiguration {

}
