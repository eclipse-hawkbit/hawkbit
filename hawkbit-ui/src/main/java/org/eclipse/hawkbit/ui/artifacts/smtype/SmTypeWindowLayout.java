/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts.smtype;

import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.builder.FormComponentBuilder;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyType;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyType.SmTypeAssign;
import org.eclipse.hawkbit.ui.management.tag.TagWindowLayout;

import com.vaadin.ui.FormLayout;
import com.vaadin.ui.RadioButtonGroup;
import com.vaadin.ui.TextField;

/**
 * Software module type Window Layout view
 */
public class SmTypeWindowLayout extends TagWindowLayout<ProxyType> {
    private final SmTypeWindowLayoutComponentBuilder smTypeComponentBuilder;

    private final TextField typeKey;
    private final RadioButtonGroup<SmTypeAssign> smTypeAssignOptionGroup;

    /**
     * Constructor for SmTypeWindowLayout
     *
     * @param uiDependencies
     *            {@link CommonUiDependencies}
     */
    public SmTypeWindowLayout(final CommonUiDependencies uiDependencies) {
        super(uiDependencies);

        this.smTypeComponentBuilder = new SmTypeWindowLayoutComponentBuilder(i18n);

        this.typeKey = FormComponentBuilder.createTypeKeyField(binder, i18n);
        this.smTypeAssignOptionGroup = smTypeComponentBuilder.createSmTypeAssignOptionGroup(binder);

        this.colorPickerComponent.getColorPickerBtn().setCaption(i18n.getMessage("label.choose.type.color"));
    }

    @Override
    protected FormLayout buildFormLayout() {
        final FormLayout formLayout = super.buildFormLayout();

        formLayout.addComponent(typeKey, formLayout.getComponentCount() - 1);
        formLayout.addComponent(smTypeAssignOptionGroup, formLayout.getComponentCount() - 1);

        return formLayout;
    }

    /**
     * Disable the software module type key text field
     */
    public void disableTypeKey() {
        typeKey.setEnabled(false);
    }

    /**
     * Disable the software module type assign option
     */
    public void disableTypeAssignOptionGroup() {
        smTypeAssignOptionGroup.setEnabled(false);
    }
}
