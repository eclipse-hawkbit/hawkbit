/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common;

import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;

/**
 * Base function for a validator used in window controllers.
 */
public class EntityValidator {

    private final CommonUiDependencies uiDependencies;

    /**
     * Constructor
     *
     * @param uiDependencies
     *            {@link CommonUiDependencies}
     */
    protected EntityValidator(final CommonUiDependencies uiDependencies) {
        this.uiDependencies = uiDependencies;

    }

    protected void displayValidationError(final String messageKey, final Object... args) {
        uiDependencies.getUiNotification()
                .displayValidationError(uiDependencies.getI18n().getMessage(messageKey, args));
    }

    protected VaadinMessageSource getI18n() {
        return uiDependencies.getI18n();
    }
}
