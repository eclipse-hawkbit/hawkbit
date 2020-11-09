/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts.smtable;

import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyIdentifiableEntity;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxySoftwareModule;
import org.eclipse.hawkbit.ui.common.event.EventLayout;
import org.eclipse.hawkbit.ui.common.event.EventView;
import org.eclipse.hawkbit.ui.common.grid.header.AbstractEntityGridHeader;
import org.eclipse.hawkbit.ui.common.state.GridLayoutUiState;
import org.eclipse.hawkbit.ui.common.state.HidableLayoutUiState;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;

/**
 * Header of Software module table.
 */
public class SoftwareModuleGridHeader extends AbstractEntityGridHeader {
    private static final long serialVersionUID = 1L;

    private static final String SWM_TABLE_HEADER = "upload.swModuleTable.header";
    private static final String SWM_CAPTION = "caption.software.module";

    /**
     * Constructor for SoftwareModuleGridHeader
     *
     * @param uiDependencies
     *            {@link CommonUiDependencies}
     * @param smTypeFilterLayoutUiState
     *            HidableLayoutUiState
     * @param smGridLayoutUiState
     *            GridLayoutUiState
     * @param smWindowBuilder
     *            SmWindowBuilder
     * @param view
     *            EventView
     */
    public SoftwareModuleGridHeader(final CommonUiDependencies uiDependencies,
            final HidableLayoutUiState smTypeFilterLayoutUiState, final GridLayoutUiState smGridLayoutUiState,
            final SmWindowBuilder smWindowBuilder, final EventView view) {
        super(uiDependencies, smTypeFilterLayoutUiState, smGridLayoutUiState, EventLayout.SM_TYPE_FILTER, view);

        addAddHeaderSupport(smWindowBuilder);
    }

    @Override
    protected String getCaptionMsg() {
        return SWM_TABLE_HEADER;
    }

    @Override
    protected String getSearchFieldId() {
        return UIComponentIdProvider.SW_MODULE_SEARCH_TEXT_FIELD;
    }

    @Override
    protected String getSearchResetIconId() {
        return UIComponentIdProvider.SW_MODULE_SEARCH_RESET_ICON;
    }

    @Override
    protected Class<? extends ProxyIdentifiableEntity> getEntityType() {
        return ProxySoftwareModule.class;
    }

    @Override
    protected String getFilterButtonsIconId() {
        return UIComponentIdProvider.SHOW_SM_TYPE_ICON;
    }

    @Override
    protected String getMaxMinIconId() {
        return UIComponentIdProvider.SW_MAX_MIN_TABLE_ICON;
    }

    @Override
    protected EventLayout getLayout() {
        return EventLayout.SM_LIST;
    }

    @Override
    protected boolean hasCreatePermission() {
        return permChecker.hasCreateRepositoryPermission();
    }

    @Override
    protected String getAddIconId() {
        return UIComponentIdProvider.SW_MODULE_ADD_BUTTON;
    }

    @Override
    protected String getAddWindowCaptionMsg() {
        return SWM_CAPTION;
    }
}
