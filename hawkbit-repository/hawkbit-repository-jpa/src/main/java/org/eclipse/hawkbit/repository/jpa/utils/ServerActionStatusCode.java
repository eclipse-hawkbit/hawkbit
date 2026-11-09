/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.eclipse.hawkbit.repository.helper.TenantConfigHelper;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.jpa.model.JpaActionStatus;

/**
 * Applies the optional, tenant configured status codes (see {@code TenantConfigurationKey.ACTION_STATUS_CODE_*}) to the action
 * statuses the server writes itself. A key that is not defined resolves to {@code null} and leaves the status untouched.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ServerActionStatusCode {

    /**
     * Resolves the code configured for the given key for the current tenant.
     *
     * @param configKey tenant configuration key of type {@link Integer}
     * @return the configured code or {@code null} if not defined
     */
    public static Integer resolve(final String configKey) {
        return TenantConfigHelper.getAsSystem(configKey, Integer.class);
    }

    /**
     * Sets the code on the status and as last action status code of its action. To be used only if the status also changes the
     * state of the action. No-op if the code is {@code null}.
     * <p/>
     * Precondition: the action of the status is set.
     *
     * @param status the server written action status
     * @param code the resolved code, could be {@code null}
     */
    public static void apply(final JpaActionStatus status, final Integer code) {
        if (code != null) {
            status.setCode(code);
            ((JpaAction) status.getAction()).setLastActionStatusCode(code);
        }
    }

    /**
     * Sets the code on the status only, the last action status code of the action is not changed. No-op if the code is {@code null}.
     *
     * @param status the server written action status
     * @param code the resolved code, could be {@code null}
     */
    public static void applyToStatusOnly(final JpaActionStatus status, final Integer code) {
        if (code != null) {
            status.setCode(code);
        }
    }
}
