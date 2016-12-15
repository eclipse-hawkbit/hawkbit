/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.tenantconfiguration;

import com.vaadin.ui.Component;

/**
 * Interface that all system configurations have to implement to save and undo
 * their customized changes.
 */
public interface ConfigurationGroup extends Component, ConfigurationItem {

    /**
     * called to store any configuration changes.
     */
    void save();

    /**
     * called to rollback any configuration changes.
     */
    void undo();

    /**
     * @return <code>true</code> if view can be shown (e.g. sufficient
     *         permissions).
     */
    default boolean show() {
        return true;
    }
}
