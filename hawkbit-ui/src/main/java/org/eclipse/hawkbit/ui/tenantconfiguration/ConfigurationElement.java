package org.eclipse.hawkbit.ui.tenantconfiguration;

import java.io.Serializable;

public interface ConfigurationElement {

    /**
     * called to verify that the Input done by the user is valid
     * 
     * @return true when the data is valid, false otherwise
     */
    boolean isUserInputValid();

    /**
     * Adds a configuration change listener to notify about configuration
     * changes.
     * 
     * @param listener
     *            the listener to be notified in case the item changes some
     *            configuration
     */
    void addChangeListener(ConfigurationGroupChangeListener listener);

    /**
     * Configuration Change Listener to be notified about configuration changes
     * in configuration group.
     *
     */
    interface ConfigurationGroupChangeListener extends Serializable {
        /**
         * called to notify about configuration has been changed.
         */
        void configurationChanged();
    }
}