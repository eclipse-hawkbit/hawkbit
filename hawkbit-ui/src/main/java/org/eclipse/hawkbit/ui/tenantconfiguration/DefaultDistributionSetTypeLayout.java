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

import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.TenantMetaData;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.vaadin.server.FontAwesome;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;

/**
 * Default DistributionSet Panel.
 */
@SpringComponent
@ViewScope
public class DefaultDistributionSetTypeLayout extends BaseConfigurationView implements ConfigurationGroup {

    private static final long serialVersionUID = 17896542758L;

    @Autowired
    private transient SystemManagement systemManagement;

    @Autowired
    private transient DistributionSetManagement distributionSetManagement;

    @Autowired
    private I18N i18n;

    private Long currentDefaultDisSetType;

    private Long selectedDefaultDisSetType;

    private TenantMetaData tenantMetaData;

    private ComboBox combobox;

    private Label changeIcon;

    /**
     * Initialize Default Distribution Set layout.
     */
    @PostConstruct
    public void init() {

        final Panel rootPanel = new Panel();
        rootPanel.setSizeFull();
        rootPanel.addStyleName("config-panel");
        final VerticalLayout vlayout = new VerticalLayout();
        vlayout.setMargin(true);
        vlayout.setSizeFull();
        final String disSetTypeTitle = i18n.get("configuration.defaultdistributionset.title");

        final Label headerDisSetType = new Label(disSetTypeTitle);
        headerDisSetType.addStyleName("config-panel-header");
        vlayout.addComponent(headerDisSetType);
        final DistributionSetType currentDistributionSetType = getCurrentDistributionSetType();
        currentDefaultDisSetType = currentDistributionSetType.getId();

        final HorizontalLayout hlayout = new HorizontalLayout();
        hlayout.setSpacing(true);
        hlayout.setStyleName("config-h-panel");

        final Label configurationLabel = new Label(i18n.get("configuration.defaultdistributionset.select.label"));
        hlayout.addComponent(configurationLabel);
        hlayout.setComponentAlignment(configurationLabel, Alignment.MIDDLE_LEFT);

        final Pageable pageReq = new PageRequest(0, 100);
        final Iterable<DistributionSetType> distributionSetTypeCollection = distributionSetManagement
                .findDistributionSetTypesAll(pageReq);

        combobox = SPUIComponentProvider.getComboBox(null, "330", null, null, false, "", "label.combobox.tag");
        combobox.setId(UIComponentIdProvider.SYSTEM_CONFIGURATION_DEFAULTDIS_COMBOBOX);
        combobox.setNullSelectionAllowed(false);
        for (final DistributionSetType distributionSetType : distributionSetTypeCollection) {
            combobox.addItem(distributionSetType.getId());
            combobox.setItemCaption(distributionSetType.getId(),
                    distributionSetType.getKey() + " (" + distributionSetType.getName() + ")");

            if (distributionSetType.getId().equals(currentDistributionSetType.getId())) {
                combobox.select(distributionSetType.getId());
            }
        }
        combobox.setImmediate(true);
        combobox.addValueChangeListener(event -> selectDistributionSetValue());
        hlayout.addComponent(combobox);

        changeIcon = new Label();
        changeIcon.setIcon(FontAwesome.CHECK);
        hlayout.addComponent(changeIcon);
        changeIcon.setVisible(false);

        vlayout.addComponent(hlayout);
        rootPanel.setContent(vlayout);
        setCompositionRoot(rootPanel);

    }

    private DistributionSetType getCurrentDistributionSetType() {
        tenantMetaData = systemManagement.getTenantMetadata();
        return tenantMetaData.getDefaultDsType();
    }

    @Override
    public void save() {
        if (!currentDefaultDisSetType.equals(selectedDefaultDisSetType) && selectedDefaultDisSetType != null) {
            final DistributionSetType defaultDistributionSetType = distributionSetManagement
                    .findDistributionSetTypeById(selectedDefaultDisSetType);
            tenantMetaData.setDefaultDsType(defaultDistributionSetType);
            tenantMetaData = systemManagement.updateTenantMetadata(tenantMetaData);
            currentDefaultDisSetType = selectedDefaultDisSetType;
        }
        changeIcon.setVisible(false);
    }

    @Override
    public void undo() {
        combobox.select(currentDefaultDisSetType);
        selectedDefaultDisSetType = currentDefaultDisSetType;
        changeIcon.setVisible(false);
    }

    /**
     * Method that is called when combobox event is performed.
     */
    public void selectDistributionSetValue() {
        selectedDefaultDisSetType = (Long) combobox.getValue();
        if (!selectedDefaultDisSetType.equals(currentDefaultDisSetType)) {
            changeIcon.setVisible(true);
            notifyConfigurationChanged();
        } else {
            changeIcon.setVisible(false);
        }
    }

}
