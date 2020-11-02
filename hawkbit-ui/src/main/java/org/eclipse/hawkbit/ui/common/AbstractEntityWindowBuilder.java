/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common;

import org.eclipse.hawkbit.ui.common.CommonDialogWindow.SaveDialogCloseListener;
import org.eclipse.hawkbit.ui.common.builder.WindowBuilder;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;

import com.vaadin.ui.Component;
import com.vaadin.ui.Window;

/**
 * Builder for abstract entity window
 *
 * @param <T>
 *            Generic type entity
 */
public abstract class AbstractEntityWindowBuilder<T> {
    protected final CommonUiDependencies uiDependencies;

    protected AbstractEntityWindowBuilder(final CommonUiDependencies uiDependencies) {
        this.uiDependencies = uiDependencies;
    }

    protected CommonDialogWindow getWindowForNewEntity(final AbstractEntityWindowController<T, ?, ?> controller) {
        return getWindowForEntity(null, controller);
    }

    protected CommonDialogWindow getWindowForNewEntity(final AbstractEntityWindowController<T, ?, ?> controller,
            final Component windowContent) {
        return getWindowForEntity(null, controller, windowContent);
    }

    protected CommonDialogWindow getWindowForEntity(final T proxyEntity,
            final AbstractEntityWindowController<T, ?, ?> controller) {
        return getWindowForEntity(proxyEntity, controller, controller.getLayout().getRootComponent());
    }

    protected CommonDialogWindow getWindowForEntity(final T proxyEntity,
            final AbstractEntityWindowController<T, ?, ?> controller, final Component windowContent) {
        controller.populateWithData(proxyEntity);

        final CommonDialogWindow window = createWindow(windowContent, controller.getSaveDialogCloseListener());

        controller.getLayout().addValidationListener(window::setSaveButtonEnabled);

        return window;
    }

    protected CommonDialogWindow createWindow(final Component content,
            final SaveDialogCloseListener saveDialogCloseListener) {
        return new WindowBuilder(SPUIDefinitions.CREATE_UPDATE_WINDOW).id(getWindowId()).content(content)
                .i18n(uiDependencies.getI18n()).helpLink(getHelpLink()).saveDialogCloseListener(saveDialogCloseListener)
                .buildCommonDialogWindow();
    }

    protected abstract String getWindowId();

    /**
     * Gets the add window
     *
     * @return window
     */
    public abstract Window getWindowForAdd();

    /**
     * Gets the update window
     *
     * @param entity
     *            Generic type entity
     *
     * @return window
     */
    public abstract Window getWindowForUpdate(final T entity);

    protected String getHelpLink() {
        // can be overriden to provide help link to documentation
        return null;
    }

    protected VaadinMessageSource getI18n() {
        return uiDependencies.getI18n();
    }
}
