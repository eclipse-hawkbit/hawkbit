/** 
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.builder;

import java.io.Serializable;

import com.vaadin.data.Binder.Binding;
import com.vaadin.ui.Component;

/**
 * Holds a {@link Component} and its {@link Binding}
 * 
 * @param <T>
 *            Component type
 */
public class BoundComponent<T extends Component> implements Serializable {
    private static final long serialVersionUID = 1L;

    private final T component;
    private final Binding<?, ?> binding;

    /**
     * Constructor
     * 
     * @param component
     *            component
     * @param binding
     *            binding of the component
     */
    public BoundComponent(final T component, final Binding<?, ?> binding) {
        this.component = component;
        this.binding = binding;
    }

    /**
     * @return The component
     */
    public T getComponent() {
        return component;
    }

    /**
     * Set to true if the component is required
     *
     * @param isRequired
     *            boolean
     */
    public void setRequired(final boolean isRequired) {
        binding.setAsRequiredEnabled(isRequired);
    }

    /**
     * @return boolean
     */
    public boolean isValid() {
        return !binding.validate(false).isError();
    }

    /**
     * Validate binding
     */
    public void validate() {
        binding.validate();
    }

    /**
     * unbind the component
     */
    public void unbind() {
        binding.unbind();
    }
}
