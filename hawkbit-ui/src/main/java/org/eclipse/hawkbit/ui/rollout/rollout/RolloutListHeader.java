/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.rollout.rollout;

import javax.annotation.PostConstruct;

import org.eclipse.hawkbit.repository.SpPermissionChecker;
import org.eclipse.hawkbit.ui.common.builder.LabelBuilder;
import org.eclipse.hawkbit.ui.common.grid.AbstractGridHeader;
import org.eclipse.hawkbit.ui.rollout.event.RolloutEvent;
import org.eclipse.hawkbit.ui.rollout.state.RolloutUIState;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.events.EventBus;

import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;

/**
 * 
 * Header layout of rollout list view.
 *
 */
@SpringComponent
@ViewScope
public class RolloutListHeader extends AbstractGridHeader {
    private static final long serialVersionUID = 2365400733081333174L;

    @Autowired
    private SpPermissionChecker permissionChecker;

    @Autowired
    private transient RolloutUIState rolloutUIState;

    @Autowired
    private transient EventBus.SessionEventBus eventBus;

    @Autowired
    private AddUpdateRolloutWindowLayout addUpdateRolloutWindow;

    @Override
    @PostConstruct
    protected void init() {
        super.init();
        addUpdateRolloutWindow.init();
    }

    @Override
    protected void resetSearchText() {
        rolloutUIState.setSearchText(null);
        eventBus.publish(this, RolloutEvent.FILTER_BY_TEXT);
    }

    protected String getHeaderCaption() {
        return SPUIDefinitions.ROLLOUT_LIST_HEADER_CAPTION;
    }

    @Override
    protected String getSearchBoxId() {
        return UIComponentIdProvider.ROLLOUT_LIST_SEARCH_BOX_ID;
    }

    @Override
    protected String getSearchRestIconId() {
        return UIComponentIdProvider.ROLLOUT_LIST_SEARCH_RESET_ICON_ID;
    }

    @Override
    protected void searchBy(final String newSearchText) {
        rolloutUIState.setSearchText(newSearchText);
        eventBus.publish(this, RolloutEvent.FILTER_BY_TEXT);
    }

    @Override
    protected String getAddIconId() {
        return UIComponentIdProvider.ROLLOUT_ADD_ICON_ID;
    }

    @Override
    protected void addNewItem(final ClickEvent event) {
        final Window addTargetWindow = addUpdateRolloutWindow.getWindow();
        UI.getCurrent().addWindow(addTargetWindow);
        addTargetWindow.setVisible(Boolean.TRUE);

    }

    @Override
    protected void onClose(final ClickEvent event) {
        // No implementation required.
    }

    @Override
    protected boolean hasCreatePermission() {
        return permissionChecker.hasRolloutCreatePermission();
    }

    @Override
    protected String getCloseButtonId() {
        return null;
    }

    @Override
    protected boolean showCloseButton() {
        return false;
    }

    @Override
    protected boolean isAllowSearch() {
        return true;
    }

    @Override
    protected String onLoadSearchBoxValue() {
        return rolloutUIState.getSearchText().isPresent() ? rolloutUIState.getSearchText().get() : null;
    }

    @Override
    protected boolean isRollout() {
        return true;
    }

    @Override
    protected HorizontalLayout getHeaderCaptionLayout() {
        final Label headerCaption = new LabelBuilder().name(getHeaderCaption()).buildCaptionLabel();
        final HorizontalLayout headerCaptionLayout = new HorizontalLayout();
        headerCaptionLayout.addComponent(headerCaption);

        return headerCaptionLayout;
    }

    @Override
    protected void restoreCaption() {
        // No implementation required.
    }

}
