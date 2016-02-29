/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.distributions.dstable;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.eclipse.hawkbit.repository.SpPermissionChecker;
import org.eclipse.hawkbit.ui.common.table.AbstractTableHeader;
import org.eclipse.hawkbit.ui.distributions.event.DistributionsUIEvent;
import org.eclipse.hawkbit.ui.distributions.event.DragEvent;
import org.eclipse.hawkbit.ui.distributions.state.ManageDistUIState;
import org.eclipse.hawkbit.ui.management.dstable.DistributionAddUpdateWindowLayout;
import org.eclipse.hawkbit.ui.management.event.DistributionTableEvent;
import org.eclipse.hawkbit.ui.management.event.DistributionTableEvent.DistributionComponentEvent;
import org.eclipse.hawkbit.ui.management.event.DistributionTableFilterEvent;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.SPUIComponetIdProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.vaadin.event.dd.DropHandler;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;

/**
 * Distribution table header.
 */
@SpringComponent
@ViewScope
public class DistributionSetTableHeader extends AbstractTableHeader {

    private static final long serialVersionUID = -3483238438474530748L;

    @Autowired
    private I18N i18n;

    @Autowired
    private SpPermissionChecker permChecker;

    @Autowired
    private transient EventBus.SessionEventBus eventbus;

    @Autowired
    private ManageDistUIState manageDistUIstate;

    @Autowired
    private DistributionAddUpdateWindowLayout addUpdateWindowLayout;

    @Override
    @PostConstruct
    protected void init() {
        super.init();
        eventbus.subscribe(this);
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final DistributionsUIEvent event) {
        if (event == DistributionsUIEvent.HIDE_DIST_FILTER_BY_TYPE) {
            setFilterButtonsIconVisible(true);
        }
    }

    @Override
    protected String getHeaderCaption() {
        return i18n.get("header.dist.table");
    }

    @Override
    protected String getSearchBoxId() {
        return SPUIComponetIdProvider.DIST_SEARCH_TEXTFIELD;
    }

    @Override
    protected String getSearchRestIconId() {
        return SPUIComponetIdProvider.DIST_SEARCH_ICON;
    }

    @Override
    protected String getAddIconId() {
        return SPUIComponetIdProvider.DIST_ADD_ICON;
    }

    @Override
    protected String onLoadSearchBoxValue() {
        if (manageDistUIstate.getManageDistFilters().getSearchText().isPresent()) {
            return manageDistUIstate.getManageDistFilters().getSearchText().get();
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
        return "show.dist.tags.icon";
    }

    @Override
    protected void showFilterButtonsLayout() {
        manageDistUIstate.setDistTypeFilterClosed(false);
        eventbus.publish(this, DistributionsUIEvent.SHOW_DIST_FILTER_BY_TYPE);
    }

    @Override
    protected void resetSearchText() {
        if (manageDistUIstate.getManageDistFilters().getSearchText().isPresent()) {
            manageDistUIstate.getManageDistFilters().setSearchText(null);
            eventbus.publish(this, DistributionTableFilterEvent.REMOVE_FILTER_BY_TEXT);
        }
    }

    @Override
    protected String getMaxMinIconId() {
        return SPUIComponetIdProvider.DS_MAX_MIN_TABLE_ICON;
    }

    @Override
    public void maximizeTable() {
        manageDistUIstate.setDsTableMaximized(Boolean.TRUE);
        eventbus.publish(this, new DistributionTableEvent(DistributionComponentEvent.MAXIMIZED, null));
    }

    @Override
    public void minimizeTable() {
        manageDistUIstate.setDsTableMaximized(Boolean.FALSE);
        eventbus.publish(this, new DistributionTableEvent(DistributionComponentEvent.MINIMIZED, null));
    }

    @Override
    public Boolean onLoadIsTableMaximized() {
        return manageDistUIstate.isDsTableMaximized();
    }

    @Override
    public Boolean onLoadIsShowFilterButtonDisplayed() {
        return manageDistUIstate.isDistTypeFilterClosed();
    }

    @Override
    protected void searchBy(final String newSearchText) {
        manageDistUIstate.getManageDistFilters().setSearchText(newSearchText);
        eventbus.publish(this, DistributionTableFilterEvent.FILTER_BY_TEXT);
    }

    @Override
    protected void addNewItem(final ClickEvent event) {
        final Window newDistWindow = addUpdateWindowLayout.getWindow();
        newDistWindow.setCaption(i18n.get("caption.add.new.dist"));
        UI.getCurrent().addWindow(newDistWindow);
        newDistWindow.setVisible(Boolean.TRUE);
        eventbus.publish(this, DragEvent.HIDE_DROP_HINT);
    }

    @PreDestroy
    void destroy() {
        eventbus.unsubscribe(this);
    }

    @Override
    protected Boolean isAddNewItemAllowed() {
        return Boolean.TRUE;
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
