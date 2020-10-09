/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.distributions.dstable;

import java.util.function.BooleanSupplier;

import org.eclipse.hawkbit.ui.common.AbstractValidator;
import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyDistributionSet;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTarget;
import org.springframework.util.StringUtils;

/**
 * Validator used in target window controllers to validate {@link ProxyTarget}.
 */
public class ProxyDsValidator extends AbstractValidator {

    /**
     * Constructor
     *
     * @param uiDependencies
     *            {@link CommonUiDependencies}
     */
    public ProxyDsValidator(final CommonUiDependencies uiDependencies) {
        super(uiDependencies);
    }

    boolean isEntityValid(final ProxyDistributionSet entity, final BooleanSupplier duplicateCheck) {
        if (!StringUtils.hasText(entity.getName()) || !StringUtils.hasText(entity.getVersion())) {
            displayValidationError("message.error.missing.nameorversion");
            return false;
        }

        final String trimmedName = StringUtils.trimWhitespace(entity.getName());
        final String trimmedVersion = StringUtils.trimWhitespace(entity.getVersion());
        if (duplicateCheck.getAsBoolean()) {
            displayValidationError("message.duplicate.dist", trimmedName, trimmedVersion);
            return false;
        }

        return true;
    }
}
