/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.tenantconfiguration;

import java.util.ArrayList;
import java.util.List;

import com.vaadin.ui.CustomComponent;

/**
 * Base class for all configuration views. This class implements the logic for
 * the handling of the configurations in a consistent way.
 * 
 */
public abstract class BaseConfigurationView extends CustomComponent implements ConfigurationGroup {

    private static final long serialVersionUID = 1L;

    private final List<ConfigurationItemChangeListener> configurationChangeListeners = new ArrayList<>();

    protected void notifyConfigurationChanged() {
        configurationChangeListeners.forEach(ConfigurationItemChangeListener::configurationHasChanged);
    }

    @Override
    public void addChangeListener(final ConfigurationItemChangeListener listener) {
        configurationChangeListeners.add(listener);
    }

    @Override
    public boolean isUserInputValid() {
        // default return value is true, because often user can only choose from
        // different valid options.
        return true;
    }
}
