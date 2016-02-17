/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.dstable;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.eclipse.hawkbit.repository.SpPermissionChecker;
import org.eclipse.hawkbit.ui.common.table.AbstractTableHeader;
import org.eclipse.hawkbit.ui.management.event.DistributionTableEvent;
import org.eclipse.hawkbit.ui.management.event.DistributionTableEvent.DistributionComponentEvent;
import org.eclipse.hawkbit.ui.management.event.DistributionTableFilterEvent;
import org.eclipse.hawkbit.ui.management.event.DragEvent;
import org.eclipse.hawkbit.ui.management.event.ManagementUIEvent;
import org.eclipse.hawkbit.ui.management.state.ManagementUIState;
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
 * 
 *
 */
@SpringComponent
@ViewScope
public class DistributionTableHeader extends AbstractTableHeader {
    private static final long serialVersionUID = 7597766804650170127L;

    @Autowired
    private I18N i18n;

    @Autowired
    private SpPermissionChecker permChecker;

    @Autowired
    private transient EventBus.SessionEventBus eventbus;

    @Autowired
    private ManagementUIState managementUIState;

    @Autowired
    private DistributionAddUpdateWindowLayout distributionAddUpdateWindowLayout;

    /**
     * Initialize the component.
     */
    @PostConstruct
    protected void init() {
        super.init();
        eventbus.subscribe(this);
    }

