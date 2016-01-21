/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.autoconfigure.conf;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * Autoconfiguration which loads the default configuration properties for
 * hawkbit.
 * 
 *
 *
 */
@Configuration
@PropertySource("classpath:/hawkbitdefaults.properties")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DefaultPropertiesAutoConfiguration {

}
