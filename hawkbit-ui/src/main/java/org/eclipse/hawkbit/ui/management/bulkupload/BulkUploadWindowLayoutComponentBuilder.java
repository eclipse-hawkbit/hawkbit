/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.bulkupload;

import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.TargetTypeManagement;
import org.eclipse.hawkbit.ui.common.builder.BoundComponent;
import org.eclipse.hawkbit.ui.common.builder.FormComponentBuilder;
import org.eclipse.hawkbit.ui.common.data.mappers.DistributionSetToProxyDistributionMapper;
import org.eclipse.hawkbit.ui.common.data.mappers.TargetTypeToTypeInfoMapper;
import org.eclipse.hawkbit.ui.common.data.providers.DistributionSetStatelessDataProvider;
import org.eclipse.hawkbit.ui.common.data.providers.TargetTypeDataProvider;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyBulkUploadWindow;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyDistributionSet;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTypeInfo;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;

import com.vaadin.data.Binder;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.TextArea;

/**
 * Builder for bulk upload window components.
 */
public final class BulkUploadWindowLayoutComponentBuilder {

    private final VaadinMessageSource i18n;
    private final DistributionSetStatelessDataProvider distributionSetDataProvider;

    private final TargetTypeDataProvider<ProxyTypeInfo> targetTypeDataProvider;

    /**
     * Constructor
     * 
     * @param i18n
     *            i18n
     * @param distributionSetManagement
     *            to build DistributionSet ComboBox
     * @param targetTypeManagement
     *            TargetTypeManagement
     */
    public BulkUploadWindowLayoutComponentBuilder(final VaadinMessageSource i18n,
                                                  final DistributionSetManagement distributionSetManagement,
                                                  final TargetTypeManagement targetTypeManagement) {
        this.i18n = i18n;

        this.targetTypeDataProvider = new TargetTypeDataProvider<>(
                targetTypeManagement, new TargetTypeToTypeInfoMapper());

        this.distributionSetDataProvider = new DistributionSetStatelessDataProvider(distributionSetManagement,
                new DistributionSetToProxyDistributionMapper());
    }

    /**
     * create optional Distribution Set ComboBox
     * 
     * @param binder
     *            binder the input will be bound to
     * @return ComboBox
     */
    public ComboBox<ProxyDistributionSet> createDistributionSetCombo(final Binder<ProxyBulkUploadWindow> binder) {
        final BoundComponent<ComboBox<ProxyDistributionSet>> boundComboBox = FormComponentBuilder
                .createDistributionSetComboBox(binder, distributionSetDataProvider, i18n,
                        UIComponentIdProvider.DIST_SET_SELECT_COMBO_ID);
        boundComboBox.setRequired(false);

        final ComboBox<ProxyDistributionSet> comboBox = boundComboBox.getComponent();
        comboBox.setEmptySelectionAllowed(true);
        comboBox.setSizeFull();

        return comboBox;
    }

    /**
     * create optional Target Type ComboBox
     *
     * @param binder
     *            binder the input will be bound to
     * @return ComboBox
     */
    public ComboBox<ProxyTypeInfo> createTargetTypeCombo(final Binder<ProxyBulkUploadWindow> binder) {
        final BoundComponent<ComboBox<ProxyTypeInfo>> boundComboBox = FormComponentBuilder
                .createTypeCombo(binder, targetTypeDataProvider, i18n, UIComponentIdProvider.TARGET_ADD_TARGETTYPE, false);
        final ComboBox<ProxyTypeInfo> comboBox = boundComboBox.getComponent();
        comboBox.setSizeFull();
        return comboBox;
    }

    /**
     * create description field
     * 
     * @param binder
     *            binder the input will be bound to
     * @return input component
     */
    public TextArea createDescriptionField(final Binder<ProxyBulkUploadWindow> binder) {
        final TextArea description = FormComponentBuilder
                .createDescriptionInput(binder, i18n, UIComponentIdProvider.TARGET_ADD_DESC).getComponent();
        description.setWidthFull();

        return description;
    }
}
