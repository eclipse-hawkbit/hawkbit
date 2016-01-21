/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.tenantconfiguration;

import java.io.Serializable;

/**
 *
 *
 *
 */
public interface ConfigurationGroup {

    /**
     * called to store any configuration changes.
     */
    void save();

    /**
     * called to rollback any configuration changes.
     */
    void undo();

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
     *
     *
     */
    interface ConfigurationGroupChangeListener extends Serializable {
        /**
         * called to notify about configuration has been changed.
         */
        void configurationChanged();
    }

}
