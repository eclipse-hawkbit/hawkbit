/**
 * Copyright (c) 2020 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ui.common;

import java.util.function.Consumer;

import com.vaadin.ui.ComponentContainer;

/**
 * Interface for entity window layout
 *
 * @param <T>
 *            Generic type entity
 */
public interface EntityWindowLayout<T> {

    ComponentContainer getRootComponent();

    void setEntity(final T proxyEntity);

    T getEntity();

    /**
     * Method to add validation listeners.
     * 
     * @param validationCallback
     *            Validation callback event
     */
    void addValidationListener(final Consumer<Boolean> validationCallback);
}
