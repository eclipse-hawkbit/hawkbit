/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.tag;

import org.eclipse.hawkbit.ui.common.AbstractEntityWindowLayout;
import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyFilterButton;
import org.eclipse.hawkbit.ui.components.ColorPickerComponent;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;

import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;

/**
 * Abstract class for tag add/update window layout.
 *
 * @param <T>
 *            Generic type of ProxyFilterButton
 */
public class TagWindowLayout<T extends ProxyFilterButton> extends AbstractEntityWindowLayout<T> {
    protected final VaadinMessageSource i18n;

    protected final TagWindowLayoutComponentBuilder tagComponentBuilder;

    protected final TextField tagName;
    protected final TextArea tagDescription;
    protected final ColorPickerComponent colorPickerComponent;

    /**
     * Constructor for AbstractTagWindowLayout
     *
     * @param uiDependencies
     *            {@link CommonUiDependencies}
     */
    public TagWindowLayout(final CommonUiDependencies uiDependencies) {
        super();

        this.i18n = uiDependencies.getI18n();

        this.tagComponentBuilder = new TagWindowLayoutComponentBuilder(i18n, uiDependencies.getUiNotification());

        this.tagName = tagComponentBuilder.createNameField(binder);
        this.tagDescription = tagComponentBuilder.createDescription(binder);
        this.colorPickerComponent = tagComponentBuilder.createColorPickerComponent(binder);
    }

    @Override
    public ComponentContainer getRootComponent() {
        final HorizontalLayout tagWindowLayout = new HorizontalLayout();

        tagWindowLayout.setSpacing(true);
        tagWindowLayout.setMargin(false);
        tagWindowLayout.setSizeUndefined();

        tagWindowLayout.addComponent(buildFormLayout());
        tagWindowLayout.addComponent(colorPickerComponent);

        return tagWindowLayout;
    }

    protected FormLayout buildFormLayout() {
        final FormLayout formLayout = new FormLayout();

        formLayout.addComponent(tagName);
        tagName.focus();

        formLayout.addComponent(tagDescription);
        formLayout.addComponent(colorPickerComponent.getColorPickerBtn());

        return formLayout;
    }

    /**
     * Disable tag name
     */
    public void disableTagName() {
        tagName.setEnabled(false);
    }
}
