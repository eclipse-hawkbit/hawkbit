/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.autoconfigure.mgmt;

import org.eclipse.hawkbit.mgmt.rest.resource.MgmtApiConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Auto-Configuration for enabling the Management API REST-Resources.
 */
@Configuration
@ConditionalOnClass(MgmtApiConfiguration.class)
@Import(MgmtApiConfiguration.class)
public class MgmtApiAutoConfiguration {}