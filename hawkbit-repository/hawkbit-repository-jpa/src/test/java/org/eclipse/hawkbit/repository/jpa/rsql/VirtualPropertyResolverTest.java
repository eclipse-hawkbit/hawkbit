/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.rsql;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import org.apache.commons.lang3.text.StrSubstitutor;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.TimestampCalculator;
import org.eclipse.hawkbit.repository.model.TenantConfigurationValue;
import org.eclipse.hawkbit.repository.rsql.VirtualPropertyResolver;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Unit Tests - Repository")
@Stories("Placeholder resolution for virtual properties")
@RunWith(PowerMockRunner.class)
@PrepareForTest(TimestampCalculator.class)
public class VirtualPropertyResolverTest {

    @Spy
    private final VirtualPropertyResolver resolverUnderTest = new VirtualPropertyResolver();

    @Mock
    private TenantConfigurationManagement confMgmt;

    private StrSubstitutor substitutor;

    private static final TenantConfigurationValue<String> TEST_POLLING_TIME_INTERVAL = TenantConfigurationValue
            .<String> builder().value("00:05:00").build();
    private static final TenantConfigurationValue<String> TEST_POLLING_OVERDUE_TIME_INTERVAL = TenantConfigurationValue
            .<String> builder().value("00:07:37").build();

    @Before
    public void before() {
        when(confMgmt.getConfigurationValue(TenantConfigurationKey.POLLING_TIME_INTERVAL, String.class))
                .thenReturn(TEST_POLLING_TIME_INTERVAL);
        when(confMgmt.getConfigurationValue(TenantConfigurationKey.POLLING_OVERDUE_TIME_INTERVAL, String.class))
                .thenReturn(TEST_POLLING_OVERDUE_TIME_INTERVAL);

        mockStatic(TimestampCalculator.class);
        when(TimestampCalculator.getTenantConfigurationManagement()).thenReturn(confMgmt);

        substitutor = new StrSubstitutor(resolverUnderTest, StrSubstitutor.DEFAULT_PREFIX,
                StrSubstitutor.DEFAULT_SUFFIX, StrSubstitutor.DEFAULT_ESCAPE);
    }

    @Test
    @Description("Tests resolution of NOW_TS by using a StrSubstitutor configured with the VirtualPropertyResolver.")
    public void resolveNowTimestampPlaceholder() {
        final String placeholder = "${NOW_TS}";
        final String testString = "lhs=lt=" + placeholder;

        final String resolvedPlaceholders = substitutor.replace(testString);
        assertThat("NOW_TS has to be resolved!", resolvedPlaceholders, not(containsString(placeholder)));
    }

    @Test
    @Description("Tests resolution of OVERDUE_TS by using a StrSubstitutor configured with the VirtualPropertyResolver.")
    public void resolveOverdueTimestampPlaceholder() {
        final String placeholder = "${OVERDUE_TS}";
        final String testString = "lhs=lt=" + placeholder;

        final String resolvedPlaceholders = substitutor.replace(testString);
        assertThat("OVERDUE_TS has to be resolved!", resolvedPlaceholders, not(containsString(placeholder)));
    }

    @Test
    @Description("Tests case insensititity of VirtualPropertyResolver.")
    public void resolveOverdueTimestampPlaceholderLowerCase() {
        final String placeholder = "${overdue_ts}";
        final String testString = "lhs=lt=" + placeholder;

        final String resolvedPlaceholders = substitutor.replace(testString);
        assertThat("overdue_ts has to be resolved!", resolvedPlaceholders, not(containsString(placeholder)));
    }

    @Test
    @Description("Tests VirtualPropertyResolver with a placeholder unknown to VirtualPropertyResolver.")
    public void handleUnknownPlaceholder() {
        final String placeholder = "${unknown}";
        final String testString = "lhs=lt=" + placeholder;

        final String resolvedPlaceholders = substitutor.replace(testString);
        assertThat("unknown should not be resolved!", resolvedPlaceholders, containsString(placeholder));
    }

    @Test
    @Description("Tests escape mechanism for placeholders (syntax is $${SOME_PLACEHOLDER}).")
    public void handleEscapedPlaceholder() {
        final String placeholder = "${OVERDUE_TS}";
        final String escaptedPlaceholder = StrSubstitutor.DEFAULT_ESCAPE + placeholder;
        final String testString = "lhs=lt=" + escaptedPlaceholder;

        final String resolvedPlaceholders = substitutor.replace(testString);
        assertThat("Escaped OVERDUE_TS should not be resolved!", resolvedPlaceholders, containsString(placeholder));
    }
}
