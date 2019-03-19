package org.eclipse.hawkbit.ui.management;

/**
 * Helper Interface for change listeners to toggle the Save button in the Confirm Assignment Dialog
 */
@FunctionalInterface
public interface SaveButtonEnabler {

    /**
     * Change the state of the Save Button in the ConfirmAssignment Dialog
     * @param enabled
     *          boolean to enable or disable the Save Button
     */
    void setButtonEnabled(boolean enabled);
}
