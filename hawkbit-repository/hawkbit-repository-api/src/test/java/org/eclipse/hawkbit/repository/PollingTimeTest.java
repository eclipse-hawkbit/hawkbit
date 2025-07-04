/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.eclipse.hawkbit.tenancy.configuration.DurationHelper;
import org.eclipse.hawkbit.tenancy.configuration.PollingTime;
import org.junit.jupiter.api.Test;

class PollingTimeTest {

    @Test
    void testBackwardsCompatibility() {
        final PollingTime pollingTime = new PollingTime("01:00:00");
        assertThat(pollingTime.getPollingInterval().getInterval()).hasToString("PT1H");
        assertThat(pollingTime.getPollingInterval().getDeviationPercent()).isZero();
        assertThat(pollingTime.getOverrides()).isEmpty();
    }

    @Test
    void testDeviation() {
        final PollingTime pollingTime = new PollingTime("01:00:00~10%");
        final long maxDeviation = (Duration.ofHours(1).toMillis() / 10);
        final long deviation = Duration.ofHours(1).toMillis()
                - DurationHelper.formattedStringToDuration(pollingTime.getPollingInterval().getFormattedIntervalWithDeviation()).toMillis();
        assertThat(deviation)
                .isGreaterThanOrEqualTo(-maxDeviation)
                .isLessThanOrEqualTo(maxDeviation);
    }

    @Test
    void testComplexWithOverrides() {
        assertExpectedComplexWithOverrides("01:00:00~10%, group == 'eu' -> 00:02:00~15%, status != in_sync -> 00:05:00");
    }

    @Test
    void testComplexWithOverridesWithWhitespaces() {
        assertExpectedComplexWithOverrides("01:00:00~10%, group == 'eu'  -> 00:02:00~15%, status != in_sync ->00:05:00");
        assertExpectedComplexWithOverrides(" 01:00:00~10%, group == 'eu'  -> 00:02:00~15%, status != in_sync ->00:05:00  ");
        assertExpectedComplexWithOverrides(" 01:00:00~10% , group == 'eu'  -> 00:02:00 ~15%, status != in_sync ->00:05:00  ");
    }

    private static void assertExpectedComplexWithOverrides(final String pollingTimeStr) {
        final PollingTime pollingTime = new PollingTime(pollingTimeStr);
        assertThat(pollingTime.getPollingInterval().getInterval()).hasToString("PT1H");
        assertThat(pollingTime.getPollingInterval().getDeviationPercent()).isEqualTo(10);
        assertThat(pollingTime.getOverrides().get(0).qlStr()).isEqualTo("group == 'eu'");
        assertThat(pollingTime.getOverrides().get(0).pollingInterval().getInterval()).hasToString("PT2M");
        assertThat(pollingTime.getOverrides().get(0).pollingInterval().getDeviationPercent()).isEqualTo(15);
        assertThat(pollingTime.getOverrides().get(1).qlStr()).isEqualTo("status != in_sync");
        assertThat(pollingTime.getOverrides().get(1).pollingInterval().getInterval()).hasToString("PT5M");
        assertThat(pollingTime.getOverrides().get(1).pollingInterval().getDeviationPercent()).isZero();
    }
}