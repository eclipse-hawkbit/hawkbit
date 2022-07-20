/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.targettag.filter;

import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.event.CommandTopics;
import org.eclipse.hawkbit.ui.common.event.EventLayout;
import org.eclipse.hawkbit.ui.common.event.EventView;
import org.eclipse.hawkbit.ui.common.event.LayoutResizeEventPayload;
import org.eclipse.hawkbit.ui.common.event.LayoutResizeEventPayload.ResizeType;
import org.eclipse.hawkbit.ui.common.grid.header.AbstractFilterHeader;
import org.eclipse.hawkbit.ui.common.grid.header.support.ResizeHeaderSupport;
import org.eclipse.hawkbit.ui.management.targettag.TargetTagWindowBuilder;
import org.eclipse.hawkbit.ui.management.targettag.targettype.TargetTypeWindowBuilder;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UIMessageIdProvider;
import org.vaadin.spring.events.EventBus.UIEventBus;

import com.vaadin.ui.Window;

/**
 * Target Tag filter by Tag Header.
 */
public class TargetTagFilterHeader extends AbstractFilterHeader {
    private static final long serialVersionUID = 1L;

    private final TargetTagFilterLayoutUiState targetTagFilterLayoutUiState;

    private final transient TargetTagWindowBuilder targetTagWindowBuilder;

    private final transient TargetTypeWindowBuilder targetTypeWindowBuilder;

    private final transient ResizeHeaderSupport resizeHeaderSupport;

    /**
     * Constructor for TargetTagFilterHeader
     * 
     * @param uiDependencies
     *            {@link CommonUiDependencies}
     * @param targetTagFilterLayoutUiState
     *            {@link TargetTagFilterLayoutUiState}
     * @param targetTagWindowBuilder
     *            {@link TargetTagWindowBuilder}
     * @param targetTypeWindowBuilder
     *            {@link TargetTypeWindowBuilder}
     */
    public TargetTagFilterHeader(
            final CommonUiDependencies uiDependencies,
            final TargetTagFilterLayoutUiState targetTagFilterLayoutUiState,
            final TargetTagWindowBuilder targetTagWindowBuilder,
            final TargetTypeWindowBuilder targetTypeWindowBuilder) {
        super(uiDependencies.getI18n(), uiDependencies.getPermChecker(), uiDependencies.getEventBus());

        this.targetTagFilterLayoutUiState = targetTagFilterLayoutUiState;
        this.targetTagWindowBuilder = targetTagWindowBuilder;
        this.targetTypeWindowBuilder = targetTypeWindowBuilder;

        this.resizeHeaderSupport = new ResizeHeaderSupport(i18n, UIComponentIdProvider.CUSTOM_FILTER_MAX_MIN_TABLE_ICON,
                this::maximizeTable, this::minimizeTable, this::onLoadIsTableMaximized);
        addHeaderSupport(resizeHeaderSupport);

        buildHeader();
    }

    @Override
    public void restoreState() {
        super.restoreState();

        if (targetTagFilterLayoutUiState.isCustomFilterTabSelected()) {
            disableCrudMenu();
        }
    }

    @Override
    protected String getHeaderCaptionMsgKey() {
        return UIMessageIdProvider.HEADER_TARGET_TAG;
    }

    @Override
    protected String getCrudMenuBarId() {
        return UIComponentIdProvider.TARGET_MENU_BAR_ID;
    }

    @Override
    protected Window getWindowForAdd() {
        if (targetTagFilterLayoutUiState.isTargetTypeFilterTabSelected()) {
            return targetTypeWindowBuilder.getWindowForAdd();
        }
        return targetTagWindowBuilder.getWindowForAdd();
    }

    @Override
    protected String getAddEntityWindowCaptionMsgKey() {
        if (targetTagFilterLayoutUiState.isTargetTypeFilterTabSelected()) {
            return UIMessageIdProvider.CAPTION_TYPE;
        }
        return UIMessageIdProvider.CAPTION_TAG;
    }

    @Override
    protected String getCloseIconId() {
        return UIComponentIdProvider.HIDE_TARGET_TAGS;
    }

    @Override
    protected void updateHiddenUiState() {
        targetTagFilterLayoutUiState.setHidden(true);
    }

    protected void maximizeTable() {
        eventBus.publish(CommandTopics.RESIZE_LAYOUT, this,
                new LayoutResizeEventPayload(ResizeType.MAXIMIZE, getLayout(), EventView.DEPLOYMENT));

        targetTagFilterLayoutUiState.setMaximized(true);
    }

    protected void minimizeTable() {
        eventBus.publish(CommandTopics.RESIZE_LAYOUT, this,
                new LayoutResizeEventPayload(ResizeType.MINIMIZE, getLayout(), EventView.DEPLOYMENT));

        targetTagFilterLayoutUiState.setMaximized(false);
    }

    protected Boolean onLoadIsTableMaximized() {
        return targetTagFilterLayoutUiState.isMaximized();
    }

    @Override
    protected EventLayout getLayout() {
        return EventLayout.TARGET_TAG_FILTER;
    }

    @Override
    protected EventView getView() {
        return EventView.DEPLOYMENT;
    }
}
