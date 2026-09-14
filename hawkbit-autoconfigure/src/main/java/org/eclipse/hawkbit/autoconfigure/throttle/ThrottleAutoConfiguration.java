/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.autoconfigure.throttle;

import javax.sql.DataSource;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.throttle.TenantThrottle;
import org.eclipse.hawkbit.throttle.ThrottleProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Wires the per-tenant DB connection throttle . Active only when
 * {@code hawkbit.throttle.enabled=true}; otherwise this auto-configuration is skipped entirely, the
 * DataSource is left un-wrapped and behavior is byte-for-byte the current one.
 * <p/>
 * A {@link BeanPostProcessor} wraps the application {@link DataSource} in a
 * {@link ThrottlingDataSourceDecorator}, mirroring the proven {@code QueryCountConfiguration} pattern.
 * Pool capacity is auto-detected from Hikari's {@code maximumPoolSize} (so granted permits never
 * exceed the real pool), or taken from {@code hawkbit.throttle.capacity} when set.
 */
@Slf4j
@AutoConfiguration
@ConditionalOnProperty(prefix = "hawkbit.throttle", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(ThrottleProperties.class)
public class ThrottleAutoConfiguration {

    private static final int DEFAULT_CAPACITY = 10; // Hikari default; used only if auto-detect fails

    @Bean
    static BeanPostProcessor throttlingDataSourcePostProcessor(final ObjectProvider<ThrottleProperties> properties) {
        return new BeanPostProcessor() {

            @Override
            public Object postProcessAfterInitialization(final Object bean, final String beanName) {
                if (!(bean instanceof DataSource dataSource) || bean instanceof ThrottlingDataSourceDecorator) {
                    return bean;
                }
                final ThrottleProperties props = properties.getObject();
                validateVirtualThreadsWhenWaitMode(props);
                final int capacity = resolveCapacity(props, dataSource);
                final int threshold = props.getThreshold();
                final TenantThrottle throttle = new TenantThrottle(capacity, threshold, props::limit);
                log.info("hawkBit per-tenant connection throttle enabled (bean '{}'): capacity={}, limit={}, timeout={}, threshold={}",
                        beanName, capacity, props.getLimit(), props.getTimeout(),
                        threshold == -1 ? " -1 (always enforce limit)" : " " + threshold + " slots");
                return new ThrottlingDataSourceDecorator(dataSource, throttle, props.getTimeout());
            }
        };
    }

    private static void validateVirtualThreadsWhenWaitMode(final ThrottleProperties props) {
        if (props.getTimeout().isZero() || props.getTimeout().isNegative()) {
            return; // fast-reject mode (timeout=0) is safe on platform threads
        }
        // timeout > 0 requires virtual threads to avoid platform thread exhaustion
        try {
            final Thread vt = Thread.ofVirtual().unstarted(() -> {});
            if (!vt.isVirtual()) {
                throw new IllegalStateException(
                        "hawkbit.throttle.timeout > 0 requires virtual threads (spring.threads.virtual.enabled=true), " + "otherwise platform thread pool will be exhausted by blocked waiters. " + "Either set timeout=0 (fast-reject) or enable virtual threads.");
            }
        } catch (final UnsupportedOperationException e) {
            throw new IllegalStateException(
                    "hawkbit.throttle.timeout > 0 requires virtual threads (spring.threads.virtual.enabled=true), " + "but this JVM does not support virtual threads. Either set timeout=0 or upgrade to JDK 21+.",
                    e);
        }
    }

    private static int resolveCapacity(final ThrottleProperties props, final DataSource dataSource) {
        final int configured = props.getCapacity();
        if (configured > 0) {
            return configured;
        }
        // -1 or 0 → auto-detect
        final Integer poolSize = hikariMaximumPoolSize(dataSource);
        if (poolSize != null && poolSize > 0) {
            return poolSize;
        }
        log.warn("Could not auto-detect DB pool size for throttling; set hawkbit.throttle.capacity. Falling back to {}",
                DEFAULT_CAPACITY);
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
