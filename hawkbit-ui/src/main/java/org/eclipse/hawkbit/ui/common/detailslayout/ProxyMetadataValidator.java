/**
 * Copyright (c) 2020 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ui.common.detailslayout;

import java.util.function.BooleanSupplier;

import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.EntityValidator;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyMetaData;
import org.springframework.util.StringUtils;

/**
 * Validator used in Metadata window controllers to validate
 * {@link ProxyMetaData}.
 */
public class ProxyMetadataValidator extends EntityValidator {

    /**
     * Constructor
     *
     * @param uiDependencies
     *            {@link CommonUiDependencies}
     */
    public ProxyMetadataValidator(final CommonUiDependencies uiDependencies) {
        super(uiDependencies);
    }

    boolean isEntityValid(final ProxyMetaData entity, final BooleanSupplier duplicateCheck) {
        if (!StringUtils.hasText(entity.getValue())) {
            displayValidationError("message.value.missing");
            return false;
        }

        if (!StringUtils.hasText(entity.getKey())) {
            displayValidationError("message.key.missing");
            return false;
        }

        if (duplicateCheck.getAsBoolean()) {
            displayValidationError("message.metadata.duplicate.check", entity.getKey());
            return false;
        }

        return true;
    }
}
