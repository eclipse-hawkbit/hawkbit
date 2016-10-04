/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.rsql;

import static org.junit.Assert.*;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.apache.commons.lang3.text.StrSubstitutor;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.jpa.TimestampCalculator;
import org.eclipse.hawkbit.repository.model.TenantConfigurationValue;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationKey;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Unit Tests - Repository")
@Stories("Placeholder resolution for virtual properties")
@RunWith(MockitoJUnitRunner.class)
public class VirtualPropertyResolverTest {

    @Spy
    VirtualPropertyResolver resolverUnderTest = new VirtualPropertyResolver(new TimestampCalculator());

    @Mock
    TenantConfigurationManagement confMgmt;

    StrSubstitutor substitutor;

    Long nowTestTime;

    private static final TenantConfigurationValue<String> TEST_POLLING_TIME_INTERVAL = TenantConfigurationValue
            .<String> builder().value("00:05:00").build();
    private static final TenantConfigurationValue<String> TEST_POLLING_OVERDUE_TIME_INTERVAL = TenantConfigurationValue
            .<String> builder().value("00:07:37").build();

    @Before
    public void before() {
        nowTestTime = Instant.now().toEpochMilli();
        when(confMgmt.getConfigurationValue(TenantConfigurationKey.POLLING_TIME_INTERVAL, String.class))
            .thenReturn(TEST_POLLING_TIME_INTERVAL);
        when(confMgmt.getConfigurationValue(TenantConfigurationKey.POLLING_OVERDUE_TIME_INTERVAL, String.class))
            .thenReturn(TEST_POLLING_OVERDUE_TIME_INTERVAL);

        when(resolverUnderTest.getTimestampCalculator()).thenReturn(new TimestampCalculator() {
            @Override
            protected TenantConfigurationManagement getTenantConfigurationManagement() {
                return confMgmt;
            }
        });

        this.substitutor = new StrSubstitutor(resolverUnderTest,
                StrSubstitutor.DEFAULT_PREFIX,
                StrSubstitutor.DEFAULT_SUFFIX, StrSubstitutor.DEFAULT_ESCAPE);
     }

    @Test
    @Description("Tests resolution of NOW_TS by using a StrSubstitutor configured with the VirtualPropertyResolver.")
    public void resolveNowTimestampPlaceholder() {
        String placeholder = "${NOW_TS}";
        String testString = "lhs=lt=" + placeholder;

        String resolvedPlaceholders = substitutor.replace(testString);
        assertFalse("NOW_TS has to be resolved!", resolvedPlaceholders.contains(placeholder));
    }

    @Test
    @Description("Tests resolution of OVERDUE_TS by using a StrSubstitutor configured with the VirtualPropertyResolver.")
    public void resolveOverdueTimestampPlaceholder() {
        String placeholder = "${OVERDUE_TS}";
        String testString = "lhs=lt=" + placeholder;

        String resolvedPlaceholders = substitutor.replace(testString);
        assertFalse("OVERDUE_TS has to be resolved!", resolvedPlaceholders.contains(placeholder));
    }

    @Test
    @Description("Tests case insensititity of VirtualPropertyResolver.")
    public void resolveOverdueTimestampPlaceholderLowerCase() {
        String placeholder = "${overdue_ts}";
        String testString = "lhs=lt=" + placeholder;

        String resolvedPlaceholders = substitutor.replace(testString);
        assertFalse("overdue_ts has to be resolved!", resolvedPlaceholders.contains(placeholder));
    }

    @Test
    @Description("Tests VirtualPropertyResolver with a placeholder unknown to VirtualPropertyResolver.")
    public void handleUnknownPlaceholder() {
        String placeholder = "${unknown}";
        String testString = "lhs=lt=" + placeholder;

        String resolvedPlaceholders = substitutor.replace(testString);
        assertTrue("unknown should not be resolved!", resolvedPlaceholders.contains(placeholder));
    }

    @Test
    @Description("Tests escape mechanism for placeholders (syntax is $${SOME_PLACEHOLDER}).")
    public void handleEscapedPlaceholder() {
        String placeholder = "${OVERDUE_TS}";
        String escaptedPlaceholder = StrSubstitutor.DEFAULT_ESCAPE + placeholder;
        String testString = "lhs=lt=" + escaptedPlaceholder;

        String resolvedPlaceholders = substitutor.replace(testString);
        assertTrue("Escaped OVERDUE_TS should not be resolved!", resolvedPlaceholders.contains(placeholder));
    }
}
