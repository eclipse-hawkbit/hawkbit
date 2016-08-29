/**
 * Copyright (c) 2016 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.rsql;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.apache.commons.lang3.text.StrSubstitutor;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
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
public class VirtualPropertyMakroResolverTest {

    @Spy
    VirtualPropertyMakroResolver resolverUnderTest = new VirtualPropertyMakroResolver();

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
        when(resolverUnderTest.getTenantConfigurationManagement()).thenReturn(confMgmt);

        this.substitutor = new StrSubstitutor(resolverUnderTest, StrSubstitutor.DEFAULT_PREFIX,
                StrSubstitutor.DEFAULT_SUFFIX, StrSubstitutor.DEFAULT_ESCAPE);
     }

    @Test
    @Description("Tests resolution of NOW_TS by using a StrSubstitutor configured with the VirtualPropertyMakroResolver.")
    public void resolveNowTimestampPlaceholder() {
        String placeholder = "${NOW_TS}";
        String testString = "lhs=lt=" + placeholder;

        String resolvedPlaceholders = substitutor.replace(testString);
        assertFalse(resolvedPlaceholders.contains(placeholder));
    }

    @Test
    @Description("Tests resolution of OVERDUE_TS by using a StrSubstitutor configured with the VirtualPropertyMakroResolver.")
    public void resolveOverdueTimestampPlaceholder() {
        String placeholder = "${OVERDUE_TS}";
        String testString = "lhs=lt=" + placeholder;

        String resolvedPlaceholders = substitutor.replace(testString);
        assertFalse(resolvedPlaceholders.contains(placeholder));
    }

    @Test
    @Description("Tests case insensititity of VirtualPropertyMakroResolver.")
    public void resolveOverdueTimestampPlaceholderLowerCase() {
        String placeholder = "${overdue_ts}";
        String testString = "lhs=lt=" + placeholder;

        String resolvedPlaceholders = substitutor.replace(testString);
        assertFalse(resolvedPlaceholders.contains(placeholder));
    }

    @Test
    @Description("Tests VirtualPropertyMakroResolver with a placeholder unknown to VirtualPropertyMakroResolver.")
    public void handleUnknownPlaceholder() {
        String placeholder = "${unknown}";
        String testString = "lhs=lt=" + placeholder;

        String resolvedPlaceholders = substitutor.replace(testString);
        assertTrue(resolvedPlaceholders.contains(placeholder));
    }

    @Test
    @Description("Tests escape mechanism for placeholders (syntax is $${SOME_PLACEHOLDER}).")
    public void handleEscapedPlaceholder() {
        String placeholder = "${OVERDUE_TS}";
        String escaptedPlaceholder = StrSubstitutor.DEFAULT_ESCAPE + placeholder;
        String testString = "lhs=lt=" + escaptedPlaceholder;

        String resolvedPlaceholders = substitutor.replace(testString);
        assertTrue(resolvedPlaceholders.contains(placeholder));
    }
}
