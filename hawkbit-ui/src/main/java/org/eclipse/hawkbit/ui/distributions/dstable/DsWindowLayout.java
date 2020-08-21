/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.distributions.dstable;

import org.eclipse.hawkbit.repository.DistributionSetTypeManagement;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey;
import org.eclipse.hawkbit.ui.common.AbstractEntityWindowLayout;
import org.eclipse.hawkbit.ui.common.data.mappers.TypeToTypeInfoMapper;
import org.eclipse.hawkbit.ui.common.data.providers.DistributionSetTypeDataProvider;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyDistributionSet;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTypeInfo;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;

import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;

/**
 * Target add/update window layout.
 */
public class DsWindowLayout extends AbstractEntityWindowLayout<ProxyDistributionSet> {
    private final SystemSecurityContext systemSecurityContext;
    private final TenantConfigurationManagement tenantConfigurationManagement;

    private final DsWindowLayoutComponentBuilder dsComponentBuilder;

    private final ComboBox<ProxyTypeInfo> dsTypeSelect;
    private final TextField dsName;
    private final TextField dsVersion;
    private final TextArea dsDescription;
    private final CheckBox dsMigrationStepRequired;

    /**
     * Constructor for AbstractTagWindowLayout
     * 
     * @param i18n
     *          VaadinMessageSource
     * @param systemSecurityContext
     *          SystemSecurityContext
     * @param tenantConfigurationManagement
     *          TenantConfigurationManagement
     * @param dsTypeManagement
     *          DistributionSetTypeManagement
     */
    public DsWindowLayout(final VaadinMessageSource i18n, final SystemSecurityContext systemSecurityContext,
            final TenantConfigurationManagement tenantConfigurationManagement,
            final DistributionSetTypeManagement dsTypeManagement) {
        super();

        this.systemSecurityContext = systemSecurityContext;
        this.tenantConfigurationManagement = tenantConfigurationManagement;

        final DistributionSetTypeDataProvider<ProxyTypeInfo> dsTypeDataProvider = new DistributionSetTypeDataProvider<>(
                dsTypeManagement, new TypeToTypeInfoMapper<DistributionSetType>());
        this.dsComponentBuilder = new DsWindowLayoutComponentBuilder(i18n, dsTypeDataProvider);

        this.dsTypeSelect = dsComponentBuilder.createDistributionSetTypeCombo(binder);
        this.dsName = dsComponentBuilder.createNameField(binder);
        this.dsVersion = dsComponentBuilder.createVersionField(binder);
        this.dsDescription = dsComponentBuilder.createDescription(binder);
        this.dsMigrationStepRequired = isMultiAssignmentEnabled() ? null
                : dsComponentBuilder.createMigrationStepField(binder);
    }

    private boolean isMultiAssignmentEnabled() {
        return systemSecurityContext.runAsSystem(() -> tenantConfigurationManagement
                .getConfigurationValue(TenantConfigurationKey.MULTI_ASSIGNMENTS_ENABLED, Boolean.class).getValue());
    }

    @Override
    public ComponentContainer getRootComponent() {
        final FormLayout dsWindowLayout = new FormLayout();

        dsWindowLayout.setSpacing(true);
        dsWindowLayout.setMargin(true);
        dsWindowLayout.setSizeUndefined();

        dsWindowLayout.addComponent(dsTypeSelect);

        dsWindowLayout.addComponent(dsName);
        dsName.focus();

        dsWindowLayout.addComponent(dsVersion);

        dsWindowLayout.addComponent(dsDescription);

        if (dsMigrationStepRequired != null) {
            dsWindowLayout.addComponent(dsMigrationStepRequired);
        }

        return dsWindowLayout;
    }

    /**
     * Disable the distribution set type selection
     */
    public void disableDsTypeSelect() {
        dsTypeSelect.setEnabled(false);
    }
}
