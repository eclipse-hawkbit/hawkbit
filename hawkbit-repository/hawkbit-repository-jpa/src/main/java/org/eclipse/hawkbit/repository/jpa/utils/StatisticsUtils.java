/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.eclipse.hawkbit.repository.jpa.Statistics;
import org.springframework.util.ObjectUtils;

/**
 * (Experimental) Utility class to get some statistics.
 * It's main purpose is to be used for debugging / testing (performance) purposes.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class StatisticsUtils {

    @SuppressWarnings("java:S5164") // intentionally don't call remove. should always keep the last reported
    private static final ThreadLocal<Map<String, Double>> LAST_COUNTERS = ThreadLocal.withInitial(MapUFToString::new);

    // for test purposes we may want to flush the statistics and to get diff from the last get int THIS thread
    public static Map<String, Double> diff() {
        final MeterRegistry meterRegistry = Statistics.getInstance().getMeterRegistry();
        if (meterRegistry == null) {
            // not a bean (i.e. no performance monitoring) is enabled or no meter registry available
            return Map.of();
        }

        final Map<String, Double> last = LAST_COUNTERS.get();
        final Map<String, Double> current = counters();
        return current.entrySet().stream()
                .filter(e -> e.getValue() != 0.0)
                .filter(e -> e.getValue().doubleValue() != last.getOrDefault(e.getKey(), 0.0).doubleValue())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue() - last.getOrDefault(e.getKey(), 0.0)));
    }

    // gets the jpa related counters
    public static Map<String, Double> counters() {
        final MeterRegistry meterRegistry = Statistics.getInstance().getMeterRegistry();
        if (meterRegistry == null) {
            // not a bean (i.e. no performance monitoring) is enabled or no meter registry available
            return Map.of();
        }

        Statistics.flush();

        final Map<String, Double> counters = new MapUFToString();
        meterRegistry.forEachMeter(m -> {
            final Meter.Id id = m.getId();
            if (id.getName().startsWith(Statistics.METER_PREFIX)) {
                final double value;
                if (m instanceof Counter counter) {
                    value = counter.count();
                } else if (m instanceof FunctionCounter functionCounter) {
                    value = functionCounter.count();
                } else {
                    return;
                }

                final StringBuilder key = new StringBuilder(id.getName());
                final List<Tag> tags = id.getTags();
                if (!ObjectUtils.isEmpty(tags)) {
                    key.append(" [");
                    tags.forEach(tag -> key.append(tag.getKey()).append('=').append(tag.getValue()).append(", "));
                    key.setLength(key.length() - 2); // remove the last ", "
                    key.append(']');
                }
                counters.put(key.toString(), value);
            }
        });

        LAST_COUNTERS.set(counters);
        return counters;
    }

    // Map with user-friendly toString, sorted and without the last ", "
    private static class MapUFToString extends HashMap<String, Double> {

        @Override
        public String toString() {
            if (isEmpty()) {
                return "{}";
            }

            final StringBuilder sb = new StringBuilder("{");
            entrySet().stream()
                    .sorted()
                    .forEach(e -> sb.append(e.getKey()).append(": ").append(e.getValue()).append(", "));
            sb.setLength(sb.length() - 2); // remove the last ", "
            sb.append("}");

            return sb.toString();
        }
    }
}