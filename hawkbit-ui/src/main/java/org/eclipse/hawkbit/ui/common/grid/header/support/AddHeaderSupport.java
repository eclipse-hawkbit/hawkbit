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
 * Add header support for Grid component
 */
public class AddHeaderSupport implements HeaderSupport {
    private final VaadinMessageSource i18n;

    private final String addIconId;
    private final Runnable addItemCallback;
    private final BooleanSupplier maximizedStateSupplier;

    private final Button addIcon;

    /**
     * Constructor for AddHeaderSupport
     *
     * @param i18n
     *            VaadinMessageSource
     * @param addIconId
     *            Add icon id
     * @param addItemCallback
     *            callback method to add new entities
     * @param maximizedStateSupplier
     *            provides the max/min state of the grid
     */
    public AddHeaderSupport(final VaadinMessageSource i18n, final String addIconId, final Runnable addItemCallback,
            final BooleanSupplier maximizedStateSupplier) {
        this.i18n = i18n;

        this.addIconId = addIconId;
        this.addItemCallback = addItemCallback;
        this.maximizedStateSupplier = maximizedStateSupplier;

        this.addIcon = createAddButton();
    }

    private Button createAddButton() {
        final Button addButton = SPUIComponentProvider.getButton(addIconId, "",
                i18n.getMessage(UIMessageIdProvider.TOOLTIP_ADD), null, false, VaadinIcons.PLUS,
                SPUIButtonStyleNoBorder.class);

        addButton.addClickListener(event -> addItemCallback.run());

        return addButton;
    }

    /**
     * Hide add icon
     */
    public void hideAddIcon() {
        addIcon.setVisible(false);
    }

    /**
     * Show add icon
     */
    public void showAddIcon() {
        addIcon.setVisible(true);
    }

    @Override
    public Component getHeaderComponent() {
        return addIcon;
    }

    @Override
    public void restoreState() {
        if (maximizedStateSupplier.getAsBoolean()) {
            hideAddIcon();
        }
    }
}
