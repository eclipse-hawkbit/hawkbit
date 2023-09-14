/**
 * Copyright (c) 2020 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ui.common.tag;

import java.util.function.BooleanSupplier;

import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.EntityValidator;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTag;
import org.springframework.util.StringUtils;

/**
 * Validator used in *Tag window controllers to validate {@link ProxyTag}.
 */
public class ProxyTagValidator extends EntityValidator {

    /**
     * Constructor
     *
     * @param uiDependencies
     *            {@link CommonUiDependencies}
     */
    public ProxyTagValidator(final CommonUiDependencies uiDependencies) {
        super(uiDependencies);
    }

    /**
     * Checks if the entity is valid
     *
     * @param entity
     *            {@link ProxyTag}
     * @param duplicateCheck
     *            <code>true</code> if the entity already exists in the
     *            repository
     * @return <code>true</code> if the entity is valid
     */
    public boolean isEntityValid(final ProxyTag entity, final BooleanSupplier duplicateCheck) {
        if (!StringUtils.hasText(entity.getName())) {
            displayValidationError("message.error.missing.tagname");
            return false;
        }

        if (duplicateCheck.getAsBoolean()) {
            displayValidationError("message.tag.duplicate.check", entity.getName());
            return false;
        }

        return true;
    }
}
