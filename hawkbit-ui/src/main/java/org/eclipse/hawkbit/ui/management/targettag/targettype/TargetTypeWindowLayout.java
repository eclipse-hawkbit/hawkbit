/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.targettag.targettype;

import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.FormLayout;
import org.eclipse.hawkbit.repository.DistributionSetTypeManagement;
import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyType;
import org.eclipse.hawkbit.ui.management.tag.TagWindowLayout;

/**
 * Distribution set window layout
 */
public class TargetTypeWindowLayout extends TagWindowLayout<ProxyType> {
    private final TargetTypeWindowLayoutComponentBuilder targetTypeWindowLayoutComponentBuilder;

    private final TargetTypeDsTypeSelectLayout targetTypeDsTypeSelectLayout;

    /**
     * Constructor for TargetTypeWindowLayout
     *
     * @param uiDependencies
     *            {@link CommonUiDependencies}
     * @param distributionSetTypeManagement
     *            DistributionSetTypeManagement
     */
    public TargetTypeWindowLayout(final CommonUiDependencies uiDependencies,
                                  final DistributionSetTypeManagement distributionSetTypeManagement) {
        super(uiDependencies);

        this.targetTypeWindowLayoutComponentBuilder = new TargetTypeWindowLayoutComponentBuilder(i18n, distributionSetTypeManagement);

        this.targetTypeDsTypeSelectLayout = targetTypeWindowLayoutComponentBuilder.createTargetTypeDsSelectLayout(binder);

        this.colorPickerComponent.getColorPickerBtn().setCaption(i18n.getMessage("label.choose.type.color"));
    }

    @Override
    protected FormLayout buildFormLayout() {
        final FormLayout formLayout = super.buildFormLayout();
        return formLayout;
    }

    @Override
    public ComponentContainer getRootComponent() {
        final ComponentContainer rootLayout = super.getRootComponent();

        rootLayout.addComponent(targetTypeDsTypeSelectLayout);

        return rootLayout;
    }

    /**
     * Disable the selection of distribution set type software module layout
     */
    public void disableTargetTypeDsSelectLayout() {
        targetTypeDsTypeSelectLayout.setEnabled(false);
    }
}
