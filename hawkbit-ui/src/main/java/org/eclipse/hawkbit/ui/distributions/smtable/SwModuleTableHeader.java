/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.distributions.smtable;

import org.eclipse.hawkbit.ui.artifacts.event.SMFilterEvent;
import org.eclipse.hawkbit.ui.artifacts.event.SoftwareModuleEvent;
import org.eclipse.hawkbit.ui.artifacts.smtable.SoftwareModuleAddUpdateWindow;
import org.eclipse.hawkbit.ui.common.table.AbstractTableHeader;
import org.eclipse.hawkbit.ui.common.table.BaseEntityEventType;
import org.eclipse.hawkbit.ui.distributions.event.DistributionsUIEvent;
import org.eclipse.hawkbit.ui.distributions.state.ManageDistUIState;
import org.eclipse.hawkbit.ui.utils.SPUIComponetIdProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.vaadin.event.dd.DropHandler;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;

/**
 * Implementation of software module Header block using generic abstract details
 * style .
 */
@SpringComponent
@ViewScope
public class SwModuleTableHeader extends AbstractTableHeader {

    private static final long serialVersionUID = 242961845006626297L;

    @Autowired
    private ManageDistUIState manageDistUIState;

    @Autowired
    private SoftwareModuleAddUpdateWindow softwareModuleAddUpdateWindow;

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final DistributionsUIEvent event) {
        if (event == DistributionsUIEvent.HIDE_SM_FILTER_BY_TYPE) {
            setFilterButtonsIconVisible(true);
        }
    }

    @Override
    protected String getHeaderCaption() {
        return i18n.get("upload.swModuleTable.header");
    }

    @Override
    protected String getSearchBoxId() {
        return SPUIComponetIdProvider.SW_MODULE_SEARCH_TEXT_FIELD;
    }

    @Override
    protected String getSearchRestIconId() {
        return SPUIComponetIdProvider.SW_MODULE_SEARCH_RESET_ICON;
    }

    @Override
    protected String getAddIconId() {
        return SPUIComponetIdProvider.SW_MODULE_ADD_BUTTON;
    }

    @Override
    protected String onLoadSearchBoxValue() {
        if (manageDistUIState.getSoftwareModuleFilters().getSearchText().isPresent()) {
            return manageDistUIState.getSoftwareModuleFilters().getSearchText().get();
        }
        return null;
    }

    @Override
    protected String getDropFilterId() {
        /* not required */
        return null;
    }

    @Override
    protected boolean isDropHintRequired() {
        return false;
    }

    @Override
    protected boolean isDropFilterRequired() {
        /* Not Yet Implemented */
        return false;
    }

    @Override
    protected String getShowFilterButtonLayoutId() {
        return "show.type.icon";
    }

    @Override
    protected void showFilterButtonsLayout() {
        manageDistUIState.setSwTypeFilterClosed(false);
        eventbus.publish(this, DistributionsUIEvent.SHOW_SM_FILTER_BY_TYPE);
    }

    @Override
    protected void resetSearchText() {
        if (manageDistUIState.getSoftwareModuleFilters().getSearchText().isPresent()) {
            manageDistUIState.getSoftwareModuleFilters().setSearchText(null);
            eventbus.publish(this, SMFilterEvent.REMOVER_FILTER_BY_TEXT);
        }
    }

    @Override
    protected String getMaxMinIconId() {
        return SPUIComponetIdProvider.SW_MAX_MIN_TABLE_ICON;
    }

    @Override
    public void maximizeTable() {
        manageDistUIState.setSwModuleTableMaximized(Boolean.TRUE);
        eventbus.publish(this, new SoftwareModuleEvent(BaseEntityEventType.MAXIMIZED, null));

    }

    @Override
    public void minimizeTable() {
        manageDistUIState.setSwModuleTableMaximized(Boolean.FALSE);
        eventbus.publish(this, new SoftwareModuleEvent(BaseEntityEventType.MINIMIZED, null));
    }

    @Override
    public Boolean onLoadIsTableMaximized() {
        return manageDistUIState.isSwModuleTableMaximized();
    }

    @Override
    public Boolean onLoadIsShowFilterButtonDisplayed() {
        return manageDistUIState.isSwTypeFilterClosed();
    }

    @Override
    protected void searchBy(final String newSearchText) {
        manageDistUIState.getSoftwareModuleFilters().setSearchText(newSearchText);
        eventbus.publish(this, SMFilterEvent.FILTER_BY_TEXT);
    }

    @Override
    protected void addNewItem(final ClickEvent event) {
        final Window addSoftwareModule = softwareModuleAddUpdateWindow.createAddSoftwareModuleWindow();
        addSoftwareModule.setCaption(i18n.get("upload.caption.add.new.swmodule"));
        UI.getCurrent().addWindow(addSoftwareModule);
        addSoftwareModule.setVisible(Boolean.TRUE);
    }

    @Override
    protected boolean hasCreatePermission() {
        return permChecker.hasCreateDistributionPermission();
    }

    @Override
    protected Boolean isAddNewItemAllowed() {
        return true;
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
