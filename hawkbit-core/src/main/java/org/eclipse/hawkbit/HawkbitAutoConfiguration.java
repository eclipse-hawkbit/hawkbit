/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit;

import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

/**
 * The {@link HawkbitAutoConfiguration} brings some default configurations needed for hawkbit. It does:
 * <ul>
 *     <li>Import {@link JacksonAutoConfiguration} and applies hawkbit-jackson-defaults.properties in order to configure the {@link tools.jackson.databind.json.JsonMapper}.</li>
 * </ul>
 */
@Configuration
@Import(JacksonAutoConfiguration.class)
@PropertySource("classpath:/hawkbit-jackson-defaults.properties")
public class HawkbitAutoConfiguration {}