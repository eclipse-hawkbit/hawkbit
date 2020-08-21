/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.tenantconfiguration.authentication;

import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;

import com.vaadin.ui.HorizontalLayout;

/**
 * This class represents the UI item for the anonymous download by in the
 * authentication configuration view.
 */
public class AnonymousDownloadAuthenticationConfigurationItem extends HorizontalLayout {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor for AnonymousDownloadAuthenticationConfigurationItem
     *
     * @param i18n
     *            VaadinMessageSource
     */
    public AnonymousDownloadAuthenticationConfigurationItem(final VaadinMessageSource i18n) {
        setSpacing(false);
        setMargin(false);
        addComponent(SPUIComponentProvider.generateLabel(i18n, "label.configuration.anonymous.download"));
    }
}
