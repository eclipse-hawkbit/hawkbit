/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.targettable;

import java.util.function.BooleanSupplier;

import org.eclipse.hawkbit.ui.common.EntityValidator;
import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
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
            final String trimmedControllerId = StringUtils.trimWhitespace(entity.getControllerId());
            displayValidationError("message.target.duplicate.check", trimmedControllerId);
            return false;
        }

        return true;
    }
}
