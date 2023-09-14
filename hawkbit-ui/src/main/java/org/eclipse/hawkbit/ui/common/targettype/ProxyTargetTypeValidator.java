/**
 * Copyright (c) 2021 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ui.common.targettype;

import java.util.function.BooleanSupplier;

import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.EntityValidator;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTargetType;
import org.springframework.util.StringUtils;


/**
 * Validator used in TargetType window controllers to validate {@link ProxyTargetType}.
 */
public class ProxyTargetTypeValidator extends EntityValidator {

    /**
     * Constructor
     *
     * @param uiDependencies
     *            {@link CommonUiDependencies}
     */
    public ProxyTargetTypeValidator(final CommonUiDependencies uiDependencies) {
        super(uiDependencies);
    }

    public boolean isEntityValid(final ProxyTargetType entity, final BooleanSupplier duplicateCheck) {
        if (!StringUtils.hasText(entity.getName())) {
            displayValidationError("message.error.missing.typename");
            return false;
        }

        if (duplicateCheck.getAsBoolean()) {
            displayValidationError("message.type.duplicate.check", entity.getName());
            return false;
        }

        return true;
    }

}
