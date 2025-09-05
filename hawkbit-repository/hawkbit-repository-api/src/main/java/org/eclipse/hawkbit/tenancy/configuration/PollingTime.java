/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.tenancy.configuration;

import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.Value;
import org.eclipse.hawkbit.repository.exception.TenantConfigurationValidatorException;

@Value
public class PollingTime {

    private static final Pattern OVERRIDE_PATTERN = Pattern.compile(
            "\\s{0,5},\\s{0,5}(?<qlStr>[^,]*)\\s{0,5}->\\s{0,5}(?<pollInterval>" + PollingInterval.POLLING_INTERVALE_REGEX + ")\\s{0,5}");

    PollingInterval pollingInterval;
    List<Override> overrides;

    public PollingTime(final String pollingTime) {
        final int indexOfComma = pollingTime.indexOf(',');
        if (indexOfComma == -1) { // no overrides
            pollingInterval = new PollingInterval(pollingTime);
            overrides = Collections.emptyList();
        } else {
            // Extract the main polling interval and overrides
            final String pollingIntervalStr = pollingTime.substring(0, indexOfComma);
            pollingInterval = new PollingInterval(pollingIntervalStr);
            overrides = new ArrayList<>();
            final String overridesStr = pollingTime.substring(indexOfComma).trim(); // with initial comma
            final Matcher overridesMatcher = OVERRIDE_PATTERN.matcher(overridesStr);
            for (int start = 0; start < overridesStr.length(); start = overridesMatcher.end()) {
                if (overridesMatcher.find(start)) {
                    overrides.add(new Override(
                            overridesMatcher.group("qlStr").trim(),
                            new PollingInterval(overridesMatcher.group("pollInterval").trim())));
                } else {
                    throw new TenantConfigurationValidatorException("Invalid pollingTime overrides: " + overridesStr);
                }
            }
        }
    }

    @Value
    public static class PollingInterval {

        @SuppressWarnings("java:S1068") // used for random delay only, no need of secure random
        private static final Random RANDOM = new Random();

        public static final String POLLING_INTERVALE_REGEX = "\\s{0,5}(?<pollingInterval>\\d{2}:[0-5]\\d:[0-5]\\d)\\s{0,5}(~(?<deviationPercent>\\d{1,2})%)?\\s{0,5}";
        private static final Pattern POLLING_INTERVAL_PATTERN = Pattern.compile(POLLING_INTERVALE_REGEX);

        Duration interval;
        int deviationPercent;

        public PollingInterval(final String pollingInterval) {
            final Matcher matcher = POLLING_INTERVAL_PATTERN.matcher(pollingInterval);
            if (matcher.matches()) {
                try {
                    this.interval = DurationHelper.fromString(matcher.group("pollingInterval"));
                } catch (final DateTimeParseException ex) {
                    throw new TenantConfigurationValidatorException(
                            "The given configuration value is expected as a string in the format HH:mm:ss(~\\d{1,2})?.");
                }
                this.deviationPercent = Optional.ofNullable(matcher.group("deviationPercent")).map(Integer::parseInt).orElse(0);
            } else {
                throw new TenantConfigurationValidatorException("Invalid pollingInterval: " + pollingInterval);
            }
        }

        public String getFormattedIntervalWithDeviation(final Duration minPollingTime, final Duration maxPollingTime) {
            if (deviationPercent > 0) {
                final long millis = interval.toMillis();
                final long maxDeviationMillis = (millis * deviationPercent) / 100;
                final long deviation = RANDOM.nextLong(-maxDeviationMillis, maxDeviationMillis + 1);
                if (deviation != 0) {
                    final Duration intervalWithDeviation = Duration.ofMillis(millis + deviation);
                    if (minPollingTime != null && intervalWithDeviation.compareTo(minPollingTime) < 0) {
                        return DurationHelper.toString(minPollingTime);
                    } else if (maxPollingTime != null && intervalWithDeviation.compareTo(maxPollingTime) > 0) {
                        return DurationHelper.toString(maxPollingTime);
                    } else {
                        return DurationHelper.toString(intervalWithDeviation);
                    }
                }
            }

            return DurationHelper.toString(interval);
        }
    }

    // This record holds the override information for a specific QL string and its associated polling interval.
    public record Override(String qlStr, PollingInterval pollingInterval) {}
}