/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.actionhistory;

import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTarget;
import org.eclipse.hawkbit.ui.common.event.CommandTopics;
import org.eclipse.hawkbit.ui.common.event.EventLayout;
import org.eclipse.hawkbit.ui.common.event.EventView;
import org.eclipse.hawkbit.ui.common.event.LayoutResizeEventPayload;
import org.eclipse.hawkbit.ui.common.event.LayoutResizeEventPayload.ResizeType;
import org.eclipse.hawkbit.ui.common.grid.header.AbstractMasterAwareGridHeader;
import org.eclipse.hawkbit.ui.common.grid.header.support.ResizeHeaderSupport;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UIMessageIdProvider;

/**
 * Header for ActionHistory with maximize-support.
 */
public class ActionHistoryGridHeader extends AbstractMasterAwareGridHeader<ProxyTarget> {
    private static final long serialVersionUID = 1L;

    private final ActionHistoryGridLayoutUiState actionHistoryGridLayoutUiState;

    private final transient ResizeHeaderSupport resizeHeaderSupport;

    /**
     * Constructor for ActionHistoryGridHeader
     *
     * @param uiDependencies
     *            {@link CommonUiDependencies}
     * @param actionHistoryGridLayoutUiState
     *            ActionHistoryGridLayoutUiState
     */
    public ActionHistoryGridHeader(final CommonUiDependencies uiDependencies,
            final ActionHistoryGridLayoutUiState actionHistoryGridLayoutUiState) {
        super(uiDependencies.getI18n(), uiDependencies.getPermChecker(), uiDependencies.getEventBus());

        this.actionHistoryGridLayoutUiState = actionHistoryGridLayoutUiState;

        this.resizeHeaderSupport = new ResizeHeaderSupport(i18n, SPUIDefinitions.EXPAND_ACTION_HISTORY,
                this::maximizeTable, this::minimizeTable, this::onLoadIsTableMaximized);
        addHeaderSupport(resizeHeaderSupport);

        buildHeader();
    }

    @Override
    protected String getEntityDetailsCaptionMsgKey() {
        return UIMessageIdProvider.CAPTION_ACTION_HISTORY;
    }

    @Override
    protected String getMasterEntityDetailsCaptionId() {
        return UIComponentIdProvider.ACTION_HISTORY_DETAILS_HEADER_LABEL_ID;
    }

    @Override
    protected String getMasterEntityName(final ProxyTarget masterEntity) {
        return masterEntity.getName();
    }

    @Override
    protected String getEntityDetailsCaptionOfMsgKey() {
        return UIMessageIdProvider.CAPTION_ACTION_HISTORY_FOR;
    }

    private void maximizeTable() {
        eventBus.publish(CommandTopics.RESIZE_LAYOUT, this, new LayoutResizeEventPayload(ResizeType.MAXIMIZE,
                EventLayout.ACTION_HISTORY_LIST, EventView.DEPLOYMENT));

        actionHistoryGridLayoutUiState.setMaximized(true);
    }

    private void minimizeTable() {
        eventBus.publish(CommandTopics.RESIZE_LAYOUT, this, new LayoutResizeEventPayload(ResizeType.MINIMIZE,
                EventLayout.ACTION_HISTORY_LIST, EventView.DEPLOYMENT));

        actionHistoryGridLayoutUiState.setMaximized(false);
    }

    private Boolean onLoadIsTableMaximized() {
        return actionHistoryGridLayoutUiState.isMaximized();
    }
}
