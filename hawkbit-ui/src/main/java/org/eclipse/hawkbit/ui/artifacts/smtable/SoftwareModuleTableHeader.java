/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts.smtable;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.eclipse.hawkbit.repository.SpPermissionChecker;
import org.eclipse.hawkbit.ui.artifacts.event.SMFilterEvent;
import org.eclipse.hawkbit.ui.artifacts.event.SoftwareModuleEvent;
import org.eclipse.hawkbit.ui.artifacts.event.SoftwareModuleEvent.SoftwareModuleEventType;
import org.eclipse.hawkbit.ui.artifacts.event.UploadArtifactUIEvent;
import org.eclipse.hawkbit.ui.artifacts.state.ArtifactUploadState;
import org.eclipse.hawkbit.ui.common.table.AbstractTableHeader;
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
 * Header of Software module table.
 * 
 *
 * 
 */
@SpringComponent
@ViewScope
public class SoftwareModuleTableHeader extends AbstractTableHeader {

    private static final long serialVersionUID = 242961845006626297L;

    @Autowired
    private I18N i18n;

    @Autowired
    private SpPermissionChecker permChecker;

    @Autowired
    private transient EventBus.SessionEventBus eventbus;

    @Autowired
    private ArtifactUploadState artifactUploadState;

    @Autowired
    private SoftwareModuleAddUpdateWindow softwareModuleAddUpdateWindow;

    /**
     * Initialize the components.
     */
    @PostConstruct
    protected void init() {
        super.init();
        eventbus.subscribe(this);
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final UploadArtifactUIEvent event) {
        if (event == UploadArtifactUIEvent.HIDE_FILTER_BY_TYPE) {
            setFilterButtonsIconVisible(true);
        }
    }

