/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common;

import java.util.Optional;
import java.util.function.Consumer;

import com.vaadin.data.Binder;

/**
 * Abstract class for entity window layout
 *
 * @param <T>
 *     Generic type entity
 */
public abstract class AbstractEntityWindowLayout<T> implements EntityWindowLayout<T> {

    protected final Binder<T> binder;

    protected Consumer<Boolean> validationCallback;

    protected AbstractEntityWindowLayout() {
        this.binder = new Binder<>();
    }

    @Override
    public void setEntity(final T proxyEntity) {
        binder.setBean(proxyEntity);
    }

    @Override
    public T getEntity() {
        return binder.getBean();
    }

    @Override
    public void addValidationListener(final Consumer<Boolean> validationCallback) {
        binder.addStatusChangeListener(event -> validationCallback.accept(event.getBinder().isValid()));
        this.validationCallback = validationCallback;
    }

    /**
     * @return Validation callback event
     */
    public Optional<Consumer<Boolean>> getValidationCallback() {
        return Optional.ofNullable(validationCallback);
    }
}
