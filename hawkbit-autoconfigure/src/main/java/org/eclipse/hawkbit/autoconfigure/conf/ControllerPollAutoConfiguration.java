/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.autoconfigure.conf;

import org.eclipse.hawkbit.ControllerPollProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Enable the Controlle Poll.
 *
 *
 *
 */
@Configuration
@ConditionalOnClass(ControllerPollProperties.class)
@EnableConfigurationProperties(ControllerPollProperties.class)
public class ControllerPollAutoConfiguration {

}
