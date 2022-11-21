/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.filtermanagement;

import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.ui.common.AbstractEntityWindowLayout;
import org.eclipse.hawkbit.ui.common.builder.BoundComponent;
import org.eclipse.hawkbit.ui.common.data.mappers.DistributionSetToProxyDistributionMapper;
import org.eclipse.hawkbit.ui.common.data.providers.DistributionSetStatelessDataProvider;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyDistributionSet;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTargetFilterQuery;
import org.eclipse.hawkbit.ui.management.miscs.ActionTypeOptionGroupAutoAssignmentLayout;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;

import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import org.eclipse.hawkbit.utils.TenantConfigHelper;

/**
 * Target add/update window layout.
 */
public class AutoAssignmentWindowLayout extends AbstractEntityWindowLayout<ProxyTargetFilterQuery> {
    private final AutoAssignmentWindowLayoutComponentBuilder autoAssignComponentBuilder;

    private final Label descriptionLabel;
    private final CheckBox enableCheckBox;
    private final CheckBox confirmationCheckBox;
    private final ActionTypeOptionGroupAutoAssignmentLayout actionTypeOptionGroupLayout;
    private final BoundComponent<ComboBox<ProxyDistributionSet>> autoAssignDsComboBox;
    
    private final TenantConfigHelper configHelper;
    
    /**
     * Constructor for AbstractTagWindowLayout
     * 
     * @param i18n
     *            VaadinMessageSource
     * @param dsManagement
     *            DistributionSetManagement
     * @param configHelper
     *            TenantConfigHelper
     */
    public AutoAssignmentWindowLayout(final VaadinMessageSource i18n, final DistributionSetManagement dsManagement,
            final TenantConfigHelper configHelper) {
        super();

        this.autoAssignComponentBuilder = new AutoAssignmentWindowLayoutComponentBuilder(i18n);

        this.descriptionLabel = autoAssignComponentBuilder.createDescriptionLabel();
        this.enableCheckBox = autoAssignComponentBuilder.createEnableCheckbox(binder);
        this.confirmationCheckBox = autoAssignComponentBuilder.createConfirmationCheckbox(binder);
        this.actionTypeOptionGroupLayout = autoAssignComponentBuilder.createActionTypeOptionGroupLayout(binder);
        this.autoAssignDsComboBox = autoAssignComponentBuilder.createDistributionSetCombo(binder,
                new DistributionSetStatelessDataProvider(dsManagement, new DistributionSetToProxyDistributionMapper()));
        this.configHelper = configHelper;

        addValueChangeListeners();
    }

    @Override
    public ComponentContainer getRootComponent() {
        final VerticalLayout autoAssignmentLayout = new VerticalLayout();
        autoAssignmentLayout.setSpacing(true);
        autoAssignmentLayout.setMargin(true);

        autoAssignmentLayout.addComponent(descriptionLabel);
        autoAssignmentLayout.addComponent(enableCheckBox);
        autoAssignmentLayout.addComponent(actionTypeOptionGroupLayout);
        autoAssignmentLayout.addComponent(autoAssignDsComboBox.getComponent());
        
        if (configHelper.isConfirmationFlowEnabled()) {
            autoAssignmentLayout.addComponent(confirmationCheckBox);
        }
        
        return autoAssignmentLayout;
    }

    private void addValueChangeListeners() {
        enableCheckBox.addValueChangeListener(event -> switchAutoAssignmentInputsVisibility(event.getValue()));
    }

    /**
     * Toggle auto assign input option
     *
     * @param autoAssignmentEnabled
     *          boolean
     */
    public void switchAutoAssignmentInputsVisibility(final boolean autoAssignmentEnabled) {
        actionTypeOptionGroupLayout.setVisible(autoAssignmentEnabled);
        autoAssignDsComboBox.getComponent().setVisible(autoAssignmentEnabled);
        autoAssignDsComboBox.setRequired(autoAssignmentEnabled);
        confirmationCheckBox.setVisible(autoAssignmentEnabled);
    }
}
