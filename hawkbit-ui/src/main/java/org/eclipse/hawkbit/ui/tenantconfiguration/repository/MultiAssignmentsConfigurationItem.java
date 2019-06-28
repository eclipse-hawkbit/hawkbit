/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.tenantconfiguration.repository;

import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey;
import org.eclipse.hawkbit.ui.common.builder.LabelBuilder;
import org.eclipse.hawkbit.ui.tenantconfiguration.generic.AbstractBooleanTenantConfigurationItem;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;

import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

/**
 * This class represents the UI item for enabling /disabling the
 * Multi-Assignments feature as part of the repository configuration view.
 */
public class MultiAssignmentsConfigurationItem extends AbstractBooleanTenantConfigurationItem {

    private static final long serialVersionUID = 1L;

    private static final String MSG_KEY_CHECKBOX = "label.configuration.repository.multiassignments";
    private static final String MSG_KEY_NOTICE = "label.configuration.repository.multiassignments.notice";

    private final VerticalLayout container;
    private final VaadinMessageSource i18n;

    private boolean isMultiAssignmentsEnabled;
    private boolean multiAssignmentsEnabledChanged;

    /**
     * Constructor.
     * 
     * @param tenantConfigurationManagement
     *            to read /write tenant-specific configuration properties
     * @param i18n
     *            to obtain localized strings
     */
    public MultiAssignmentsConfigurationItem(final TenantConfigurationManagement tenantConfigurationManagement,
            final VaadinMessageSource i18n) {
        super(TenantConfigurationKey.MULTI_ASSIGNMENTS_ENABLED, tenantConfigurationManagement, i18n);
        this.i18n = i18n;

        super.init(MSG_KEY_CHECKBOX);
        isMultiAssignmentsEnabled = isConfigEnabled();

        container = new VerticalLayout();
        container.setImmediate(true);

        container.addComponent(newLabel(MSG_KEY_NOTICE));

        if (isMultiAssignmentsEnabled) {
            setSettingsVisible(isMultiAssignmentsEnabled);
        }

    }

    @Override
    public void configEnable() {
        if (!isMultiAssignmentsEnabled) {
            multiAssignmentsEnabledChanged = true;
        }
        isMultiAssignmentsEnabled = true;
        setSettingsVisible(true);
    }

    @Override
    public void configDisable() {
        if (isMultiAssignmentsEnabled) {
            multiAssignmentsEnabledChanged = true;
        }
        isMultiAssignmentsEnabled = false;
        setSettingsVisible(false);
    }

    @Override
    public void save() {
        if (!multiAssignmentsEnabledChanged) {
            return;
        }
        getTenantConfigurationManagement().addOrUpdateConfiguration(getConfigurationKey(), isMultiAssignmentsEnabled);
    }

    @Override
    public void undo() {
        multiAssignmentsEnabledChanged = false;
        isMultiAssignmentsEnabled = getTenantConfigurationManagement()
                .getConfigurationValue(getConfigurationKey(), Boolean.class).getValue();
    }

    private void setSettingsVisible(final boolean visible) {
        if (visible) {
            addComponent(container);
        } else {
            removeComponent(container);
        }
    }

    private Label newLabel(final String msgKey) {
        final Label label = new LabelBuilder().name(i18n.getMessage(msgKey)).buildLabel();
        label.setWidthUndefined();
        return label;
    }

}
