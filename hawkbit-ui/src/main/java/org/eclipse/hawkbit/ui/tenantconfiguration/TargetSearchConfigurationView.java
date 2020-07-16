/**
 * Copyright (c) 2020 devolo AG and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.tenantconfiguration;

import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.ui.UiProperties;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.tenantconfiguration.search.TargetSearchConfigurationItem;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;

import com.vaadin.data.Property;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;

/**
 * Provides configuration for target search option
 * to include/exclude target attributes for the search.
 */
public class TargetSearchConfigurationView extends BaseConfigurationView
        implements Property.ValueChangeListener, ConfigurationItem.ConfigurationItemChangeListener {

    private static final long serialVersionUID = 1L;

    private final TargetSearchConfigurationItem targetSearchConfigurationItem;
    private final VaadinMessageSource i18n;
    private final UiProperties uiProperties;
    private CheckBox attributeSearchCheckbox;

    TargetSearchConfigurationView(final VaadinMessageSource i18n,
                                  final TenantConfigurationManagement tenantConfigurationManagement, final UiProperties uiProperties) {
        this.i18n = i18n;
        this.uiProperties = uiProperties;
        this.targetSearchConfigurationItem = new TargetSearchConfigurationItem(tenantConfigurationManagement, i18n);
        this.init();
    }

    private void init() {

        final Panel rootPanel = new Panel();
        rootPanel.setSizeFull();

        rootPanel.addStyleName("config-panel");

        final VerticalLayout vLayout = new VerticalLayout();
        vLayout.setMargin(true);
        vLayout.setSizeFull();

        final Label header = new Label(i18n.getMessage("configuration.targetsearch.title"));
        header.addStyleName("config-panel-header");
        vLayout.addComponent(header);

        final GridLayout gridLayout = new GridLayout(3, 1);
        gridLayout.setSpacing(true);
        gridLayout.setImmediate(true);
        gridLayout.setColumnExpandRatio(1, 1.0F);
        gridLayout.setSizeFull();

        attributeSearchCheckbox = SPUIComponentProvider.getCheckBox("", "", null, false, "");
        attributeSearchCheckbox.setId(UIComponentIdProvider.TARGET_SEARCH_ATTRIBUTES);
        attributeSearchCheckbox.setValue(targetSearchConfigurationItem.isConfigEnabled());
        attributeSearchCheckbox.addValueChangeListener(this);
        targetSearchConfigurationItem.addChangeListener(this);
        gridLayout.addComponent(attributeSearchCheckbox, 0, 0);
        gridLayout.addComponent(targetSearchConfigurationItem, 1, 0);

        vLayout.addComponent(gridLayout);
        rootPanel.setContent(vLayout);
        setCompositionRoot(rootPanel);
    }

    @Override
    public void save() {
        this.targetSearchConfigurationItem.save();
    }

    @Override
    public void undo() {
        this.targetSearchConfigurationItem.undo();
        this.attributeSearchCheckbox.setValue(targetSearchConfigurationItem.isConfigEnabled());
    }

    @Override
    public void valueChange(final Property.ValueChangeEvent event) {
        if (attributeSearchCheckbox.equals(event.getProperty())) {
            if (attributeSearchCheckbox.getValue()) {
                targetSearchConfigurationItem.configEnable();
            } else {
                targetSearchConfigurationItem.configDisable();
            }
            notifyConfigurationChanged();
        }
    }

    @Override
    public void configurationHasChanged() {
        notifyConfigurationChanged();
    }
}
