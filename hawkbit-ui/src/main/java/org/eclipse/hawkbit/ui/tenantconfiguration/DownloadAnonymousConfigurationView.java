/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.tenantconfiguration;

import javax.annotation.PostConstruct;

import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.model.TenantConfigurationValue;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationKey;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;

/**
 * View to enable anonymous download.
 */
@SpringComponent
@ViewScope
public class DownloadAnonymousConfigurationView extends BaseConfigurationView
        implements ConfigurationItem.ConfigurationItemChangeListener {

    private static final String DIST_CHECKBOX_STYLE = "dist-checkbox-style";

    private static final long serialVersionUID = 1L;

    @Autowired
    private I18N i18n;

    @Autowired
    private transient TenantConfigurationManagement tenantConfigurationManagement;

    boolean anonymousDownloadEnabled;

    private CheckBox downloadAnonymousCheckBox;

    /**
     * Initialize Default Download Anonymous layout.
     */
    @PostConstruct
    public void init() {

        final TenantConfigurationValue<Boolean> value = tenantConfigurationManagement
                .getConfigurationValue(TenantConfigurationKey.ANONYMOUS_DOWNLOAD_MODE_ENABLED, Boolean.class);
        anonymousDownloadEnabled = value.getValue();

        final Panel rootPanel = new Panel();
        rootPanel.setSizeFull();
        rootPanel.addStyleName("config-panel");

        final VerticalLayout vLayout = new VerticalLayout();
        vLayout.setMargin(true);
        vLayout.setSizeFull();

        final Label headerDisSetType = new Label(i18n.get("enonymous.download.title"));
        headerDisSetType.addStyleName("config-panel-header");
        vLayout.addComponent(headerDisSetType);

        final GridLayout gridLayout = new GridLayout(2, 1);
        gridLayout.setSpacing(true);
        gridLayout.setImmediate(true);
        gridLayout.setColumnExpandRatio(1, 1.0F);
        gridLayout.setSizeFull();

        downloadAnonymousCheckBox = SPUIComponentProvider.getCheckBox("", DIST_CHECKBOX_STYLE, null, false, "");
        downloadAnonymousCheckBox.setValue(anonymousDownloadEnabled);
        downloadAnonymousCheckBox.addValueChangeListener(event -> configurationHasChanged());
        gridLayout.addComponent(downloadAnonymousCheckBox);

        final Label configurationLabel = SPUIComponentProvider.getLabel(i18n.get("enonymous.download.label"),
                SPUILabelDefinitions.SP_LABEL_SIMPLE);
        gridLayout.addComponent(configurationLabel);
        gridLayout.setComponentAlignment(configurationLabel, Alignment.MIDDLE_LEFT);

        vLayout.addComponent(gridLayout);

        rootPanel.setContent(vLayout);
        setCompositionRoot(rootPanel);
    }

    @Override
    public void configurationHasChanged() {
        anonymousDownloadEnabled = downloadAnonymousCheckBox.getValue();
        notifyConfigurationChanged();
    }

    @Override
    public void save() {
        tenantConfigurationManagement.addOrUpdateConfiguration(TenantConfigurationKey.ANONYMOUS_DOWNLOAD_MODE_ENABLED,
                downloadAnonymousCheckBox.getValue());
    }

    @Override
    public void undo() {
        anonymousDownloadEnabled = tenantConfigurationManagement
                .getGlobalConfigurationValue(TenantConfigurationKey.ANONYMOUS_DOWNLOAD_MODE_ENABLED, Boolean.class);
        downloadAnonymousCheckBox.setValue(anonymousDownloadEnabled);

    }

}
