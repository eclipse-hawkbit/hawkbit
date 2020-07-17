package org.eclipse.hawkbit.ui.rollout.window.components;

import com.vaadin.data.Binder;

/**
 * Layout for input validation
 */
public abstract class ValidatableLayout {

    protected ValidationStatus validationStatus;
    protected ValidationListener validationListener;

    protected ValidatableLayout() {
        this.validationStatus = ValidationStatus.UNKNOWN;
    }

    /**
     * Sets the validation listener
     *
     * @param validationListener
     *          ValidationListener
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
