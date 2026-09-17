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

import java.util.Optional;

import javax.sql.DataSource;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.throttle.Throttle;
import org.eclipse.hawkbit.throttle.ThrottleProperties;
import org.eclipse.hawkbit.throttle.ThrottleProperties.ThrottleConfig;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Wires the per-tenant DB connection throttle only if explicitly enabled.
 *
 * <p>Default configuration for every data source is read from {@code hawkbit.throttle.db.*}. Per-data-source overrides are read from
 * {@code hawkbit.throttle.db-<beanName>.*}. Overrides are self-contained override - i.e. doesn't inherit anything from default.
 *
 * <p>A {@link BeanPostProcessor} wraps the application {@link DataSource} in a {@link ThrottlingDataSourceDecorator}, mirroring the proven
 * {@code QueryCountConfiguration} pattern. Pool capacity is auto-detected from Hikari's {@code maximumPoolSize} (so granted permits never
 * exceed the real pool), or taken from the configuration {@code capacity} when set.
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(ThrottleProperties.class)
@SuppressWarnings("java:S1118") // false positive - auto config instantiated by Spring
public class ThrottleAutoConfiguration {

    private static final String DB_DOMAIN = "db";
    private static final int DEFAULT_CAPACITY = 10; // Hikari default; used only if auto-detect fails

    @Bean
    static BeanPostProcessor throttlingDataSourcePostProcessor(final ObjectProvider<ThrottleProperties> provider) {
        return new BeanPostProcessor() {

            @Override
            public Object postProcessAfterInitialization(final Object bean, final String beanName) {
                if (!(bean instanceof DataSource dataSource) || bean instanceof ThrottlingDataSourceDecorator) {
                    return bean;
                }

                final ThrottleConfig props = Optional.ofNullable(provider.getIfAvailable())
                        .map(properties -> Optional.ofNullable(properties.getThrottle().get(DB_DOMAIN + "-" + beanName))
                                .orElseGet(() -> properties.getThrottle().get(DB_DOMAIN)))
                        .orElse(null);
                if (props != null && props.isEnabled()) {
                    final Throttle.Policy policy = props.toPolicy(resolveCapacity(props, dataSource));
                    log.info(
                            "hawkBit connection throttle enabled (bean '{}'): capacity={}, limit={}, timeout={}, threshold={}, systemFloor={} slots",
                            beanName, policy.capacity(), props.getLimit(), props.getTimeout(),
                            props.getThreshold() == -1 ? " -1 (always enforce limit)" : " " + props.getThreshold() + " slots",
                            policy.priorityFloor(null));
                    return new ThrottlingDataSourceDecorator(dataSource, new Throttle(policy), props.getTimeout());
                } else {
                    return bean; // the throttle is not enabled
                }
            }
        };
    }

    private static int resolveCapacity(final ThrottleConfig props, final DataSource dataSource) {
        final int configured = props.getCapacity();
        if (configured > 0) {
            return configured;
        }
        // -1 or 0 → auto-detect
        final Integer poolSize = hikariMaximumPoolSize(dataSource);
        if (poolSize != null && poolSize > 0) {
            return poolSize;
        }
        log.warn("Could not auto-detect DB pool size for throttling; set hawkbit.throttle.{}.capacity. Falling back to {}",
                DB_DOMAIN, DEFAULT_CAPACITY);
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
