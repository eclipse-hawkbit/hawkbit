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

import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.persistence.EntityManagerFactory;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.eclipse.persistence.sessions.Session;
import org.eclipse.persistence.tools.profiler.PerformanceMonitor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * (Experimental) Report EclipseLink statistics to Micrometer.
 * <p/>
 * To be enabled:
 * <ol>
 *     <li>The Spring property spring.jpa.properties.eclipselink.profiler=PerformanceMonitor shall be set - enables Eclipselink statistics
 *         collecting</li>
 *     <li>By default, the periodic stdout log is disabled by setting hawkbit.jpa.statistics.dump-period-ms=9223372036854775807 (Long.MAX_VALUE) -
 *         i.e. effectively <b>never</b>. If log is required it should be set to the required period</li>
 *     <li>The MeterRegistry shall be registered and available</li>
 *     <li>(?) When using in test the metrics MAYBE shall be enabled with @AutoConfigureObservability(tracing = false)</li>
 * </ol>
 *
 * It encapsulates reporting the Eclipselink {@link PerformanceMonitor} statistics to the {@link MeterRegistry} and the Spring autoconfiguration.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@SuppressWarnings("java:S6548") // java:S6548 - singleton holder ensures static access to spring resources in some places
public class Statistics {

    public static final String METER_PREFIX = "eclipselink.";

    private static final Statistics SINGLETON = new Statistics();

    private static final Pattern PATTERN = Pattern.compile("(?<type>(Counter|Timer)+):(?<key>[^ -]+)");
    private static final Map<String, Long> REPORTED_TIMER_VALUES = new HashMap<>();

    private EntityManagerFactory entityManagerFactory;
    // if meter registry is unavailable, the statistics will not send to metrics
    @Getter
    private MeterRegistry meterRegistry;

    private boolean flushing;

    /**
     * @return the singleton {@link Statistics} instance
     */
    public static Statistics getInstance() {
        return SINGLETON;
    }

    @Autowired
    public void setEntityManagerFactory(
            final EntityManagerFactory entityManagerFactory,
            @Value("${hawkbit.jpa.statistics.dump-period-ms:9223372036854775807}") final long dumpPeriod) {
        this.entityManagerFactory = entityManagerFactory;
        // set stdout log PerformanceMonitor. By default, it is Long.MAX_VALUE (9223372036854775807) which effectively disable logging
        getPerformanceMonitor(entityManagerFactory).setDumpTime(dumpPeriod);
    }

    @Autowired // spring setter injection
    public void setMeterRegistry(final MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    // flushes the statistics to the meter registry (if needed)
    public static void flush() {
        final MeterRegistry meterRegistry = SINGLETON.meterRegistry;
        if (meterRegistry == null) {
            // not a bean (i.e. no performance monitoring) is enabled or no meter registry available
            return;
        }

        synchronized (SINGLETON) {
            if (SINGLETON.flushing) {
                // wait for flushing
                do {
                    try {
                        SINGLETON.wait(1000);
                    } catch (final InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                } while (SINGLETON.flushing);
                // flushed
                return;
            }
            // flush
            SINGLETON.flushing = true;
        }
        SINGLETON.flush0();
    }

    @Scheduled(initialDelayString = "${hawkbit.jpa.statistics.flush.fixedDelay:60000}", fixedDelayString = "${hawkbit.jpa.statistics.flush.fixedDelay:60000}")
    void periodicFlush() {
        if (meterRegistry == null) {
            // meter registry available
            return;
        }

        synchronized (this) {
            if (flushing) {
                // no need to wait for flushing
                return;
            }
            // flush
            flushing = true;
        }
        flush0();
    }

    private void flush0() {
        final PerformanceMonitor performanceMonitor = getPerformanceMonitor(entityManagerFactory);
        final Map<String, Object> opTimings = performanceMonitor.getOperationTimings();
        opTimings.forEach((k, v) -> {
            if (opTimings.keySet().stream().anyMatch(key -> !key.equals(k) && key.startsWith(k))) {
                // it is a group, e.g.:
                //   Timer:ReportQuery	65,402,376
                //   Timer:ReportQuery:org.eclipse.hawkbit.repository.jpa.model.DistributionSetTypeElement:null:QueryPreparation	177,375
                //   Timer:ReportQuery:org.eclipse.hawkbit.repository.jpa.model.DistributionSetTypeElement:null:SqlGeneration	36,083
                //   Counter:ReportQuery	56
                //   Counter:ReportQuery:org.eclipse.hawkbit.repository.jpa.model.JpaTenantMetaData:null	56
                // we want to report per tag/operation, not the group - the sum could be made on the metric collector side (e.g. prometheus)
                return;
            }

            final Matcher matcher = PATTERN.matcher(k);
            if (matcher.matches()) {
                recordMetric(v, matcher);
            }
        });

        synchronized (this) {
            if (flushing) {
                flushing = false;
            }
            this.notifyAll();
        }
    }

    private void recordMetric(final Object v, final Matcher matcher) {
        final String type = matcher.group("type");
        final StringTokenizer stringTokenizer = new StringTokenizer(matcher.group("key"), ":");
        final String name = METER_PREFIX + stringTokenizer.nextToken();
        if (type.equals("Counter")) {
            final double quantity = v instanceof Double d ? d : Double.parseDouble(v.toString());
            final Counter counter;
            if (stringTokenizer.hasMoreTokens()) {
                counter = meterRegistry.counter(name, "entity", stringTokenizer.nextToken());
            } else {
                counter = meterRegistry.counter(name);
            }
            counter.increment(quantity - counter.count());
        } else { // Timer
            final long quantity = v instanceof Long l ? l : (long) Double.parseDouble(v.toString());
            final Timer timer;
            if (stringTokenizer.hasMoreTokens()) {
                final String entity = stringTokenizer.nextToken();
                stringTokenizer.nextToken(); // skip, what is this?
                final String subOp = stringTokenizer.hasMoreTokens() ? stringTokenizer.nextToken() : "n/a";
                timer = meterRegistry.timer(name, "entity", entity, "subOp", subOp);
            } else {
                timer = meterRegistry.timer(name);
            }
            timer.record(quantity - REPORTED_TIMER_VALUES.getOrDefault(name, 0L), TimeUnit.NANOSECONDS);
            REPORTED_TIMER_VALUES.put(name, quantity);
        }
    }

    private static PerformanceMonitor getPerformanceMonitor(final EntityManagerFactory entityManagerFactory) {
        return (PerformanceMonitor) entityManagerFactory.unwrap(Session.class).getProfiler();
    }

    // autoconfigure after CompositeMeterRegistryAutoConfiguration, so when the autoconfiguration is being processed the MeterRegistry
    // has already been registered / resolved (if it is to be registered at all) - otherwise @ConditionalOnBean(MeterRegistry.class) may not be
    // met event if the MeterRegistry is registered (if resolved later).
    // 'autoconfigure after' relies on this is being an AutoConfiguration
    @AutoConfiguration(afterName = "org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration")
    @Configuration
    public static class StatisticsAutoConfiguration {

        @ConditionalOnProperty(prefix = "spring.jpa.properties.eclipselink", name = "profiler", havingValue = "PerformanceMonitor")
        @ConditionalOnBean(MeterRegistry.class)
        @Bean
        public Statistics statistics() {
            // injects the singleton Statistics, and start scheduler
            return Statistics.getInstance();
        }
    }
}