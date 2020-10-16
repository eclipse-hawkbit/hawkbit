/** 
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.tenantconfiguration.window;

import org.eclipse.hawkbit.ui.common.data.proxies.ProxySystemConfigDsType;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTypeInfo;
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
    public ComboBox<ProxyTypeInfo> createDistributionSetTypeCombo(final Binder<ProxySystemConfigDsType> binder) {
        final ComboBox<ProxyTypeInfo> dsTypeCombo = SPUIComponentProvider.getComboBox(
                UIComponentIdProvider.SYSTEM_CONFIGURATION_DEFAULTDIS_COMBOBOX, null,
                dependencies.getI18n().getMessage("caption.type"), null, false, ProxyTypeInfo::getKeyAndName,
                dependencies.getDistributionSetTypeDataProvider());
        dsTypeCombo.removeStyleName(ValoTheme.COMBOBOX_SMALL);
        dsTypeCombo.addStyleName(ValoTheme.COMBOBOX_TINY);
        dsTypeCombo.setWidth(330.0F, Unit.PIXELS);

        binder.forField(dsTypeCombo).bind(ProxySystemConfigDsType::getDsTypeInfo,
                ProxySystemConfigDsType::setDsTypeInfo);

        return dsTypeCombo;
    }
}
