/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.targettype;

import java.util.function.BooleanSupplier;

import org.eclipse.hawkbit.ui.common.EntityValidator;
import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
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
            final String trimmedName = StringUtils.trimWhitespace(entity.getName());
            displayValidationError("message.type.duplicate.check", trimmedName);
            return false;
        }

        return true;
    }

}
