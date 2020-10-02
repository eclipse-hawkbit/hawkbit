/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.distributions.disttype;

import org.eclipse.hawkbit.repository.SoftwareModuleTypeManagement;
import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.builder.FormComponentBuilder;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyType;
import org.eclipse.hawkbit.ui.management.tag.TagWindowLayout;

import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.TextField;

/**
 * Distribution set window layout
 */
public class DsTypeWindowLayout extends TagWindowLayout<ProxyType> {
    private final DsTypeWindowLayoutComponentBuilder dsTypeComponentBuilder;

    private final TextField typeKey;
    private final DsTypeSmSelectLayout dsTypeSmSelectLayout;

    /**
     * Constructor for DsTypeWindowLayout
     *
     * @param uiDependencies
     *            {@link CommonUiDependencies}
     * @param softwareModuleTypeManagement
     *            SoftwareModuleTypeManagement
     */
    public DsTypeWindowLayout(final CommonUiDependencies uiDependencies,
            final SoftwareModuleTypeManagement softwareModuleTypeManagement) {
        super(uiDependencies);

        this.dsTypeComponentBuilder = new DsTypeWindowLayoutComponentBuilder(i18n, softwareModuleTypeManagement);

        this.typeKey = FormComponentBuilder.createTypeKeyField(binder, i18n);
        this.dsTypeSmSelectLayout = dsTypeComponentBuilder.createDsTypeSmSelectLayout(binder);

        this.colorPickerComponent.getColorPickerBtn().setCaption(i18n.getMessage("label.choose.type.color"));
    }

    @Override
    protected FormLayout buildFormLayout() {
        final FormLayout formLayout = super.buildFormLayout();

        formLayout.addComponent(typeKey, formLayout.getComponentCount() - 1);

        return formLayout;
    }

    @Override
    public ComponentContainer getRootComponent() {
        final ComponentContainer rootLayout = super.getRootComponent();

        rootLayout.addComponent(dsTypeSmSelectLayout);

        return rootLayout;
    }

    /**
     * Disable the key text file
     */
    public void disableTypeKey() {
        typeKey.setEnabled(false);
    }

    /**
     * Disable the selection of distribution set type software module layout
     */
    public void disableDsTypeSmSelectLayout() {
        dsTypeSmSelectLayout.setEnabled(false);
    }
}