    @PreDestroy
    void destroy() {
        /*
         * It's good manners to do this, even though vaadin-spring will
         * automatically unsubscribe when this UI is garbage collected.
         */
        eventbus.unsubscribe(this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.hawkbit.server.ui.common.table.SPTableHeader#getHeaderCaption
     * ()
     */
    @Override
    protected String getHeaderCaption() {
        return i18n.get("upload.swModuleTable.header");
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.hawkbit.server.ui.common.table.SPTableHeader#getSearchBoxId()
     */
    @Override
    protected String getSearchBoxId() {
        return SPUIComponetIdProvider.SW_MODULE_SEARCH_TEXT_FIELD;
    }

    /*
     * (non-Javadoc)
     * 
     * @see hawkbit.server.ui.common.table.SPTableHeader#getSearchRestIconId()
     */
    @Override
    protected String getSearchRestIconId() {
        return SPUIComponetIdProvider.SW_MODULE_SEARCH_RESET_ICON;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.hawkbit.server.ui.common.table.SPTableHeader#getAddIconId()
     */
    @Override
    protected String getAddIconId() {
        return SPUIComponetIdProvider.SW_MODULE_ADD_BUTTON;
    }

    /*
     * (non-Javadoc)
     * 
     * @see hawkbit.server.ui.common.table.SPTableHeader#onLoadSearchBoxValue()
     */
    @Override
    protected String onLoadSearchBoxValue() {
        if (artifactUploadState.getSoftwareModuleFilters().getSearchText().isPresent()) {
            return artifactUploadState.getSoftwareModuleFilters().getSearchText().get();
        }
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.hawkbit.server.ui.common.table.SPTableHeader#getDropFilterId(
     * )
     */
    @Override
    protected String getDropFilterId() {
        /* No dropping on software module table header in Upload View */
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see hawkbit.server.ui.common.table.SPTableHeader#hasCreatePermission()
     */
    @Override
    protected boolean hasCreatePermission() {
        return permChecker.hasCreateDistributionPermission();
    }

    /*
     * (non-Javadoc)
     * 
     * @see hawkbit.server.ui.common.table.SPTableHeader#isDropHintRequired()
     */
    @Override
    protected boolean isDropHintRequired() {
        /* No dropping on software module table header in Upload View */
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see hawkbit.server.ui.common.table.SPTableHeader#isDropFilterRequired()
     */
    @Override
    protected boolean isDropFilterRequired() {
        /* No dropping on software module table header in Upload View */
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.hawkbit.server.ui.common.table.SPTableHeader#
     * getShowFilterButtonLayoutId()
     */
    @Override
    protected String getShowFilterButtonLayoutId() {
        return "show.type.icon";
    }

    /*
     * (non-Javadoc)
     * 
     * @see hawkbit.server.ui.common.table.SPTableHeader#showFilterButtonsLayout
     * ()
     */
    @Override
    protected void showFilterButtonsLayout() {
        artifactUploadState.setSwTypeFilterClosed(false);
        eventbus.publish(this, UploadArtifactUIEvent.SHOW_FILTER_BY_TYPE);

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.hawkbit.server.ui.common.table.SPTableHeader#resetSearchText(
     * )
     */
    @Override
    protected void resetSearchText() {
        if(artifactUploadState.getSoftwareModuleFilters().getSearchText().isPresent()){
            artifactUploadState.getSoftwareModuleFilters().setSearchText(null);
            eventbus.publish(this, SMFilterEvent.REMOVER_FILTER_BY_TEXT);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.hawkbit.server.ui.common.table.SPTableHeader#getMaxMinIconId(
     * )
     */
    @Override
    protected String getMaxMinIconId() {
        return SPUIComponetIdProvider.SW_MAX_MIN_TABLE_ICON;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.hawkbit.server.ui.common.table.SPTableHeader#maximizeTable()
     */
    @Override
    public void maximizeTable() {
        artifactUploadState.setSwModuleTableMaximized(Boolean.TRUE);
        eventbus.publish(this, new SoftwareModuleEvent(SoftwareModuleEventType.MAXIMIZED, null));

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.hawkbit.server.ui.common.table.SPTableHeader#minimizeTable()
     */
    @Override
    public void minimizeTable() {
        artifactUploadState.setSwModuleTableMaximized(Boolean.FALSE);
        eventbus.publish(this, new SoftwareModuleEvent(SoftwareModuleEventType.MINIMIZED, null));
    }

    /*
     * (non-Javadoc)
     * 
     * @see hawkbit.server.ui.common.table.SPTableHeader#onLoadIsTableMaximized(
     * )
     */
    @Override
    public Boolean onLoadIsTableMaximized() {
        return artifactUploadState.isSwModuleTableMaximized();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.hawkbit.server.ui.common.table.SPTableHeader#
     * onLoadIsShowFilterButtonDisplayed()
     */
    @Override
    public Boolean onLoadIsShowFilterButtonDisplayed() {
        return artifactUploadState.isSwTypeFilterClosed();
    }

    /*
     * (non-Javadoc)
     * 
     * @see hawkbit.server.ui.common.table.SPTableHeader#searchBy(java.lang.
     * String)
     */
    @Override
    protected void searchBy(final String newSearchText) {
        artifactUploadState.getSoftwareModuleFilters().setSearchText(newSearchText);
        eventbus.publish(this, SMFilterEvent.FILTER_BY_TEXT);
    }

    /*
     * (non-Javadoc)
     * 
     * @see hawkbit.server.ui.common.table.SPTableHeader#addNewItem(com.vaadin.
     * ui.Button.ClickEvent)
     */
    @Override
    protected void addNewItem(final ClickEvent event) {
        final Window addSoftwareModule = softwareModuleAddUpdateWindow.createAddSoftwareModuleWindow();
        addSoftwareModule.setCaption(i18n.get("upload.caption.add.new.swmodule"));
        UI.getCurrent().addWindow(addSoftwareModule);
        addSoftwareModule.setVisible(Boolean.TRUE);
    }

    /*
     * (non-Javadoc)
     * 
     * @see hawkbit.server.ui.common.table.AbstractTableHeader#canAddNewItem()
     */
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
