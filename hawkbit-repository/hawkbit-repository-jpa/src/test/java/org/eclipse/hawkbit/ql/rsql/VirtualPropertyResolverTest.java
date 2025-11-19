/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ql.rsql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.helper.TenantConfigHelper;
import org.eclipse.hawkbit.repository.model.TenantConfigurationValue;
import org.eclipse.hawkbit.repository.rsql.VirtualPropertyResolver;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Feature: Unit Tests - Repository<br/>
 * Story: Placeholder resolution for virtual properties
 */
@ExtendWith(SpringExtension.class)
class VirtualPropertyResolverTest {

    private static final TenantConfigurationValue<String> TEST_POLLING_TIME_INTERVAL =
            TenantConfigurationValue.<String> builder().value("00:05:00").build();
    private static final TenantConfigurationValue<String> TEST_POLLING_OVERDUE_TIME_INTERVAL =
            TenantConfigurationValue.<String> builder().value("00:07:37").build();

    @MockitoBean
    private TenantConfigurationManagement confMgmt;

    private final VirtualPropertyResolver substitutor = new VirtualPropertyResolver();

    @BeforeEach
    void before() {
        when(confMgmt.getConfigurationValue(TenantConfigurationKey.POLLING_TIME, String.class))
                .thenReturn(TEST_POLLING_TIME_INTERVAL);
        when(confMgmt.getConfigurationValue(TenantConfigurationKey.POLLING_OVERDUE_TIME, String.class))
                .thenReturn(TEST_POLLING_OVERDUE_TIME_INTERVAL);
    }

    /**
     * Tests VirtualPropertyResolver with a placeholder unknown to VirtualPropertyResolver.
     */
    @Test
    void handleUnknownPlaceholder() {
        final String placeholder = "${unknown}";
        final String testString = "lhs=lt=" + placeholder;

        final String resolvedPlaceholders = substitutor.replace(testString);
        assertThat(resolvedPlaceholders).as("unknown should not be resolved!").contains(placeholder);
    }

    /**
     * Tests escape mechanism for placeholders (syntax is $${SOME_PLACEHOLDER}).
     */
    @Test
    void handleEscapedPlaceholder() {
        final String placeholder = "${OVERDUE_TS}";
        final String escapedPlaceholder = "$" + placeholder;
        final String testString = "lhs=lt=" + escapedPlaceholder;

        final String resolvedPlaceholders = substitutor.replace(testString);
        assertThat(resolvedPlaceholders).as("Escaped OVERDUE_TS should not be resolved!").contains(placeholder);
    }

    /**
     * Tests resolution of NOW_TS by using a StringSubstitutor configured with the VirtualPropertyResolver.
     */
    @ParameterizedTest
    @ValueSource(strings = { "${NOW_TS}", "${OVERDUE_TS}", "${overdue_ts}" })
    void resolveNowTimestampPlaceholder(final String placeholder) {
        final String testString = "lhs=lt=" + placeholder;

        final String resolvedPlaceholders = substitutor.replace(testString);
        assertThat(resolvedPlaceholders).as("'%s' placeholder was not replaced", placeholder).doesNotContain(placeholder);
    }

    @Configuration
    static class Config {

        @Bean
        TenantConfigHelper tenantConfigHelper() {
            return TenantConfigHelper.getInstance();
        }
    }
}