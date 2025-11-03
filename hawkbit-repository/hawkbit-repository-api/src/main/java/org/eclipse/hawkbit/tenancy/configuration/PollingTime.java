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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.Value;
import org.eclipse.hawkbit.repository.exception.TenantConfigurationValidatorException;

@Value
public class PollingTime {

    PollingInterval pollingInterval;
    List<Override> overrides;

    @SuppressWarnings("java:S127")
    public PollingTime(final String pollingTime) {
        final int indexOfComma = pollingTime.indexOf(',');
        if (indexOfComma == -1) { // no overrides
            pollingInterval = new PollingInterval(pollingTime);
            overrides = List.of();
        } else {
            // Extract the main polling interval and overrides
            final String pollingIntervalStr = pollingTime.substring(0, indexOfComma);
            pollingInterval = new PollingInterval(pollingIntervalStr);
            overrides = new ArrayList<>();
            final String overridesStr = pollingTime.substring(indexOfComma + 1).trim();
            for (int start = 0; ; ) {
                final int separatorIndex = overridesStr.indexOf("->", start);
                if (separatorIndex == -1) {
                    throw new TenantConfigurationValidatorException("Invalid pollingTime override: '" + overridesStr.substring(start) + "'");
                } else {
                    final String ql = overridesStr.substring(start, separatorIndex).trim();
                    final int nextCommaIndex = overridesStr.indexOf(',', separatorIndex);
                    if (nextCommaIndex == -1) { // last override
                        overrides.add(new Override(ql, new PollingInterval(overridesStr.substring(separatorIndex + 2).trim())));
                        break;
                    } else {
                        overrides.add(new Override(ql, new PollingInterval(overridesStr.substring(separatorIndex + 2, nextCommaIndex).trim())));
                        start = nextCommaIndex + 1;
                    }
                }
            }
        }
    }

    @Value
    public static class PollingInterval {

        @SuppressWarnings("java:S1068") // used for random delay only, no need of secure random
        private static final Random RANDOM = new Random();

        private static final String POLLING_INTERVALE_REGEX = "(?<pollingInterval>[^~]+)(~(?<deviationPercent>\\d{1,2})%)?\\s{0,5}";
        private static final Pattern POLLING_INTERVAL_PATTERN = Pattern.compile(POLLING_INTERVALE_REGEX);

        Duration interval;
        int deviationPercent;

        public PollingInterval(final String pollingInterval) {
            final Matcher matcher = POLLING_INTERVAL_PATTERN.matcher(pollingInterval);
            try {
                if (matcher.matches()) {
                    interval = DurationHelper.fromString(matcher.group("pollingInterval").trim());
                    deviationPercent = Optional.ofNullable(matcher.group("deviationPercent"))
                            .map(String::trim).map(Integer::parseInt).orElse(0);
                } else {
                    throw new IllegalArgumentException();
                }
            } catch (final Exception e) {
                throw new TenantConfigurationValidatorException(
                        "Invalid pollingInterval: '" + pollingInterval + "', expecting: (HH:mm:ss|ISO-8601)(~\\d{1,2}%)?");
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