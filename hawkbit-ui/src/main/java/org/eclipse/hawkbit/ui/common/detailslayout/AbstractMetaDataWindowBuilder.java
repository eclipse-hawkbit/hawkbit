/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.detailslayout;

import org.eclipse.hawkbit.ui.common.AbstractEntityWindowBuilder;
import org.eclipse.hawkbit.ui.common.CommonDialogWindow;
import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyMetaData;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;

import com.vaadin.server.Sizeable.Unit;
import com.vaadin.ui.Window;

/**
 * Abstract builder for metadata window
 *
 * @param <F>
 *            Generic type
 */
public abstract class AbstractMetaDataWindowBuilder<F> extends AbstractEntityWindowBuilder<ProxyMetaData> {

    /**
     * Constructor for AbstractMetaDataWindowBuilder
     *
     * @param uiDependencies
     *            {@link CommonUiDependencies}
     */
    protected AbstractMetaDataWindowBuilder(final CommonUiDependencies uiDependencies) {
        super(uiDependencies);
    }

    @Override
    protected String getWindowId() {
        return UIComponentIdProvider.METADATA_POPUP_ID;
    }

    protected Window getWindowForShowMetaData(final AbstractMetaDataWindowLayout<F> metaDataWindowLayout,
            final F selectedEntityFilter, final String selectedEntityName, final ProxyMetaData proxyMetaData) {
        final CommonDialogWindow window = createWindow(metaDataWindowLayout, null);

        window.setAssistivePrefix(getI18n().getMessage("caption.metadata.popup") + " " + "<b>");
        window.setCaptionAsHtml(false);
        window.setCaption(selectedEntityName);
        window.setAssistivePostfix("</b>");

        metaDataWindowLayout.addValidationListener(window::setSaveButtonEnabled);
        metaDataWindowLayout.setSaveCallback(window::setCloseListener);
        metaDataWindowLayout.setMasterEntityFilter(selectedEntityFilter, proxyMetaData);

        window.setHeight(600, Unit.PIXELS);
        window.setWidth(800, Unit.PIXELS);

        return window;
    }

    @Override
    public Window getWindowForAdd() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Window getWindowForUpdate(final ProxyMetaData entity) {
        throw new UnsupportedOperationException();
    }
}
