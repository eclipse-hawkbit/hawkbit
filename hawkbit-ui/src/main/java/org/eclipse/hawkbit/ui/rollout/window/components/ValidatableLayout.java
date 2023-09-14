/**
 * Copyright (c) 2020 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ui.rollout.window.components;

import com.vaadin.data.Binder;

/**
 * Layout for input validation
 */
public class ValidatableLayout {

    protected ValidationStatus validationStatus;
    protected ValidationListener validationListener;

    protected ValidatableLayout() {
        this.validationStatus = ValidationStatus.UNKNOWN;
    }

    /**
     * Sets the validation listener
     *
     * @param validationListener
     *            ValidationListener
     */
    public void setValidationListener(final ValidationListener validationListener) {
        this.validationListener = validationListener;
    }

    protected void setValidationStatusByBinder(final Binder<?> binder) {
        binder.addStatusChangeListener(event -> setValidationStatus(
                event.getBinder().isValid() ? ValidationStatus.VALID : ValidationStatus.INVALID));
    }

    protected void setValidationStatus(final ValidationStatus status) {
        if (status == validationStatus) {
            return;
        }

        validationStatus = status;

        if (validationListener != null) {
            validationListener.validationStatusChanged(status);
        }
    }

    public ValidationStatus getValidationStatus() {
        return validationStatus;
    }

    /**
     * @return True when status is valid else false
     */
    public boolean isValid() {
        return ValidationStatus.VALID == validationStatus;
    }

    /**
     * Reset the validation status to unknown
     */
    public void resetValidationStatus() {
        validationStatus = ValidationStatus.UNKNOWN;
    }

    /**
     * Implement the interface and set the instance with setValidationListener
     * to receive updates for any validation status changes of the layout.
     */
    @FunctionalInterface
    public interface ValidationListener {
        /**
         * Is called after user input
         *
         * @param validationStatus
         *            whether the input of the group rows is valid
         */
        void validationStatusChanged(ValidationStatus validationStatus);
    }

    /**
     * Status of the validation
     */
    public enum ValidationStatus {
        UNKNOWN, VALID, INVALID
    }
}
