/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.dstable;

import org.eclipse.hawkbit.repository.SpPermissionChecker;
import org.eclipse.hawkbit.ui.common.table.AbstractTableHeader;
import org.eclipse.hawkbit.ui.common.table.BaseEntityEventType;
import org.eclipse.hawkbit.ui.management.event.DistributionTableEvent;
import org.eclipse.hawkbit.ui.management.event.DistributionTableFilterEvent;
import org.eclipse.hawkbit.ui.management.event.ManagementUIEvent;
import org.eclipse.hawkbit.ui.management.state.ManagementUIState;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.vaadin.spring.events.EventBus.UIEventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.vaadin.event.dd.DropHandler;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;

/**
 * Distribution table header.
 *
 */
public class DistributionTableHeader extends AbstractTableHeader {
    private static final long serialVersionUID = 7597766804650170127L;

    private final DistributionAddUpdateWindowLayout distributionAddUpdateWindowLayout;

    DistributionTableHeader(final I18N i18n, final SpPermissionChecker permChecker, final UIEventBus eventbus,
            final ManagementUIState managementUIState,
            final DistributionAddUpdateWindowLayout distributionAddUpdateWindowLayout) {
        super(i18n, permChecker, eventbus, managementUIState, null, null);
        this.distributionAddUpdateWindowLayout = distributionAddUpdateWindowLayout;
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onEvent(final ManagementUIEvent event) {
        if (event == ManagementUIEvent.HIDE_DISTRIBUTION_TAG_LAYOUT) {
            setFilterButtonsIconVisible(true);
        } else if (event == ManagementUIEvent.SHOW_DISTRIBUTION_TAG_LAYOUT) {
            setFilterButtonsIconVisible(false);
        }
    }

    @Override
    protected String getHeaderCaption() {
        return i18n.get("header.dist.table");
    }

    @Override
    protected String getSearchBoxId() {
        return UIComponentIdProvider.DIST_SEARCH_TEXTFIELD;
    }

    @Override
    protected String getSearchRestIconId() {
        return UIComponentIdProvider.DIST_SEARCH_ICON;
    }

    @Override
    protected String getAddIconId() {
        return UIComponentIdProvider.DIST_ADD_ICON;
    }

    @Override
    protected String onLoadSearchBoxValue() {
        if (managementUIState.getDistributionTableFilters().getSearchText().isPresent()) {
            return managementUIState.getDistributionTableFilters().getSearchText().get();
        }
        return null;
    }

    @Override
    protected String getDropFilterId() {
        return null;
    }

    @Override
    protected boolean hasCreatePermission() {
        return permChecker.hasCreateDistributionPermission();
    }

    @Override
    protected boolean isDropHintRequired() {
        return true;
    }

    @Override
    protected boolean isDropFilterRequired() {
        return false;
    }

    @Override
    protected String getShowFilterButtonLayoutId() {
        return UIComponentIdProvider.SHOW_DIST_TAG_ICON;
    }

    @Override
    protected void showFilterButtonsLayout() {
        managementUIState.setDistTagFilterClosed(false);
        eventbus.publish(this, ManagementUIEvent.SHOW_DISTRIBUTION_TAG_LAYOUT);
    }

    @Override
    protected void resetSearchText() {
        if (managementUIState.getDistributionTableFilters().getSearchText().isPresent()) {
            managementUIState.getDistributionTableFilters().setSearchText(null);
            eventbus.publish(this, DistributionTableFilterEvent.REMOVE_FILTER_BY_TEXT);
        }
    }

    @Override
    protected String getMaxMinIconId() {
        return UIComponentIdProvider.DS_MAX_MIN_TABLE_ICON;
    }

    @Override
    public void maximizeTable() {
        managementUIState.setDsTableMaximized(Boolean.TRUE);
        eventbus.publish(this, new DistributionTableEvent(BaseEntityEventType.MAXIMIZED, null));
    }

    @Override
    public void minimizeTable() {
        managementUIState.setDsTableMaximized(Boolean.FALSE);
        eventbus.publish(this, new DistributionTableEvent(BaseEntityEventType.MINIMIZED, null));
    }

    @Override
    public Boolean onLoadIsTableMaximized() {
        return managementUIState.isDsTableMaximized();
    }

    @Override
    public Boolean onLoadIsShowFilterButtonDisplayed() {
        return managementUIState.isDistTagFilterClosed();
    }

    @Override
    protected void searchBy(final String newSearchText) {
        managementUIState.getDistributionTableFilters().setSearchText(newSearchText);
        eventbus.publish(this, DistributionTableFilterEvent.FILTER_BY_TEXT);
    }

    @Override
    protected void addNewItem(final ClickEvent event) {
        final Window newDistWindow = distributionAddUpdateWindowLayout.getWindow(null);
        newDistWindow.setCaption(i18n.get("caption.add.new.dist"));
        UI.getCurrent().addWindow(newDistWindow);
        newDistWindow.setVisible(Boolean.TRUE);
    }

    @Override
    protected Boolean isAddNewItemAllowed() {
        return Boolean.FALSE;
    }

    @Override
    protected String getFilterIconStyle() {
        return null;
    }

    @Override
    protected String getDropFilterWrapperId() {
        return null;
    }

    @Override
    protected DropHandler getDropFilterHandler() {
        return null;
    }

    @Override
    protected String getBulkUploadIconId() {
        return null;
    }

    @Override
    protected void bulkUpload(final ClickEvent event) {
        // No implementation as no bulk upload is supported.
    }

    @Override
    protected Boolean isBulkUploadAllowed() {
        return Boolean.FALSE;
    }

    @Override
    protected boolean isBulkUploadInProgress() {
        return false;
    }
}
