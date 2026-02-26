/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * (Experimental) Report Hibernate statistics to Micrometer.
 * <p/>
 * To be enabled:
 * <ol>
 *     <li>The Spring property spring.jpa.properties.hibernate.generate_statistics=true shall be set - enables Hibernate statistics
 *         collecting</li>
 *     <li>If you don't need periodic log (Slf4J) set logging.level.org.hibernate.engine.internal.StatisticalLoggingSessionEventListener=WARN</li>
 *     <li>The MeterRegistry shall be registered and available</li>
 *     <li>Hibernate reporting to micrometer shall be enabled - include org.hibernate.orm:hibernate-micrometer</li>
 *     <li>(?) When using in test the metrics MAYBE shall be enabled with @AutoConfigureObservability(tracing = false)</li>
 * </ol>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@SuppressWarnings("java:S6548") // java:S6548 - singleton holder ensures static access to spring resources in some places
public class Statistics {

    public static final String METER_PREFIX = "hibernate.";

    private static final Statistics SINGLETON = new Statistics();

    // if meter registry is unavailable, the statistics will not send to metrics
    @Getter
    public MeterRegistry meterRegistry;

    /**
     * @return the singleton {@link Statistics} instance
     */
    public static Statistics getInstance() {
        return SINGLETON;
    }

    // autoconfigure after CompositeMeterRegistryAutoConfiguration, so when the autoconfiguration is being processed the MeterRegistry
    // has already been registered / resolved (if it is to be registered at all) - otherwise @ConditionalOnBean(MeterRegistry.class) may not be
    // met event if the MeterRegistry is registered (if resolved later).
    // 'autoconfigure after' relies on this is being an AutoConfiguration
    @AutoConfiguration(afterName = "org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration")
    @Configuration
    public static class StatisticsAutoConfiguration {

        @ConditionalOnProperty(prefix = "spring.jpa.properties.hibernate", name = "generate_statistics", havingValue = "true")
        @ConditionalOnBean(MeterRegistry.class)
        @Bean
        public Statistics statistics() {
            // injects the singleton Statistics, and start scheduler
            return Statistics.getInstance();
        }
    }

    @Autowired // spring setter injection
    public void setMeterRegistry(final MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    // flushes the statistics to the meter registry (if needed)
    public static void flush() {
        // nothing to do for Hibernate
    }
}