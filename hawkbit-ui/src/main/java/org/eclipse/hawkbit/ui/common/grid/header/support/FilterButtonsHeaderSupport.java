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
 * Header support for filter button
 */
public class FilterButtonsHeaderSupport implements HeaderSupport {
    private final VaadinMessageSource i18n;

    private final String filterButtonsIconId;
    private final Runnable showFilterButtonsLayoutCallback;
    private final BooleanSupplier filterButtonsStateSupplier;

    private final Button filterButtonsIcon;

    /**
     * Constructor for FilterButtonsHeaderSupport
     *
     * @param i18n
     *          FilterButtonsHeaderSupport
     * @param filterButtonsIconId
     *          Filter button icon id
     * @param showFilterButtonsLayoutCallback
     *          Runnable
     * @param filterButtonsStateSupplier
     *          BooleanSupplier
     */
    public FilterButtonsHeaderSupport(final VaadinMessageSource i18n, final String filterButtonsIconId,
            final Runnable showFilterButtonsLayoutCallback, final BooleanSupplier filterButtonsStateSupplier) {
        this.i18n = i18n;

        this.filterButtonsIconId = filterButtonsIconId;
        this.showFilterButtonsLayoutCallback = showFilterButtonsLayoutCallback;
        this.filterButtonsStateSupplier = filterButtonsStateSupplier;

        this.filterButtonsIcon = createfilterButtonsIcon();
    }

    private Button createfilterButtonsIcon() {
        final Button filterButtonsButton = SPUIComponentProvider.getButton(filterButtonsIconId, null,
                i18n.getMessage(UIMessageIdProvider.TOOLTIP_SHOW_TAGS), null, false, VaadinIcons.TAGS,
                SPUIButtonStyleNoBorder.class);

        // Hidden by default.
        filterButtonsButton.setVisible(false);
        filterButtonsButton.addClickListener(event -> filterButtonsIconClicked());

        return filterButtonsButton;
    }

    private void filterButtonsIconClicked() {
        hideFilterButtonsIcon();
        showFilterButtonsLayoutCallback.run();
    }

    /**
     * Hide filter button icon
     */
    public void hideFilterButtonsIcon() {
        filterButtonsIcon.setVisible(false);
    }

    /**
     * Show filter button icon
     */
    public void showFilterButtonsIcon() {
        filterButtonsIcon.setVisible(true);
    }

    @Override
    public Component getHeaderComponent() {
        return filterButtonsIcon;
    }

    @Override
    public void restoreState() {
        if (filterButtonsStateSupplier.getAsBoolean()) {
            filterButtonsIcon.setVisible(true);
        }
    }
}
