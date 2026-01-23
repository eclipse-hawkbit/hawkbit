/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.event;

import org.eclipse.hawkbit.HawkbitAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * The {@link EventJacksonConfiguration} adds to the {@link tools.jackson.databind.json.JsonMapper} configuration
 * (already modified by the {@link HawkbitAutoConfiguration}) the event subtypes defined in {@link EventType}
 */
@Configuration
@Import(HawkbitAutoConfiguration.class)
public class EventJacksonConfiguration {

    @Bean
    JsonMapperBuilderCustomizer jsonMapperBuilderCustomizer() {
        return jsonMapperBuilder -> jsonMapperBuilder.registerSubtypes(EventType.getNamedTypes());
    }
}