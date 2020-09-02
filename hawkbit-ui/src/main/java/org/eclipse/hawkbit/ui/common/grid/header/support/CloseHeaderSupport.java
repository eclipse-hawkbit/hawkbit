/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.grid.header.support;

import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleNoBorder;
import org.eclipse.hawkbit.ui.utils.UIMessageIdProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;

/**
 * Close header support
 */
public class CloseHeaderSupport implements HeaderSupport {
    private final VaadinMessageSource i18n;

    private final String closeIconId;
    private final Runnable closeCallback;

    private final Button closeIcon;

    /**
     * Constructor for CloseHeaderSupport
     *
     * @param i18n
     *          VaadinMessageSource
     * @param closeIconId
     *          Close icon id
     * @param closeCallback
     *          Runnable
     */
    public CloseHeaderSupport(final VaadinMessageSource i18n, final String closeIconId, final Runnable closeCallback) {
        this.i18n = i18n;

        this.closeIconId = closeIconId;
        this.closeCallback = closeCallback;

        this.closeIcon = createCloseButton();
    }

    private Button createCloseButton() {
        final Button closeButton = SPUIComponentProvider.getButton(closeIconId, "",
                i18n.getMessage(UIMessageIdProvider.TOOLTIP_CLOSE), null, false, VaadinIcons.CLOSE,
                SPUIButtonStyleNoBorder.class);

        closeButton.addClickListener(event -> closeCallback.run());

        return closeButton;
    }

    @Override
    public Component getHeaderComponent() {
        return closeIcon;
    }
}
