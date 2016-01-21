/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.autoconfigure.cache;

import org.eclipse.hawkbit.cache.RedisConfiguration;
import org.eclipse.hawkbit.cache.annotation.EnableRedis;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;

/**
 * A configuration for configuring the redis configuration.
 *
 *
 *
 */
@Configuration
@ConditionalOnClass(value = RedisConfiguration.class)
@EnableRedis
public class RedisAutoConfiguration {

}
