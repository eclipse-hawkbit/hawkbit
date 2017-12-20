/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.hawkbit.im.authentication.SpPermission.SpringEvalExpressions;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

/**
 * @author Michael Hirsch
 *
 */
@Features("Unit Tests - Security")
@Stories("Exclude path aware shallow ETag filter")
@RunWith(MockitoJUnitRunner.class)
public class ControllerPreAuthenticatedAnonymousDownloadTest {

    private ControllerPreAuthenticatedAnonymousDownload underTest;

    @Mock
    private TenantConfigurationManagement tenantConfigurationManagementMock;

    @Mock
    private TenantAware tenantAwareMock;

    @Before
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
