/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.detailslayout;

import java.util.function.BooleanSupplier;

import org.eclipse.hawkbit.ui.common.AbstractValidator;
import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyMetaData;
import org.springframework.util.StringUtils;

/**
 * Validator used in Metadata window controllers to validate
 * {@link ProxyMetaData}.
 */
public class ProxyMetadataValidator extends AbstractValidator {

    /**
     * Constructor
     *
     * @param uiDependencies
     *            {@link CommonUiDependencies}
     */
    public ProxyMetadataValidator(final CommonUiDependencies uiDependencies) {
        super(uiDependencies);
    }

    boolean isEntityValidForAdd(final ProxyMetaData entity, final BooleanSupplier duplicateCheck) {
        return mandatoryAttributesPresent(entity) && keyValid(entity, duplicateCheck);
    }

    boolean isEntityValidForUpdate(final ProxyMetaData entity) {
        return mandatoryAttributesPresent(entity);
    }

    private boolean keyValid(final ProxyMetaData entity, final BooleanSupplier duplicateCheck) {
        final String trimmedKey = StringUtils.trimWhitespace(entity.getKey());
        if (duplicateCheck.getAsBoolean()) {
            displayValidationError("message.metadata.duplicate.check", trimmedKey);
            return false;
        }
        return true;
    }

    private boolean mandatoryAttributesPresent(final ProxyMetaData entity) {
        if (!StringUtils.hasText(entity.getValue())) {
            displayValidationError("message.value.missing");
            return false;
        }

        if (!StringUtils.hasText(entity.getKey())) {
            displayValidationError("message.key.missing");
            return false;
        }

        return true;
    }
}
