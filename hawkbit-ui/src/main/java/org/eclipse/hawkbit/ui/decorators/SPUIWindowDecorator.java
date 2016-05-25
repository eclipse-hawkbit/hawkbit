/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.decorators;

import org.eclipse.hawkbit.ui.common.CommonDialogWindow;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;

import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.Window;

/**
 * Decorator for Window.
 * 
 *
 *
 */
public final class SPUIWindowDecorator {

    /**
     * Private Constructor.
     */
    private SPUIWindowDecorator() {

    }

    /**
     * Decorates window based on type.
     * 
     * @param caption
     *            window caption
     * @param id
     *            window id
     * @param type
     *            window type
     * @return Window
     */
    public static CommonDialogWindow getDeocratedWindow(final String caption, final String id, final String type,
            final Component content, final ClickListener saveButtonClickListener,
            final ClickListener cancelButtonClickListener, final String helpLink) {

        final CommonDialogWindow window = new CommonDialogWindow(caption, content, helpLink, saveButtonClickListener,
                cancelButtonClickListener);
        if (null != id) {
            window.setId(id);
        }
        if (SPUIDefinitions.CONFIRMATION_WINDOW.equals(type)) {
            window.setDraggable(false);
            window.setClosable(true);
            window.addStyleName(SPUIStyleDefinitions.CONFIRMATION_WINDOW_CAPTION);

        } else if (SPUIDefinitions.CREATE_UPDATE_WINDOW.equals(type)) {
            window.setDraggable(true);
            window.setClosable(true);
        }
        return window;
    }

    /**
     * Decorates window based on type.
     * 
     * @param caption
     *            window caption
     * @param id
     *            window id
     * @param type
     *            window type
     * @return Window
     */
    public static Window getDeocratedWindow(final String caption, final String id, final String type) {
        final Window window = new Window(caption);
        window.setSizeUndefined();
        window.setModal(true);
        window.setResizable(false);
        if (null != id) {
            window.setId(id);
        }
        if (SPUIDefinitions.CONFIRMATION_WINDOW.equals(type)) {
            window.setDraggable(false);
            window.setClosable(true);
            window.addStyleName(SPUIStyleDefinitions.CONFIRMATION_WINDOW_CAPTION);

        } else if (SPUIDefinitions.CREATE_UPDATE_WINDOW.equals(type)) {
            window.setDraggable(true);
            window.setClosable(false);
        }
        return window;
    }
}
