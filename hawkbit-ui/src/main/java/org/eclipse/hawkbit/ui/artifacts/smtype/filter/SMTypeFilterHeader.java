/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts.smtype.filter;

import org.eclipse.hawkbit.ui.artifacts.smtype.SmTypeWindowBuilder;
import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.event.EventLayout;
import org.eclipse.hawkbit.ui.common.event.EventView;
import org.eclipse.hawkbit.ui.common.grid.header.AbstractFilterHeader;
import org.eclipse.hawkbit.ui.common.state.TypeFilterLayoutUiState;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UIMessageIdProvider;

import com.vaadin.ui.Window;

/**
 * Software module type filter buttons header.
 */
public class SMTypeFilterHeader extends AbstractFilterHeader {
    private static final long serialVersionUID = 1L;

    private final TypeFilterLayoutUiState smTypeFilterLayoutUiState;

    private final transient SmTypeWindowBuilder smTypeWindowBuilder;

    private final EventView view;

    /**
     * Constructor for SMTypeFilterHeader
     *
     * @param uiDependencies
     *            {@link CommonUiDependencies}
     * @param smTypeWindowBuilder
     *            SmTypeWindowBuilder
     * @param smTypeFilterLayoutUiState
     *            TypeFilterLayoutUiState
     * @param view
     *            EventView
     */
    public SMTypeFilterHeader(final CommonUiDependencies uiDependencies, final SmTypeWindowBuilder smTypeWindowBuilder,
            final TypeFilterLayoutUiState smTypeFilterLayoutUiState, final EventView view) {
        super(uiDependencies.getI18n(), uiDependencies.getPermChecker(), uiDependencies.getEventBus());

        this.smTypeFilterLayoutUiState = smTypeFilterLayoutUiState;
        this.smTypeWindowBuilder = smTypeWindowBuilder;
        this.view = view;

        buildHeader();
    }

    @Override
    protected String getHeaderCaptionMsgKey() {
        return UIMessageIdProvider.CAPTION_FILTER_BY_TYPE;
    }

    @Override
    protected String getCrudMenuBarId() {
        return UIComponentIdProvider.SOFT_MODULE_TYPE_MENU_BAR_ID;
    }

    @Override
    protected Window getWindowForAdd() {
        return smTypeWindowBuilder.getWindowForAdd();
    }

    @Override
    protected String getAddEntityWindowCaptionMsgKey() {
        return "caption.type";
    }

    @Override
    protected String getCloseIconId() {
        return UIComponentIdProvider.HIDE_SM_TYPES;
    }

    @Override
    protected void updateHiddenUiState() {
        smTypeFilterLayoutUiState.setHidden(true);
    }

    @Override
    protected EventLayout getLayout() {
        return EventLayout.SM_TYPE_FILTER;
    }

    @Override
    protected EventView getView() {
        return view;
    }
}
