/**
 * Copyright (c) 2020 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ui.management.targettable;

import java.util.function.BooleanSupplier;

import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.EntityValidator;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTarget;
import org.springframework.util.StringUtils;

/**
 * Validator used in target window controllers to validate {@link ProxyTarget}.
 */
public class ProxyTargetValidator extends EntityValidator {

    /**
     * Constructor
     *
     * @param uiDependencies
     *            {@link CommonUiDependencies}
     */
    public ProxyTargetValidator(final CommonUiDependencies uiDependencies) {
        super(uiDependencies);
    }

    boolean isEntityValid(final ProxyTarget entity, final BooleanSupplier duplicateCheck) {
        if (!StringUtils.hasText(entity.getControllerId())) {
            displayValidationError("message.error.missing.controllerId");
            return false;
        }

        if (duplicateCheck.getAsBoolean()) {
            displayValidationError("message.target.duplicate.check", entity.getControllerId());
            return false;
        }

        return true;
    }
}
