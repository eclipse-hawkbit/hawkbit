/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts.smtable;

import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.artifacts.event.SMFilterEvent;
import org.eclipse.hawkbit.ui.artifacts.event.SoftwareModuleEvent;
import org.eclipse.hawkbit.ui.artifacts.event.UploadArtifactUIEvent;
import org.eclipse.hawkbit.ui.artifacts.state.ArtifactUploadState;
import org.eclipse.hawkbit.ui.common.table.AbstractTableHeader;
import org.eclipse.hawkbit.ui.common.table.BaseEntityEventType;
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
 * Header of Software module table.
 */
public class SoftwareModuleTableHeader extends AbstractTableHeader {

    private static final long serialVersionUID = 242961845006626297L;

    private final SoftwareModuleAddUpdateWindow softwareModuleAddUpdateWindow;

    SoftwareModuleTableHeader(final I18N i18n, final SpPermissionChecker permChecker, final UIEventBus eventbus,
            final ArtifactUploadState artifactUploadState,
            final SoftwareModuleAddUpdateWindow softwareModuleAddUpdateWindow) {
        super(i18n, permChecker, eventbus, null, null, artifactUploadState);
        this.softwareModuleAddUpdateWindow = softwareModuleAddUpdateWindow;
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onEvent(final UploadArtifactUIEvent event) {
        if (event == UploadArtifactUIEvent.HIDE_FILTER_BY_TYPE) {
            setFilterButtonsIconVisible(true);
        }
    }

    @Override
    protected String getHeaderCaption() {
        return i18n.get("upload.swModuleTable.header");
    }

    @Override
    protected String getSearchBoxId() {
        return UIComponentIdProvider.SW_MODULE_SEARCH_TEXT_FIELD;
    }

    @Override
    protected String getSearchRestIconId() {
        return UIComponentIdProvider.SW_MODULE_SEARCH_RESET_ICON;
    }

    @Override
    protected String getAddIconId() {
        return UIComponentIdProvider.SW_MODULE_ADD_BUTTON;
    }

    @Override
    protected String onLoadSearchBoxValue() {
        return artifactUploadState.getSoftwareModuleFilters().getSearchText().orElse(null);
    }

    @Override
    protected String getDropFilterId() {
        /* No dropping on software module table header in Upload View */
        return null;
    }

    @Override
    protected boolean hasCreatePermission() {
        return permChecker.hasCreateDistributionPermission();
    }

    @Override
    protected boolean isDropHintRequired() {
        /* No dropping on software module table header in Upload View */
        return false;
    }

    @Override
    protected boolean isDropFilterRequired() {
        /* No dropping on software module table header in Upload View */
        return false;
    }

    @Override
    protected String getShowFilterButtonLayoutId() {
        return "show.type.icon";
    }

    @Override
    protected void showFilterButtonsLayout() {
        artifactUploadState.setSwTypeFilterClosed(false);
        eventbus.publish(this, UploadArtifactUIEvent.SHOW_FILTER_BY_TYPE);

    }

    @Override
    protected void resetSearchText() {
        if (artifactUploadState.getSoftwareModuleFilters().getSearchText().isPresent()) {
            artifactUploadState.getSoftwareModuleFilters().setSearchText(null);
            eventbus.publish(this, SMFilterEvent.REMOVER_FILTER_BY_TEXT);
        }
    }

    @Override
    protected String getMaxMinIconId() {
        return UIComponentIdProvider.SW_MAX_MIN_TABLE_ICON;
    }

    @Override
    public void maximizeTable() {
        artifactUploadState.setSwModuleTableMaximized(Boolean.TRUE);
        eventbus.publish(this, new SoftwareModuleEvent(BaseEntityEventType.MAXIMIZED));

    }

    @Override
    public void minimizeTable() {
        artifactUploadState.setSwModuleTableMaximized(Boolean.FALSE);
        eventbus.publish(this, new SoftwareModuleEvent(BaseEntityEventType.MINIMIZED));
    }

    @Override
    public Boolean onLoadIsTableMaximized() {
        return artifactUploadState.isSwModuleTableMaximized();
    }

    @Override
    public Boolean onLoadIsShowFilterButtonDisplayed() {
        return artifactUploadState.isSwTypeFilterClosed();
    }

    @Override
    protected void searchBy(final String newSearchText) {
        artifactUploadState.getSoftwareModuleFilters().setSearchText(newSearchText);
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
