/**
 * Copyright (c) 2020 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ui.filtermanagement;

import java.util.function.BooleanSupplier;

import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.EntityValidator;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTargetFilterQuery;
import org.springframework.util.StringUtils;

/**
 * Validator used in targetfilter window controllers to validate
 * {@link ProxyTargetFilterQuery}.
 */
public class ProxyTargetFilterValidator extends EntityValidator {

    /**
     * Constructor
     *
     * @param uiDependencies
     *            {@link CommonUiDependencies}
     */
    public ProxyTargetFilterValidator(final CommonUiDependencies uiDependencies) {
        super(uiDependencies);
    }

    boolean isEntityValid(final ProxyTargetFilterQuery entity, final BooleanSupplier duplicateCheck) {
        if (!StringUtils.hasText(entity.getName())) {
            displayValidationError("message.error.missing.filtername");
            return false;
        }

        if (duplicateCheck.getAsBoolean()) {
            displayValidationError("message.target.filter.duplicate", entity.getName());
            return false;
        }

        return true;
    }
}
