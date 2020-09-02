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
import java.util.Set;

import org.eclipse.hawkbit.ui.common.grid.selection.client.RangeSelectionServerRpc;
import org.eclipse.hawkbit.ui.common.grid.selection.client.RangeSelectionState;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;

import com.vaadin.event.ShortcutAction;
import com.vaadin.event.ShortcutListener;
import com.vaadin.icons.VaadinIcons;
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

    private final VaadinMessageSource i18n;

    public RangeSelectionModel(final VaadinMessageSource i18n) {
        this.i18n = i18n;
    }

    @Override
    protected void init() {
        super.init();
        registerRpc(new RangeSelectionServerRpcImp());
        getGrid().addShortcutListener(new SelectAllListener());
    }

    @Override
    protected RangeSelectionState getState() {
        return (RangeSelectionState) super.getState();
    }

    @Override
    protected RangeSelectionState getState(final boolean markAsDirty) {
        return (RangeSelectionState) super.getState(markAsDirty);
    }

    @Override
    protected void updateSelection(final Set<T> addedItems, final Set<T> removedItems, final boolean userOriginated) {
        super.updateSelection(addedItems, removedItems, userOriginated);

        getState().setSelectionCount(getSelectedItems().size());
    }

    /**
     * {@link ServerRpc} implementation to enable client side code to execute a
     * range selection
     */
    public class RangeSelectionServerRpcImp implements RangeSelectionServerRpc {
        private static final long serialVersionUID = 1L;

        @Override
        public void selectRange(final int startIndex, final int endIndex) {
            onDeselectAll(true);
            onSelectRange(startIndex, endIndex);
        }

        @Override
        public void selectAll() {
            onDeselectAll(true);
            onSelectRange(0, getGrid().getDataCommunicator().getDataProviderSize());
        }

        private void onSelectRange(final int startIndex, final int endIndex) {
            int offset = Math.min(startIndex, endIndex);
            int limit = Math.abs(startIndex - endIndex) + 1;

            if (limit > RangeSelectionState.MAX_SELECTION_LIMIT) {
                showMaxSelectionLimitWarning();

                limit = RangeSelectionState.MAX_SELECTION_LIMIT;

                if (startIndex > endIndex) {
                    offset = startIndex - RangeSelectionState.MAX_SELECTION_LIMIT + 1;
                }
            }

            final LinkedHashSet<T> addedItems = new LinkedHashSet<>(
                    getGrid().getDataCommunicator().fetchItemsWithRange(offset, limit));
            updateSelection(addedItems, Collections.emptySet(), true);
        }

        @Override
        public void showMaxSelectionLimitWarning() {
            UINotification.showNotification(SPUIStyleDefinitions.SP_NOTIFICATION_WARNING_MESSAGE_STYLE,
                    i18n.getMessage("action.grid.selection.limit", RangeSelectionState.MAX_SELECTION_LIMIT),
                    VaadinIcons.WARNING);
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
}
