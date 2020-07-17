/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.targettag.filter;

import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.common.event.EventLayout;
import org.eclipse.hawkbit.ui.common.event.EventView;
import org.eclipse.hawkbit.ui.common.grid.header.AbstractFilterHeader;
import org.eclipse.hawkbit.ui.management.targettag.TargetTagWindowBuilder;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UIMessageIdProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus.UIEventBus;

import com.vaadin.ui.Window;

/**
 * Target Tag filter by Tag Header.
 */
public class TargetTagFilterHeader extends AbstractFilterHeader {
    private static final long serialVersionUID = 1L;

    private final TargetTagFilterLayoutUiState targetTagFilterLayoutUiState;

    private final transient TargetTagWindowBuilder targetTagWindowBuilder;

    /**
     * Constructor for TargetTagFilterHeader
     *
     * @param i18n
     *          VaadinMessageSource
     * @param permChecker
     *          SpPermissionChecker
     * @param eventBus
     *          UIEventBus
     * @param targetTagFilterLayoutUiState
     *          TargetTagFilterLayoutUiState
     * @param targetTagWindowBuilder
     *          TargetTagWindowBuilder
     */
    public TargetTagFilterHeader(final VaadinMessageSource i18n, final SpPermissionChecker permChecker,
            final UIEventBus eventBus, final TargetTagFilterLayoutUiState targetTagFilterLayoutUiState,
            final TargetTagWindowBuilder targetTagWindowBuilder) {
        super(i18n, permChecker, eventBus);

        this.targetTagFilterLayoutUiState = targetTagFilterLayoutUiState;
        this.targetTagWindowBuilder = targetTagWindowBuilder;

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
        return targetTagWindowBuilder.getWindowForAdd();
    }

    @Override
    protected String getAddEntityWindowCaptionMsgKey() {
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

    @Override
    protected EventLayout getLayout() {
        return EventLayout.TARGET_TAG_FILTER;
    }

    @Override
    protected EventView getView() {
        return EventView.DEPLOYMENT;
    }
}
