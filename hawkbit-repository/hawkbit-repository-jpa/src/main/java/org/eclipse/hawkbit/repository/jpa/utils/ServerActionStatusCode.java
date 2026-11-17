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
     * Sets the code configured for the given key on the status and as last action status code of its action. To be used only if
     * the status also changes the state of the action. No-op if the key is not defined for the current tenant.
     * <p/>
     * Precondition: the action of the status is set.
     * <p/>
     * The code is resolved on every call - tenant configuration values are cached, so no need to resolve them once per batch.
     *
     * @param status the server written action status
     * @param configKey tenant configuration key of type {@link Integer}
     * @return {@code true} if a code has been set (i.e. the action has been changed), {@code false} otherwise
     */
    public static boolean apply(final JpaActionStatus status, final String configKey) {
        final Integer code = resolve(configKey);
        if (code == null) {
            return false;
        }
        status.setCode(code);
        ((JpaAction) status.getAction()).setLastActionStatusCode(code);
        return true;
    }

    /**
     * Sets the code configured for the given key on the status only, the last action status code of the action is not changed.
     * No-op if the key is not defined for the current tenant.
     *
     * @param status the server written action status
     * @param configKey tenant configuration key of type {@link Integer}
     */
    public static void applyToStatusOnly(final JpaActionStatus status, final String configKey) {
        final Integer code = resolve(configKey);
        if (code != null) {
            status.setCode(code);
        }
    }

    private static Integer resolve(final String configKey) {
        return TenantConfigHelper.getAsSystem(configKey, Integer.class);
    }
}
