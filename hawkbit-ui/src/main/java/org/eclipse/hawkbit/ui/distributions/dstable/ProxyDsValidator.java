/**
 * Copyright (c) 2020 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ui.distributions.dstable;

import java.util.function.BooleanSupplier;

import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.EntityValidator;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyDistributionSet;
import org.springframework.util.StringUtils;

/**
 * Validator used in target window controllers to validate {@link ProxyDistributionSet}.
 */
public class ProxyDsValidator extends EntityValidator {

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

        if (duplicateCheck.getAsBoolean()) {
            displayValidationError("message.duplicate.dist", entity.getName(), entity.getVersion());
            return false;
        }

        return true;
    }
}
