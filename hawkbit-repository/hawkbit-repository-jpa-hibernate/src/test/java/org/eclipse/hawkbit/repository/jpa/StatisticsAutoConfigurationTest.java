/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.micrometer.metrics.autoconfigure.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.micrometer.metrics.autoconfigure.MetricsAutoConfiguration;
import org.springframework.boot.micrometer.metrics.autoconfigure.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * Tests for {@link Statistics.StatisticsAutoConfiguration}.
 * <p/>
 * Feature: Unit Tests - Repository<br/>
 * Story: Statistics auto-configuration
 */
class StatisticsAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    Statistics.StatisticsAutoConfiguration.class, MetricsAutoConfiguration.class,
                    SimpleMetricsExportAutoConfiguration.class, CompositeMeterRegistryAutoConfiguration.class))
            .withPropertyValues("spring.jpa.properties.hibernate.generate_statistics=true");

    @Test
    void statisticsAreCreatedWithTheMeterRegistryOfSpringBoot() {
        contextRunner.run(context -> assertThat(context).hasSingleBean(Statistics.class));
    }
}
