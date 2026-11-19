/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Feature: Unit Tests - Security<br/>
 * Story: Requested tenant accessor
 */
class RequestedTenantTest {

    private static final String VALID_TENANT = "VALID-TENANT-000";
    private static final String INVALID_TENANT = "INVALID-TENANT-005\uFEFF";

    @AfterEach
    void cleanup() {
        RequestContextHolder.resetRequestAttributes();
    }

    /**
     * Validates the tenant format.
     */
    @Test
    void validateTenant() {
        List.of(VALID_TENANT, "VALIDTENANT001", "_VALID-TENANT_002", "_valid_-tenant003_", "ValidTenant004", "acme.com", "-VALID--TENANT~005-",
                        "...")
                .forEach(tenant -> assertThat(RequestedTenant.isValidTenant(tenant)).as(tenant).isTrue());
        List.of(INVALID_TENANT, "INVALID-TENANT-008 ", "INVALID TENANT009", "INVALID/TENANT010", "INVALID%41TENANT011", "INVALID;TENANT012",
                        ".", "..", "")
                .forEach(tenant -> assertThat(RequestedTenant.isValidTenant(tenant)).as(tenant).isFalse());
        assertThat(RequestedTenant.isValidTenant(null)).isFalse();
    }

    /**
     * The requested tenant is available (from the thread local) only while the callable is executed.
     */
    @Test
    void withTenantSetsTenantOnlyForTheCall() throws Exception {
        assertThat(RequestedTenant.getTenant()).isNull();
        assertThat(RequestedTenant.withTenant(VALID_TENANT, RequestedTenant::getTenant)).isEqualTo(VALID_TENANT);
        assertThat(RequestedTenant.getTenant()).isNull();
    }

    /**
     * Nested calls restore the previous requested tenant.
     */
    @Test
    void withTenantRestoresPreviousTenant() throws Exception {
        RequestedTenant.withTenant(VALID_TENANT, () -> {
            assertThat(RequestedTenant.withTenant("OTHER", RequestedTenant::getTenant)).isEqualTo("OTHER");
            assertThat(RequestedTenant.getTenant()).isEqualTo(VALID_TENANT);
            return null;
        });
        assertThat(RequestedTenant.getTenant()).isNull();
    }

    /**
     * Invalid tenant can't be set.
     */
    @Test
    void withInvalidTenantFails() {
        assertThatThrownBy(() -> RequestedTenant.withTenant(INVALID_TENANT, () -> null))
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
        assertThatThrownBy(() -> RequestedTenant.withTenant(null, () -> null))
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }

    /**
     * If the thread local is not set, the requested tenant is taken from the current request attribute. The thread local has priority.
     */
    @Test
    void fallbackToRequestAttribute() throws Exception {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        assertThat(RequestedTenant.getTenant()).isNull();

        request.setAttribute(RequestedTenant.REQUEST_ATTRIBUTE, VALID_TENANT);
        assertThat(RequestedTenant.getTenant()).isEqualTo(VALID_TENANT);
        assertThat(RequestedTenant.withTenant("OTHER", RequestedTenant::getTenant)).isEqualTo("OTHER");
    }
}
