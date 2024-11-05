/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.security;

import static org.assertj.core.api.Assertions.assertThat;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.eclipse.hawkbit.im.authentication.SpPermission.SpringEvalExpressions;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 *
 */
@Feature("Unit Tests - Security")
@Story("Exclude path aware shallow ETag filter")
@ExtendWith(MockitoExtension.class)
public class ControllerPreAuthenticatedAnonymousDownloadTest {

    private ControllerPreAuthenticatedAnonymousDownload underTest;

    @Mock
    private TenantConfigurationManagement tenantConfigurationManagementMock;

    @Mock
    private TenantAware tenantAwareMock;

    @BeforeEach
    public void before() {
        underTest = new ControllerPreAuthenticatedAnonymousDownload(tenantConfigurationManagementMock, tenantAwareMock,
                new SystemSecurityContext(tenantAwareMock));
    }

    @Test
    public void useCorrectTenantConfiguationKey() {
        assertThat(underTest.getTenantConfigurationKey()).as("Should be using the correct tenant configuration key")
                .isEqualTo(underTest.getTenantConfigurationKey());
    }

    @Test
    public void successfulAuthenticationAdditionalAuthoritiesForDownload() {
        assertThat(underTest.getSuccessfulAuthenticationAuthorities())
                .as("Additional authorities should be containing the download anonymous role")
                .contains(new SimpleGrantedAuthority(SpringEvalExpressions.CONTROLLER_ROLE));
    }
}
