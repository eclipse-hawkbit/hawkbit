/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.distributions.smtable;

import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.artifacts.event.RefreshSoftwareModuleByFilterEvent;
import org.eclipse.hawkbit.ui.artifacts.event.SoftwareModuleEvent;
import org.eclipse.hawkbit.ui.artifacts.smtable.SoftwareModuleAddUpdateWindow;
import org.eclipse.hawkbit.ui.common.table.AbstractSoftwareModuleTableHeader;
import org.eclipse.hawkbit.ui.common.table.BaseEntityEventType;
import org.eclipse.hawkbit.ui.distributions.event.DistributionsUIEvent;
import org.eclipse.hawkbit.ui.distributions.state.ManageDistUIState;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus.UIEventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

/**
 * Implementation of software module Header block using generic abstract details
 * style .
 */
public class SwModuleTableHeader extends AbstractSoftwareModuleTableHeader {

    private static final long serialVersionUID = 1L;

    SwModuleTableHeader(final VaadinMessageSource i18n, final SpPermissionChecker permChecker,
            final UIEventBus eventbus, final ManageDistUIState manageDistUIstate,
            final SoftwareModuleAddUpdateWindow softwareModuleAddUpdateWindow) {
        super(i18n, permChecker, eventbus, null, manageDistUIstate, null, softwareModuleAddUpdateWindow);
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onEvent(final DistributionsUIEvent event) {
        if (event == DistributionsUIEvent.HIDE_SM_FILTER_BY_TYPE) {
            setFilterButtonsIconVisible(true);
        }
    }

    @Override
    protected String onLoadSearchBoxValue() {
        return getManageDistUIstate().getSoftwareModuleFilters().getSearchText().orElse(null);
    }

    @Override
    protected void showFilterButtonsLayout() {
        getManageDistUIstate().setSwTypeFilterClosed(false);
        eventbus.publish(this, DistributionsUIEvent.SHOW_SM_FILTER_BY_TYPE);
    }

    @Override
    protected void resetSearchText() {
        if (getManageDistUIstate().getSoftwareModuleFilters().getSearchText().isPresent()) {
            getManageDistUIstate().getSoftwareModuleFilters().setSearchText(null);
            eventbus.publish(this, new RefreshSoftwareModuleByFilterEvent());
        }
    }

    @Override
    public void maximizeTable() {
        getManageDistUIstate().setSwModuleTableMaximized(Boolean.TRUE);
        eventbus.publish(this, new SoftwareModuleEvent(BaseEntityEventType.MAXIMIZED));

    }

    @Override
    public void minimizeTable() {
        getManageDistUIstate().setSwModuleTableMaximized(Boolean.FALSE);
        eventbus.publish(this, new SoftwareModuleEvent(BaseEntityEventType.MINIMIZED));
    }

    @Override
    public Boolean onLoadIsTableMaximized() {
        return getManageDistUIstate().isSwModuleTableMaximized();
    }

    @Override
    public Boolean onLoadIsShowFilterButtonDisplayed() {
        return getManageDistUIstate().isSwTypeFilterClosed();
    }

    @Override
    protected void searchBy(final String newSearchText) {
        getManageDistUIstate().getSoftwareModuleFilters().setSearchText(newSearchText);
        eventbus.publish(this, new RefreshSoftwareModuleByFilterEvent());
    }

}
