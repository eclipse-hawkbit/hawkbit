/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.rollout.window.controllers;

import java.util.function.BooleanSupplier;

import org.eclipse.hawkbit.ui.common.EntityValidator;
import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyRolloutWindow;
import org.springframework.util.StringUtils;

/**
 * Validator used in rollout window controllers to validate
 * {@link ProxyRolloutWindow}.
 */
public class ProxyRolloutValidator extends EntityValidator {

    /**
     * Constructor
     *
     * @param uiDependencies
     *            {@link CommonUiDependencies}
     */
    public ProxyRolloutValidator(final CommonUiDependencies uiDependencies) {
        super(uiDependencies);
    }

    boolean isEntityValid(final ProxyRolloutWindow entity, final BooleanSupplier duplicateCheck) {
        if (entity == null) {
            displayValidationError("message.save.fail", getI18n().getMessage("caption.rollout"));
            return false;
        }

        if (!StringUtils.hasText(entity.getName())) {
            displayValidationError("message.rollout.name.empty");
            return false;
        }

        final String trimmedName = StringUtils.trimWhitespace(entity.getName());
        if (duplicateCheck.getAsBoolean()) {
            displayValidationError("message.rollout.duplicate.check", trimmedName);
            return false;
        }

        return true;
    }
}
