/**
 * Copyright (c) 2020 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ui.rollout.window.controllers;

import java.util.function.BooleanSupplier;

import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.EntityValidator;
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

        if (duplicateCheck.getAsBoolean()) {
            displayValidationError("message.rollout.duplicate.check", entity.getName());
            return false;
        }

        return true;
    }
}
