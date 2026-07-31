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

import lombok.NoArgsConstructor;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.TenantConfigurationValue;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.stream.Collectors;

@NoArgsConstructor
public class ActionUtils {

    private static final EnumSet<Action.Status> EMPTY_STATUS_SET = EnumSet.noneOf(Action.Status.class);

    public static EnumSet<Action.Status> getActionStatus(final TenantConfigurationManagement config, String key) {
        final TenantConfigurationValue<String> statusStr = config.getConfigurationValue(key, String.class);
        if (statusStr != null) {
            return Arrays.stream(statusStr.getValue().split("[;,]"))
                    .map(Action.Status::valueOf).collect(Collectors.toCollection(() -> EnumSet.noneOf(Action.Status.class)));
        }
        return EMPTY_STATUS_SET;
    }
}
