/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.rsql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.concurrent.Callable;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.model.TenantConfigurationValue;
import org.eclipse.hawkbit.repository.model.helper.SystemSecurityContextHolder;
import org.eclipse.hawkbit.repository.model.helper.TenantConfigurationManagementHolder;
import org.eclipse.hawkbit.repository.rsql.VirtualPropertyResolver;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@Feature("Unit Tests - Repository")
@Story("Placeholder resolution for virtual properties")
class VirtualPropertyResolverTest {

    private static final TenantConfigurationValue<String> TEST_POLLING_TIME_INTERVAL =
            TenantConfigurationValue.<String> builder().value("00:05:00").build();
    private static final TenantConfigurationValue<String> TEST_POLLING_OVERDUE_TIME_INTERVAL =
            TenantConfigurationValue.<String> builder().value("00:07:37").build();

    @Spy
    private final VirtualPropertyResolver resolverUnderTest = new VirtualPropertyResolver();
    @MockitoBean
    private TenantConfigurationManagement confMgmt;
    @MockitoBean
    private SystemSecurityContext securityContext;
    private StrSubstitutor substitutor;

    @BeforeEach
    void before() {
        when(confMgmt.getConfigurationValue(TenantConfigurationKey.POLLING_TIME_INTERVAL, String.class))
                .thenReturn(TEST_POLLING_TIME_INTERVAL);
        when(confMgmt.getConfigurationValue(TenantConfigurationKey.POLLING_OVERDUE_TIME_INTERVAL, String.class))
                .thenReturn(TEST_POLLING_OVERDUE_TIME_INTERVAL);

        substitutor = new StrSubstitutor(resolverUnderTest, StrSubstitutor.DEFAULT_PREFIX,
                StrSubstitutor.DEFAULT_SUFFIX, StrSubstitutor.DEFAULT_ESCAPE);
    }

    @Test
    @Description("Tests VirtualPropertyResolver with a placeholder unknown to VirtualPropertyResolver.")
    void handleUnknownPlaceholder() {
        final String placeholder = "${unknown}";
        final String testString = "lhs=lt=" + placeholder;

        final String resolvedPlaceholders = substitutor.replace(testString);
        assertThat(resolvedPlaceholders).as("unknown should not be resolved!").contains(placeholder);
    }

    @Test
    @Description("Tests escape mechanism for placeholders (syntax is $${SOME_PLACEHOLDER}).")
    void handleEscapedPlaceholder() {
        final String placeholder = "${OVERDUE_TS}";
        final String escaptedPlaceholder = StrSubstitutor.DEFAULT_ESCAPE + placeholder;
        final String testString = "lhs=lt=" + escaptedPlaceholder;

        final String resolvedPlaceholders = substitutor.replace(testString);
        assertThat(resolvedPlaceholders).as("Escaped OVERDUE_TS should not be resolved!").contains(placeholder);
    }

    @ParameterizedTest
    @ValueSource(strings = { "${NOW_TS}", "${OVERDUE_TS}", "${overdue_ts}" })
    @Description("Tests resolution of NOW_TS by using a StrSubstitutor configured with the VirtualPropertyResolver.")
    void resolveNowTimestampPlaceholder(final String placeholder) {
        when(securityContext.runAsSystem(Mockito.any())).thenAnswer(a -> ((Callable<?>) a.getArgument(0)).call());
        final String testString = "lhs=lt=" + placeholder;

        final String resolvedPlaceholders = substitutor.replace(testString);
        assertThat(resolvedPlaceholders).as("'%s' placeholder was not replaced", placeholder)
                .doesNotContain(placeholder);
    }

    @Configuration
    static class Config {

        @Bean
        TenantConfigurationManagementHolder tenantConfigurationManagementHolder() {
            return TenantConfigurationManagementHolder.getInstance();
        }

        @Bean
        SystemSecurityContextHolder systemSecurityContextHolder() {
            return SystemSecurityContextHolder.getInstance();
        }
    }
}
