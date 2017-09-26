/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.tenantconfiguration;

import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.tenantconfiguration.generic.BooleanConfigurationItem;
import org.eclipse.hawkbit.ui.tenantconfiguration.repository.ActionAutocloseConfigurationItem;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;

/**
 * View to configure the authentication mode.
 */
public class RepositoryConfigurationView extends BaseConfigurationView
        implements ConfigurationGroup, ConfigurationItem.ConfigurationItemChangeListener, ValueChangeListener {

    private static final String DIST_CHECKBOX_STYLE = "dist-checkbox-style";

    private static final long serialVersionUID = 1L;

    private final VaadinMessageSource i18n;

    private final ActionAutocloseConfigurationItem actionAutocloseConfigurationItem;

    private CheckBox actionAutocloseCheckBox;

    RepositoryConfigurationView(final VaadinMessageSource i18n,
            final TenantConfigurationManagement tenantConfigurationManagement) {
        this.i18n = i18n;
        this.actionAutocloseConfigurationItem = new ActionAutocloseConfigurationItem(tenantConfigurationManagement,
                i18n);

        init();
    }

    private void init() {

        final Panel rootPanel = new Panel();
        rootPanel.setSizeFull();

        rootPanel.addStyleName("config-panel");

        final VerticalLayout vLayout = new VerticalLayout();
        vLayout.setMargin(true);
        vLayout.setSizeFull();

        final Label headerDisSetType = new Label(i18n.getMessage("configuration.repository.title"));
        headerDisSetType.addStyleName("config-panel-header");
        vLayout.addComponent(headerDisSetType);

        final GridLayout gridLayout = new GridLayout(2, 1);
        gridLayout.setSpacing(true);
        gridLayout.setImmediate(true);
        gridLayout.setColumnExpandRatio(1, 1.0F);
        gridLayout.setSizeFull();

        actionAutocloseCheckBox = SPUIComponentProvider.getCheckBox("", DIST_CHECKBOX_STYLE, null, false, "");
        actionAutocloseCheckBox.setId(UIComponentIdProvider.REPOSITORY_ACTIONS_AUTOCLOSE_CHECKBOX);
        actionAutocloseCheckBox.setValue(actionAutocloseConfigurationItem.isConfigEnabled());
        actionAutocloseCheckBox.addValueChangeListener(this);
        actionAutocloseConfigurationItem.addChangeListener(this);
        gridLayout.addComponent(actionAutocloseCheckBox, 0, 0);
        gridLayout.addComponent(actionAutocloseConfigurationItem, 1, 0);

        vLayout.addComponent(gridLayout);
        rootPanel.setContent(vLayout);
        setCompositionRoot(rootPanel);
    }

    @Override
    public void save() {
        actionAutocloseConfigurationItem.save();
    }

    @Override
    public void undo() {
        actionAutocloseConfigurationItem.undo();
        actionAutocloseCheckBox.setValue(actionAutocloseConfigurationItem.isConfigEnabled());
    }

    @Override
    public void configurationHasChanged() {
        notifyConfigurationChanged();
    }

    @Override
    public void valueChange(final ValueChangeEvent event) {

        if (!(event.getProperty() instanceof CheckBox)) {
            return;
        }

        notifyConfigurationChanged();

        final CheckBox checkBox = (CheckBox) event.getProperty();
        BooleanConfigurationItem configurationItem;

        if (actionAutocloseCheckBox.equals(checkBox)) {
            configurationItem = actionAutocloseConfigurationItem;
        } else {
            return;
        }

        if (checkBox.getValue()) {
            configurationItem.configEnable();
        } else {
            configurationItem.configDisable();
        }
    }
}
