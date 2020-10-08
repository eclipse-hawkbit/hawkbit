/**
 * Copyright (c) 2020 Bosch.IO GmbH, Germany. All rights reserved.
 */
package org.eclipse.hawkbit.ui.management.targettable;

import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTarget;
import org.springframework.util.StringUtils;

/**
 * Validator used in target window controllers to validate {@link ProxyTarget}.
 */
public class ProxyTargetValidator {

    private final String KEY_MISSING_CONTROLLER_ID = "message.error.missing.controllerId";
    private final String KEY_DUPLICATE = "message.target.duplicate.check";
    private final CommonUiDependencies uiDependencies;

    /**
     * Constructor
     *
     * @param uiDependencies
     *            {@link CommonUiDependencies}
     */
    public ProxyTargetValidator(final CommonUiDependencies uiDependencies) {
        this.uiDependencies = uiDependencies;

    }

    boolean isEntityValid(final ProxyTarget entity, final boolean hasEntityChanged,
            final boolean entityExistsInRepository) {
        if (!StringUtils.hasText(entity.getControllerId())) {
            displayValidationError(KEY_MISSING_CONTROLLER_ID);
            return false;
        }

        final String trimmedControllerId = StringUtils.trimWhitespace(entity.getControllerId());
        if (hasEntityChanged && entityExistsInRepository) {
            displayValidationError(KEY_DUPLICATE, trimmedControllerId);
            return false;
        }

        return true;
    }

    private void displayValidationError(final String messageKey, final Object... args) {
        uiDependencies.getUiNotification()
                .displayValidationError(uiDependencies.getI18n().getMessage(messageKey, args));
    }
}
