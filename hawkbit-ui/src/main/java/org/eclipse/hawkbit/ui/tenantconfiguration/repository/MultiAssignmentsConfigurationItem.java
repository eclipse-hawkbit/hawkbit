/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.tenantconfiguration.repository;

import org.eclipse.hawkbit.ui.common.data.proxies.ProxySystemConfigRepository;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;

import com.vaadin.data.Binder;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

/**
 * This class represents the UI item for enabling /disabling the
 * Multi-Assignments feature as part of the repository configuration view.
 */
public class MultiAssignmentsConfigurationItem extends VerticalLayout {

    private static final long serialVersionUID = 1L;

    private static final String MSG_KEY_CHECKBOX = "label.configuration.repository.multiassignments";
    private static final String MSG_KEY_NOTICE = "label.configuration.repository.multiassignments.notice";

    private final VerticalLayout container;
    private final VaadinMessageSource i18n;

    /**
     * Constructor.
     *
     * @param i18n
     *            VaadinMessageSource
     * @param binder
     *            System config window binder
     */
    public MultiAssignmentsConfigurationItem(final VaadinMessageSource i18n,
            final Binder<ProxySystemConfigRepository> binder) {
        this.i18n = i18n;
        this.setSpacing(false);
        this.setMargin(false);
        addComponent(SPUIComponentProvider.generateLabel(i18n, MSG_KEY_CHECKBOX));
        container = new VerticalLayout();
        container.setSpacing(false);
        container.setMargin(false);
        container.addComponent(newLabel(MSG_KEY_NOTICE));
        if (binder.getBean().isMultiAssignments()) {
            showSettings();
        }
    }

    /**
     * Show multi assignment settings
     */
    public void showSettings() {
        addComponent(container);
    }

    /**
     * Hide multi assignment settings
     */
    public void hideSettings() {
        removeComponent(container);
    }

    private Label newLabel(final String msgKey) {
        final Label label = SPUIComponentProvider.generateLabel(i18n, msgKey);
        label.setWidthUndefined();
        return label;
    }

}
