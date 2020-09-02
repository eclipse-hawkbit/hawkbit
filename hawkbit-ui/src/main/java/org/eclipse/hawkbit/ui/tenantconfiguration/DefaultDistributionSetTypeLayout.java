/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.tenantconfiguration;

import org.eclipse.hawkbit.repository.DistributionSetTypeManagement;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.common.builder.LabelBuilder;
import org.eclipse.hawkbit.ui.common.data.mappers.TypeToProxyTypeMapper;
import org.eclipse.hawkbit.ui.common.data.providers.DistributionSetTypeDataProvider;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxySystemConfigWindow;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyType;
import org.eclipse.hawkbit.ui.tenantconfiguration.window.SystemConfigWindowDependencies;
import org.eclipse.hawkbit.ui.tenantconfiguration.window.SystemConfigWindowLayoutComponentBuilder;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;

import com.vaadin.data.Binder;
import com.vaadin.data.HasValue;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;

/**
 * Default DistributionSet Panel.
 */
public class DefaultDistributionSetTypeLayout extends CustomComponent {

    private static final long serialVersionUID = 1L;

    private final VaadinMessageSource i18n;

    private final SpPermissionChecker permissionChecker;
    private Long currentDefaultDistSetTypeId;
    private ComboBox<ProxyType> dsSetTypeComboBox = new ComboBox<>();
    private final Binder<ProxySystemConfigWindow> binder;
    private final transient SystemConfigWindowLayoutComponentBuilder builder;
    private Label changeIcon;

    DefaultDistributionSetTypeLayout(final SystemManagement systemManagement, final VaadinMessageSource i18n,
            final SpPermissionChecker permChecker, final DistributionSetTypeManagement dsTypeManagement,
            final Binder<ProxySystemConfigWindow> binder) {
        this.i18n = i18n;
        this.permissionChecker = permChecker;
        this.binder = binder;
        final DistributionSetTypeDataProvider<ProxyType> dataProvider = new DistributionSetTypeDataProvider<>(
                dsTypeManagement, new TypeToProxyTypeMapper<>());
        final SystemConfigWindowDependencies dependencies = new SystemConfigWindowDependencies(systemManagement, i18n,
                permChecker, dsTypeManagement, dataProvider);
        this.builder = new SystemConfigWindowLayoutComponentBuilder(dependencies);

        initDsSetTypeComponent();
    }

    private void initDsSetTypeComponent() {
        if (!permissionChecker.hasReadRepositoryPermission()) {
            return;
        }
        final Panel rootPanel = new Panel();
        rootPanel.setSizeFull();
        rootPanel.addStyleName("config-panel");
        final VerticalLayout vlayout = new VerticalLayout();
        vlayout.setSpacing(false);
        vlayout.setMargin(true);
        vlayout.setSizeFull();

        final Label header = new Label(i18n.getMessage("configuration.defaultdistributionset.title"));
        header.addStyleName("config-panel-header");
        vlayout.addComponent(header);

        currentDefaultDistSetTypeId = getCurrentDistributionSetTypeId();

        final HorizontalLayout hlayout = new HorizontalLayout();
        hlayout.setSpacing(true);

        final Label configurationLabel = new LabelBuilder()
                .name(i18n.getMessage("configuration.defaultdistributionset.select.label")).buildLabel();
        hlayout.addComponent(configurationLabel);

        initDsSetTypeComboBox();
        hlayout.addComponent(dsSetTypeComboBox);

        changeIcon = new Label();
        changeIcon.setIcon(VaadinIcons.CHECK);
        hlayout.addComponent(changeIcon);
        changeIcon.setVisible(false);

        vlayout.addComponent(hlayout);
        rootPanel.setContent(vlayout);
        setCompositionRoot(rootPanel);
    }

    private void initDsSetTypeComboBox() {
        dsSetTypeComboBox = builder.createDistributionSetTypeCombo(binder);
        dsSetTypeComboBox.addValueChangeListener(this::selectDistributionSetTypeValue);
    }

    private Long getCurrentDistributionSetTypeId() {
        return binder.getBean().getDistributionSetTypeId();
    }

    /**
     * Method that is called when combobox event is performed.
     *
     * @param event
     */
    private void selectDistributionSetTypeValue(final HasValue.ValueChangeEvent<ProxyType> event) {
        changeIcon.setVisible(!event.getValue().getId().equals(currentDefaultDistSetTypeId));
    }

}
