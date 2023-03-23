/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.rollout.window.layouts;

import java.util.Collection;
import java.util.HashSet;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.ui.common.EntityWindowLayout;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyRolloutWindow;
import org.eclipse.hawkbit.ui.rollout.window.RolloutWindowDependencies;
import org.eclipse.hawkbit.ui.rollout.window.RolloutWindowLayoutComponentBuilder;
import org.eclipse.hawkbit.ui.rollout.window.components.ValidatableLayout;
import org.eclipse.hawkbit.ui.rollout.window.components.ValidatableLayout.ValidationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.data.ValidationException;
import com.vaadin.data.ValidationResult;
import com.vaadin.server.Sizeable.Unit;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.GridLayout;

/**
 * Abstract Grid Rollout window layout.
 */
@SuppressWarnings({ "squid:MaximumInheritanceDepth", "squid:S2160" })
public abstract class AbstractRolloutWindowLayout implements EntityWindowLayout<ProxyRolloutWindow> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractRolloutWindowLayout.class);

    protected final RolloutWindowLayoutComponentBuilder rolloutComponentBuilder;

    private final Collection<ValidatableLayout> validatableLayouts;
    private Consumer<Boolean> validationCallback;

    protected AbstractRolloutWindowLayout(final RolloutWindowDependencies dependencies) {
        this.rolloutComponentBuilder = new RolloutWindowLayoutComponentBuilder(dependencies);
        this.validatableLayouts = new HashSet<>();
    }

    @Override
    public ComponentContainer getRootComponent() {
        final GridLayout rootLayout = new GridLayout();

        rootLayout.setSpacing(true);
        rootLayout.setSizeUndefined();
        rootLayout.setColumns(4);
        rootLayout.setStyleName("marginTop");
        rootLayout.setColumnExpandRatio(3, 1);
        rootLayout.setWidth(900, Unit.PIXELS);

        addComponents(rootLayout);

        return rootLayout;
    }

    protected void addValidatableLayouts(final Collection<ValidatableLayout> validatableLayouts) {
        validatableLayouts.forEach(this::addValidatableLayout);
    }

    protected void addValidatableLayout(final ValidatableLayout validatableLayout) {
        if (validatableLayout != null) {
            validatableLayout.setValidationListener(this::onValidationChanged);
            validatableLayouts.add(validatableLayout);
        }
    }

    protected void removeValidatableLayout(final ValidatableLayout validatableLayout) {
        if (validatableLayout != null) {
            validatableLayout.setValidationListener(null);
            validatableLayouts.remove(validatableLayout);
        }
    }

    private void onValidationChanged(final ValidationStatus status) {
        if (validationCallback == null) {
            return;
        }

        // shortcut for setting the whole layout as invalid if at least one
        // validatable sub-layout becomes invalid
        if (ValidationStatus.VALID != status) {
            validationCallback.accept(false);
            return;
        }

        validationCallback.accept(allLayoutsValid());
    }

    private boolean allLayoutsValid() {
        if (validatableLayouts.isEmpty()) {
            return false;
        }

        return validatableLayouts.stream().allMatch(ValidatableLayout::isValid);
    }

    @Override
    public void addValidationListener(final Consumer<Boolean> validationCallback) {
        this.validationCallback = validationCallback;

        validationCallback.accept(allLayoutsValid());
    }

    @Override
    public ProxyRolloutWindow getEntity() {
        try {
            return getValidatableEntity();
        } catch (final ValidationException e) {
            final String validationErrors = e.getValidationErrors().stream().map(ValidationResult::getErrorMessage)
                    .collect(Collectors.joining(";"));
            LOGGER.trace("There was a validation error while trying to get the rollouts window bean: {} {}",
                    e.getMessage(), validationErrors);

            return null;
        }
    }

    protected abstract ProxyRolloutWindow getValidatableEntity() throws ValidationException;

    protected abstract void addComponents(final GridLayout rootLayout);
}
