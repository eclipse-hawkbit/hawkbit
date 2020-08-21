/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.grid.header.support;

import java.util.function.BooleanSupplier;

import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleNoBorder;
import org.eclipse.hawkbit.ui.utils.UIMessageIdProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;

/**
 * Header support for minimize and maximize the layout
 */
public class ResizeHeaderSupport implements HeaderSupport {
    private static final String MODE_MAXIMIZED = "mode-maximized";

    private final VaadinMessageSource i18n;

    private final String maxMinIconId;
    private final Runnable maximizeCallback;
    private final Runnable minimizeCallback;
    private final BooleanSupplier isMaximizedStateSupplier;

    private final Button maxMinIcon;

    private boolean isMaximized;

    /**
     * Constructor for ResizeHeaderSupport
     *
     * @param i18n
     *          VaadinMessageSource
     * @param maxMinIconId
     *          Max-min icon id
     * @param maximizeCallback
     *          Runnable
     * @param minimizeCallback
     *          Runnable
     * @param isMaximizedStateSupplier
     *          BooleanSupplier
     */
    public ResizeHeaderSupport(final VaadinMessageSource i18n, final String maxMinIconId,
            final Runnable maximizeCallback, final Runnable minimizeCallback,
            final BooleanSupplier isMaximizedStateSupplier) {
        this.i18n = i18n;

        this.maxMinIconId = maxMinIconId;
        this.maximizeCallback = maximizeCallback;
        this.minimizeCallback = minimizeCallback;
        this.isMaximizedStateSupplier = isMaximizedStateSupplier;

        this.maxMinIcon = createMaxMinIcon();

        this.isMaximized = false;
    }

    private Button createMaxMinIcon() {
        final Button maxMinbutton = SPUIComponentProvider.getButton(maxMinIconId, "",
                i18n.getMessage(UIMessageIdProvider.TOOLTIP_MAXIMIZE), null, false, VaadinIcons.EXPAND,
                SPUIButtonStyleNoBorder.class);

        maxMinbutton.addClickListener(event -> maxMinButtonClicked());

        return maxMinbutton;
    }

    private void maxMinButtonClicked() {
        if (isMaximized) {
            // Clicked on min icon
            showMaxIcon();
            minimizeCallback.run();
        } else {
            // Clicked on max Icon
            showMinIcon();
            maximizeCallback.run();
        }
        isMaximized = !isMaximized;
    }

    /**
     * Styles min-max-button icon with minimize decoration
     */
    private void showMinIcon() {
        maxMinIcon.setIcon(VaadinIcons.COMPRESS);
        maxMinIcon.setDescription(i18n.getMessage(UIMessageIdProvider.TOOLTIP_MINIMIZE));
        maxMinIcon.addStyleName(MODE_MAXIMIZED);
    }

    /**
     * Styles min-max-button icon with maximize decoration
     */
    private void showMaxIcon() {
        maxMinIcon.setIcon(VaadinIcons.EXPAND);
        maxMinIcon.setDescription(i18n.getMessage(UIMessageIdProvider.TOOLTIP_MAXIMIZE));
        maxMinIcon.removeStyleName(MODE_MAXIMIZED);
    }

    @Override
    public Component getHeaderComponent() {
        return maxMinIcon;
    }

    @Override
    public void restoreState() {
        if (isMaximizedStateSupplier.getAsBoolean()) {
            showMinIcon();
            isMaximized = true;
        }
    }
}