    @PreDestroy
    void destroy() {
        eventbus.unsubscribe(this);
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final ManagementUIEvent event) {
        if (event == ManagementUIEvent.HIDE_DISTRIBUTION_TAG_LAYOUT) {
            setFilterButtonsIconVisible(true);
        } else if (event == ManagementUIEvent.SHOW_DISTRIBUTION_TAG_LAYOUT) {
            setFilterButtonsIconVisible(false);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see hawkbit.server.ui.common.table.AbstractTableHeader#getHeaderCaption(
     * )
     */
    @Override
    protected String getHeaderCaption() {
        return i18n.get("header.dist.table");
    }

    /*
     * (non-Javadoc)
     * 
     * @see hawkbit.server.ui.common.table.AbstractTableHeader#getSearchBoxId()
     */
    @Override
    protected String getSearchBoxId() {
        return SPUIComponetIdProvider.DIST_SEARCH_TEXTFIELD;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.hawkbit.server.ui.common.table.AbstractTableHeader#
     * getSearchRestIconId()
     */
    @Override
    protected String getSearchRestIconId() {
        return SPUIComponetIdProvider.DIST_SEARCH_ICON;
    }

    /*
     * (non-Javadoc)
     * 
     * @see hawkbit.server.ui.common.table.AbstractTableHeader#getAddIconId()
     */
    @Override
    protected String getAddIconId() {
        return SPUIComponetIdProvider.DIST_ADD_ICON;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.hawkbit.server.ui.common.table.AbstractTableHeader#
     * onLoadSearchBoxValue()
     */
    @Override
    protected String onLoadSearchBoxValue() {
        if (managementUIState.getDistributionTableFilters().getSearchText().isPresent()) {
            return managementUIState.getDistributionTableFilters().getSearchText().get();
        }
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see hawkbit.server.ui.common.table.AbstractTableHeader#getDropFilterId()
     */
    @Override
    protected String getDropFilterId() {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.hawkbit.server.ui.common.table.AbstractTableHeader#
     * hasCreatePermission()
     */
    @Override
    protected boolean hasCreatePermission() {
        return permChecker.hasCreateDistributionPermission();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.hawkbit.server.ui.common.table.AbstractTableHeader#
     * isDropHintRequired()
     */
    @Override
    protected boolean isDropHintRequired() {
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.hawkbit.server.ui.common.table.AbstractTableHeader#
     * isDropFilterRequired()
     */
    @Override
    protected boolean isDropFilterRequired() {
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.hawkbit.server.ui.common.table.AbstractTableHeader#
     * getShowFilterButtonLayoutId()
     */
    @Override
    protected String getShowFilterButtonLayoutId() {
        return "show.dist.tags.icon";
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.hawkbit.server.ui.common.table.AbstractTableHeader#
     * showFilterButtonsLayout()
     */
    @Override
    protected void showFilterButtonsLayout() {
        managementUIState.setDistTagFilterClosed(false);
        eventbus.publish(this, ManagementUIEvent.SHOW_DISTRIBUTION_TAG_LAYOUT);
    }

    /*
     * (non-Javadoc)
     * 
     * @see hawkbit.server.ui.common.table.AbstractTableHeader#resetSearchText()
     */
    @Override
    protected void resetSearchText() {
        if( managementUIState.getDistributionTableFilters().getSearchText().isPresent()){
            managementUIState.getDistributionTableFilters().setSearchText(null);
            eventbus.publish(this, DistributionTableFilterEvent.REMOVE_FILTER_BY_TEXT);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see hawkbit.server.ui.common.table.AbstractTableHeader#getMaxMinIconId()
     */
    @Override
    protected String getMaxMinIconId() {
        return SPUIComponetIdProvider.DS_MAX_MIN_TABLE_ICON;
    }

    /*
     * (non-Javadoc)
     * 
     * @see hawkbit.server.ui.common.table.AbstractTableHeader#maximizeTable()
     */
    @Override
    public void maximizeTable() {
        managementUIState.setDsTableMaximized(Boolean.TRUE);
        eventbus.publish(this, new DistributionTableEvent(DistributionComponentEvent.MAXIMIZED, null));
    }

    /*
     * (non-Javadoc)
     * 
     * @see hawkbit.server.ui.common.table.AbstractTableHeader#minimizeTable()
     */
    @Override
    public void minimizeTable() {
        managementUIState.setDsTableMaximized(Boolean.FALSE);
        eventbus.publish(this, new DistributionTableEvent(DistributionComponentEvent.MINIMIZED, null));
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.hawkbit.server.ui.common.table.AbstractTableHeader#
     * onLoadIsTableMaximized()
     */
    @Override
    public Boolean onLoadIsTableMaximized() {
        return managementUIState.isDsTableMaximized();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.hawkbit.server.ui.common.table.AbstractTableHeader#
     * onLoadIsShowFilterButtonDisplayed()
     */
    @Override
    public Boolean onLoadIsShowFilterButtonDisplayed() {
        return managementUIState.isDistTagFilterClosed();
    }

    /*
     * (non-Javadoc)
     * 
     * @see hawkbit.server.ui.common.table.AbstractTableHeader#searchBy(java.
     * lang.String)
     */
    @Override
    protected void searchBy(final String newSearchText) {
        managementUIState.getDistributionTableFilters().setSearchText(newSearchText);
        eventbus.publish(this, DistributionTableFilterEvent.FILTER_BY_TEXT);
    }

    /*
     * (non-Javadoc)
     * 
     * @see hawkbit.server.ui.common.table.AbstractTableHeader#addNewItem(com.
     * vaadin.ui.Button.ClickEvent )
     */
    @Override
    protected void addNewItem(final ClickEvent event) {
        final Window newDistWindow = distributionAddUpdateWindowLayout.getWindow();
        newDistWindow.setCaption(i18n.get("caption.add.new.dist"));
        UI.getCurrent().addWindow(newDistWindow);
        newDistWindow.setVisible(Boolean.TRUE);
        eventbus.publish(this, DragEvent.HIDE_DROP_HINT);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.hawkbit.server.ui.common.table.AbstractTableHeader#
     * isAddNewItemAllowed()
     */
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

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.hawkbit.server.ui.common.table.AbstractTableHeader#
     * getBulkUploadIconId()
     */
    @Override
    protected String getBulkUploadIconId() {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see hawkbit.server.ui.common.table.AbstractTableHeader#bulkUpload(com.
     * vaadin.ui.Button.ClickEvent )
     */
    @Override
    protected void bulkUpload(final ClickEvent event) {
        /**
         * No implementation as no bulk upload is supported.
         */
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.hawkbit.server.ui.common.table.AbstractTableHeader#
     * isBulkUploadAllowed()
     */
    @Override
    protected Boolean isBulkUploadAllowed() {
        return Boolean.FALSE;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.hawkbit.ui.common.table.AbstractTableHeader#
     * isBulkUploadInProgress()
     */
    @Override
    protected boolean isBulkUploadInProgress() {
        return false;
    }
}
