/** Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.tenantconfiguration.window;

import org.eclipse.hawkbit.ui.common.data.proxies.ProxySystemConfigWindow;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyType;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;

import com.vaadin.data.Binder;
import com.vaadin.server.Sizeable.Unit;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Builder for system config window layout component
 */
public class SystemConfigWindowLayoutComponentBuilder {
    private final SystemConfigWindowDependencies dependencies;

    /**
     * Constructor for SystemConfigWindowLayoutComponentBuilder
     *
     * @param dependencies
     *            SystemConfigWindowDependencies
     */
    public SystemConfigWindowLayoutComponentBuilder(final SystemConfigWindowDependencies dependencies) {
        this.dependencies = dependencies;
    }

    /**
     * Create the distribution set type combo
     *
     * @param binder
     *            System config window binder
     *
     * @return Distribution set type combo box
     */
    public ComboBox<ProxyType> createDistributionSetTypeCombo(final Binder<ProxySystemConfigWindow> binder) {
        final ComboBox<ProxyType> distributionSetType = SPUIComponentProvider.getComboBox(
                UIComponentIdProvider.SYSTEM_CONFIGURATION_DEFAULTDIS_COMBOBOX, null,
                dependencies.getI18n().getMessage("caption.type"), null, false, ProxyType::getKeyAndName,
                dependencies.getDistributionSetTypeDataProvider());
        distributionSetType.removeStyleName(ValoTheme.COMBOBOX_SMALL);
        distributionSetType.addStyleName(ValoTheme.COMBOBOX_TINY);
        distributionSetType.setWidth(330f, Unit.PIXELS);

        binder.forField(distributionSetType).withConverter(dstType -> {
            if (dstType == null) {
                return null;
            }

            return dstType.getId();
        }, dstTypeId -> {
            if (dstTypeId == null) {
                return null;
            }

            final ProxyType dstType = new ProxyType();
            dstType.setId(dstTypeId);

            return dstType;
        }).bind(ProxySystemConfigWindow::getDistributionSetTypeId, ProxySystemConfigWindow::setDistributionSetTypeId);

        return distributionSetType;
    }
}
