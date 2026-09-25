/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.autoconfigure.throttle;

import javax.sql.DataSource;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.throttle.Config;
import org.eclipse.hawkbit.throttle.Throttle;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Wires the DB connection throttle only if explicitly enabled.
 *
 * <p>A {@link BeanPostProcessor} wraps the application {@link DataSource} in a {@link ThrottlingDataSourceDecorator}, mirroring the proven
 * {@code QueryCountConfiguration} pattern. Pool capacity is auto-detected from Hikari's {@code maximumPoolSize} (so granted permits never
 * exceed the real pool), or taken from the configuration {@code capacity} when set.
 */
@Slf4j
@AutoConfiguration
@ConditionalOnProperty(prefix = "hawkbit.throttle", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(ThrottleProperties.class)
@SuppressWarnings("java:S1118") // false positive - auto config instantiated by Spring
public class ThrottleAutoConfiguration {

    private static final int DEFAULT_CAPACITY = 10; // Hikari default; used only if auto-detect fails

    @Bean
    static BeanPostProcessor throttlingDataSourcePostProcessor(
            final ObjectProvider<ThrottleProperties> provider, final ObjectProvider<MeterRegistry> meterRegistryProvider) {
        return new BeanPostProcessor() {

            @Override
            public Object postProcessAfterInitialization(@NonNull final Object bean, @NonNull final String beanName) {
                if (!(bean instanceof DataSource dataSource) || bean instanceof ThrottlingDataSourceDecorator) {
                    return bean;
                }

                final ThrottleProperties props = provider.getIfAvailable();
                if (props == null) { // should never happen, but just in case
                    log.warn("hawkBit throttle enabled (for bean '{}'): but no ThrottleProperties available", beanName);
                    return bean;
                }

                final Config config = props.toConfig(resolveCapacity(props.getCapacity(), dataSource));
                log.info("hawkBit connection throttle enabled (bean '{}'): props: {}, config: {}", beanName, props, config);
                return new ThrottlingDataSourceDecorator(
                        dataSource, new Throttle(config), props.getTimeout(),
                        () -> meterRegistry(meterRegistryProvider));
            }
        };
    }

    // the data source is decorated long before the metrics infrastructure is up, so the decorator asks for the
    // registry lazily. A failure here means the context is not far enough along to have one - report no metrics
    // rather than let a monitoring concern break a connection acquisition
    private static MeterRegistry meterRegistry(final ObjectProvider<MeterRegistry> meterRegistryProvider) {
        try {
            return meterRegistryProvider.getIfAvailable();
        } catch (final BeansException e) {
            log.debug("MeterRegistry not (yet) resolvable, throttle metrics are skipped", e);
            return null;
        }
    }

    private static int resolveCapacity(final int configuredCapacity, final DataSource dataSource) {
        if (configuredCapacity >= 0) {
            return configuredCapacity;
        }
        // < 0 → auto-detect
        final Integer poolSize = hikariMaximumPoolSize(dataSource);
        if (poolSize != null && poolSize > 0) {
            return poolSize;
        }
        log.warn("Could not auto-detect DB pool size for throttling; set hawkbit.throttle.capacity. Falling back to {}", DEFAULT_CAPACITY);
        return DEFAULT_CAPACITY;
    }

    // reflective read so hawkbit-autoconfigure need not compile-depend on HikariCP
    private static Integer hikariMaximumPoolSize(final DataSource dataSource) {
        try {
            final Object value = dataSource.getClass().getMethod("getMaximumPoolSize").invoke(dataSource);
            return value instanceof Integer size ? size : null;
        } catch (final ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }
}
