/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.helper;

import static org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey.MULTI_ASSIGNMENTS_ENABLED;
import static org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey.USER_CONFIRMATION_ENABLED;

import java.io.Serializable;
import java.util.Objects;
import java.util.function.Function;

import lombok.NoArgsConstructor;
import org.eclipse.hawkbit.context.AccessContext;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.model.PollStatus;
import org.eclipse.hawkbit.repository.model.Target;

/**
 * A collection of static helper methods for the tenant configuration
 */
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class TenantConfigHelper {

    private static TenantConfigurationManagement tenantConfigurationManagement;

    // method to be initialized by the TenantConfigurationManagement or TenantConfigurationManagement creator
    // it will be accessed directly and used so shall be fully initialized instance, i.e. a bean in order to onore things
    // like @PreAuthorize, @Transactional etc.
    public static void setTenantConfigurationManagement(final TenantConfigurationManagement tenantConfigurationManagement) {
        TenantConfigHelper.tenantConfigurationManagement = tenantConfigurationManagement;
    }

    public static TenantConfigurationManagement getTenantConfigurationManagement() {
        return Objects.requireNonNull(tenantConfigurationManagement, "TenantConfigurationManagement has not been initialized");
    }

    public static <T extends Serializable> T getAsSystem(final String key, final Class<T> valueType) {
        return AccessContext.asSystem(() -> getTenantConfigurationManagement().getConfigurationValue(key, valueType).getValue());
    }

    public static boolean isMultiAssignmentsEnabled() {
        return getAsSystem(MULTI_ASSIGNMENTS_ENABLED, Boolean.class);
    }

    public static boolean isConfirmationFlowEnabled() {
        return getAsSystem(USER_CONFIRMATION_ENABLED, Boolean.class);
    }

    public static Function<Target, PollStatus> pollStatusResolver() {
        return getTenantConfigurationManagement().pollStatusResolver();
    }
}