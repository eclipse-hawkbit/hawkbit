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
import org.eclipse.hawkbit.ui.common.CommonDialogWindow.SaveDialogCloseListener;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;

import com.vaadin.ui.AbstractLayout;
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
     * @param content
     *            content in the window
     * @param cancelButtonClickListener
     *            cancel button
     * @param helpLink
     *            help link
     * @param layout
     *            layout in the window
     * @param i18n
     *            i18n for internationalization
     * @return Window
     * 
     */

    public static CommonDialogWindow getWindow(final String caption, final String id, final String type,
            final Component content, final ClickListener cancelButtonClickListener, final String helpLink,
            final AbstractLayout layout, final I18N i18n) {
        return getWindow(caption, id, type, content, null, cancelButtonClickListener, helpLink, layout, i18n);
    }

    /**
     * @param caption
     *            window caption
     * @param id
     *            window id
     * @param type
     *            window type
     * @param content
     *            content in the window
     * @param saveButtonClickListener
     *            save button
     * @param cancelButtonClickListener
     *            cancel button
     * @param helpLink
     *            help link
     * @param layout
     *            layout in the window
     * @param i18n
     *            i18n for internationalization
     * @return window
     */
    public static CommonDialogWindow getWindow(final String caption, final String id, final String type,
            final Component content, final ClickListener saveButtonClickListener,
            final ClickListener cancelButtonClickListener, final String helpLink, final AbstractLayout layout,
            final I18N i18n) {
        final CommonDialogWindow window = new CommonDialogWindow(caption, content, helpLink, cancelButtonClickListener,
                layout, i18n);

        if (SPUIDefinitions.CUSTOM_METADATA_WINDOW.equals(type)) {
            window.setSaveDialogCloseListener(new SaveDialogCloseListener() {

                @Override
                public void saveOrUpdate() {
                    saveButtonClickListener.buttonClick(null);

                }

                @Override
                public boolean canWindowClose() {
                    return false;
                }

            });
            window.setDraggable(true);
            window.setClosable(true);
        } else {
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
