/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.detailslayout;

import org.eclipse.hawkbit.ui.common.grid.header.support.HeaderSupport;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleNoBorder;
import org.eclipse.hawkbit.ui.utils.UIMessageIdProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;

/**
 * Edit Detail header support
 */
public class EditDetailsHeaderSupport implements HeaderSupport {
    private final VaadinMessageSource i18n;

    private final String editIconId;
    private final Runnable editItemCallback;

    private final Button editIcon;

    /**Constructor for EditDetailsHeaderSupport
     *
     * @param i18n
     *          VaadinMessageSource
     * @param editIconId
     *          Id of edit icon
     * @param editItemCallback
 *              Runnable
     */
    public EditDetailsHeaderSupport(final VaadinMessageSource i18n, final String editIconId,
            final Runnable editItemCallback) {
        this.i18n = i18n;
        this.editIconId = editIconId;
        this.editItemCallback = editItemCallback;

        this.editIcon = createEditIcon();
    }

    private Button createEditIcon() {
        final Button editButton = SPUIComponentProvider.getButton("", "",
                i18n.getMessage(UIMessageIdProvider.TOOLTIP_UPDATE), null, false, VaadinIcons.PENCIL,
                SPUIButtonStyleNoBorder.class);

        editButton.setId(editIconId);
        editButton.addClickListener(event -> editItemCallback.run());
        editButton.setEnabled(false);

        return editButton;
    }

    @Override
    public Component getHeaderComponent() {
        return editIcon;
    }

    /**
     * Enable edit icon
     */
    public void enableEditIcon() {
        editIcon.setEnabled(true);
    }

    /**
     * Disable edit icon
     */
    public void disableEditIcon() {
        editIcon.setEnabled(false);
    }
}
