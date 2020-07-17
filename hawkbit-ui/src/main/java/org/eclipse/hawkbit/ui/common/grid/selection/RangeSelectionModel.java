/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.grid.selection;

import java.util.Collections;
import java.util.LinkedHashSet;

import org.eclipse.hawkbit.ui.common.grid.selection.client.RangeSelectionServerRpc;

import com.vaadin.event.ShortcutAction;
import com.vaadin.event.ShortcutListener;
import com.vaadin.server.Page;
import com.vaadin.server.WebBrowser;
import com.vaadin.shared.communication.ServerRpc;
import com.vaadin.ui.components.grid.MultiSelectionModelImpl;

/**
 * 
 * Extends {@link MultiSelectionModelImpl} to allow selecting items by clicking
 * instead of using the checkboxes. Also allows using CTRL and SHIFT to select
 * multiple items or a range of items.
 *
 * @param <T>
 *            Item type
 */
public class RangeSelectionModel<T> extends MultiSelectionModelImpl<T> {
    private static final long serialVersionUID = 1L;

    /**
     * {@link ServerRpc} implementation to enable client side code to execute a
     * range selection
     */
    public class RangeSelectionServerRpcImp implements RangeSelectionServerRpc {
        private static final long serialVersionUID = 1L;

        @Override
        public void selectRange(final int startIndex, final int endIndex, final boolean overwrite) {
            if (overwrite) {
                onDeselectAll(true);
            }
            onSelectRange(startIndex, endIndex);
        }

        @Override
        public void selectAll() {
            onSelectAll(true);
        }
    }

    /**
     * Consumes CTRL+A presses of the user. This prevents the selection of
     * everything shown in the browser. Since the listening does not seem to be
     * bound to the grid, the selection logic is moved to the client side.
     *
     */
    private static class SelectAllListener extends ShortcutListener {
        private static final long serialVersionUID = 1L;

        private static final int[] CTRL_MODIFIER_KEYS = { getCtrlOrMetaModifier() };

        /**
         * Constructor
         */
        public SelectAllListener() {
            super("Select all", ShortcutAction.KeyCode.A, CTRL_MODIFIER_KEYS);
        }

        @Override
        public void handleAction(final Object sender, final Object target) {
            // Do nothing
        }

        /**
         * Returns the ctrl or meta modifier depending on the platform.
         * 
         * @return on mac return
         *         {@link com.vaadin.event.ShortcutAction.ModifierKey#META}
         *         other platform return
         *         {@link com.vaadin.event.ShortcutAction.ModifierKey#CTRL}
         */
        public static int getCtrlOrMetaModifier() {
            final WebBrowser webBrowser = Page.getCurrent().getWebBrowser();
            if (webBrowser.isMacOSX()) {
                return ShortcutAction.ModifierKey.META;
            }

            return ShortcutAction.ModifierKey.CTRL;
        }
    }

    @Override
    protected void init() {
        super.init();
        registerRpc(new RangeSelectionServerRpcImp());
        getGrid().addShortcutListener(new SelectAllListener());
    }

    protected void onSelectRange(final int startIndex, final int endIndex) {
        final int offset = Math.min(startIndex, endIndex);
        final int limit = Math.abs(startIndex - endIndex) + 1;

        final LinkedHashSet<T> addedItems = new LinkedHashSet<>(
                getGrid().getDataCommunicator().fetchItemsWithRange(offset, limit));
        updateSelection(addedItems, Collections.emptySet(), true);
    }
}
