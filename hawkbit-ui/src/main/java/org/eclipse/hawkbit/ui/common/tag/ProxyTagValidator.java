/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.tag;

import java.util.function.BooleanSupplier;

import org.eclipse.hawkbit.ui.common.EntityValidator;
import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
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
            final String trimmedName = StringUtils.trimWhitespace(entity.getName());
            displayValidationError("message.tag.duplicate.check", trimmedName);
            return false;
        }

        return true;
    }
}
