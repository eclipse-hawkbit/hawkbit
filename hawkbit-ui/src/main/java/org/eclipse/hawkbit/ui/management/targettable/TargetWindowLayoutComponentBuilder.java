/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.targettable;

import com.vaadin.data.Binder;
import com.vaadin.data.validator.RegexpValidator;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.ui.common.builder.BoundComponent;
import org.eclipse.hawkbit.ui.common.builder.FormComponentBuilder;
import org.eclipse.hawkbit.ui.common.builder.TextFieldBuilder;
import org.eclipse.hawkbit.ui.common.data.providers.TargetTypeDataProvider;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTarget;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTypeInfo;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;

/**
 * Builder for target window layout component
 */
public class TargetWindowLayoutComponentBuilder {

    public static final String TEXTFIELD_CONTROLLER_ID = "prompt.target.id";

    private final VaadinMessageSource i18n;

    /**
     * Constructor for TargetWindowLayoutComponentBuilder
     *
     * @param i18n
     *            VaadinMessageSource
     */
    public TargetWindowLayoutComponentBuilder(final VaadinMessageSource i18n) {
        this.i18n = i18n;
    }

    /**
     * Create a controller id field
     *
     * @param binder
     *            Target binder
     *
     * @return Target controller id Text field
     */
    public TextField createControllerIdField(final Binder<ProxyTarget> binder) {
        final TextField targetControllerId = new TextFieldBuilder(Target.CONTROLLER_ID_MAX_SIZE)
                .id(UIComponentIdProvider.TARGET_ADD_CONTROLLER_ID).caption(i18n.getMessage(TEXTFIELD_CONTROLLER_ID))
                .prompt(i18n.getMessage(TEXTFIELD_CONTROLLER_ID)).buildTextComponent();
        targetControllerId.setSizeUndefined();

        binder.forField(targetControllerId).asRequired(i18n.getMessage("message.error.missing.controllerId"))
                .withValidator(new RegexpValidator(i18n.getMessage("message.target.whitespace.check"), "[.\\S]*"))
                .bind(ProxyTarget::getControllerId, ProxyTarget::setControllerId);

        return targetControllerId;
    }

    /**
     * create a required name field
     * 
     * @param binder
     *            binder the input will be bound to
     * @return input component
     */
    public BoundComponent<TextField> createNameField(final Binder<ProxyTarget> binder) {
        return FormComponentBuilder.createNameInput(binder, i18n, UIComponentIdProvider.TARGET_ADD_NAME);
    }

    /**
     * create description field
     * 
     * @param binder
     *            binder the input will be bound to
     * @return input component
     */
    public TextArea createDescriptionField(final Binder<ProxyTarget> binder) {
        return FormComponentBuilder.createDescriptionInput(binder, i18n, UIComponentIdProvider.TARGET_ADD_DESC)
                .getComponent();
    }

    /**
     * create target type combo
     *
     * @param binder
     *            binder the input will be bound to
     * @param targetTypeDataProvider
     *            TargetTypeDataProvider
     * @return input component
     */
    public BoundComponent<ComboBox<ProxyTypeInfo>> createTargetTypeCombo(final Binder<ProxyTarget> binder, TargetTypeDataProvider<ProxyTypeInfo> targetTypeDataProvider) {
        return FormComponentBuilder
                .createTypeCombo(binder, targetTypeDataProvider, i18n, UIComponentIdProvider.TARGET_ADD_TARGETTYPE, false);
    }

}
