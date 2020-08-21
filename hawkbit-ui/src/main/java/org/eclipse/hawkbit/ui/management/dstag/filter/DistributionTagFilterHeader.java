/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.dstag.filter;

import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.common.event.EventLayout;
import org.eclipse.hawkbit.ui.common.event.EventView;
import org.eclipse.hawkbit.ui.common.grid.header.AbstractFilterHeader;
import org.eclipse.hawkbit.ui.common.state.TagFilterLayoutUiState;
import org.eclipse.hawkbit.ui.management.dstag.DsTagWindowBuilder;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UIMessageIdProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus.UIEventBus;

import com.vaadin.ui.Window;

/**
 * Distribution Set Tag filter buttons header.
 */
public class DistributionTagFilterHeader extends AbstractFilterHeader {
    private static final long serialVersionUID = 1L;

    private final TagFilterLayoutUiState distributionTagLayoutUiState;

    private final transient DsTagWindowBuilder dsTagWindowBuilder;

    /**
     * Constructor for UIEventBus
     *
     * @param i18n
     *          VaadinMessageSource
     * @param permChecker
     *          SpPermissionChecker
     * @param eventBus
     *         UIEventBus
     * @param dsTagWindowBuilder
     *          DsTagWindowBuilder
     * @param distributionTagLayoutUiState
     *          TagFilterLayoutUiState
     */
    public DistributionTagFilterHeader(final VaadinMessageSource i18n, final SpPermissionChecker permChecker,
            final UIEventBus eventBus, final DsTagWindowBuilder dsTagWindowBuilder,
            final TagFilterLayoutUiState distributionTagLayoutUiState) {
        super(i18n, permChecker, eventBus);

        this.distributionTagLayoutUiState = distributionTagLayoutUiState;
        this.dsTagWindowBuilder = dsTagWindowBuilder;

        buildHeader();
    }

    @Override
    protected String getHeaderCaptionMsgKey() {
        return UIMessageIdProvider.HEADER_TAG;
    }

    @Override
    protected String getCrudMenuBarId() {
        return UIComponentIdProvider.DIST_TAG_MENU_BAR_ID;
    }

    @Override
    protected Window getWindowForAdd() {
        return dsTagWindowBuilder.getWindowForAdd();
    }

    @Override
    protected String getAddEntityWindowCaptionMsgKey() {
        return UIMessageIdProvider.CAPTION_TAG;
    }

    @Override
    protected String getCloseIconId() {
        return UIComponentIdProvider.HIDE_DS_TAGS;
    }

    @Override
    protected void updateHiddenUiState() {
        distributionTagLayoutUiState.setHidden(true);
    }

    @Override
    protected EventLayout getLayout() {
        return EventLayout.DS_TAG_FILTER;
    }

    @Override
    protected EventView getView() {
        return EventView.DEPLOYMENT;
    }
}
