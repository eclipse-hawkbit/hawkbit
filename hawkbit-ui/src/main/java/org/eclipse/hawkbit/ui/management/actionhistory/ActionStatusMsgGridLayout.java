/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.actionhistory;

import java.util.Arrays;
import java.util.List;

import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyActionStatus;
import org.eclipse.hawkbit.ui.common.event.EventLayout;
import org.eclipse.hawkbit.ui.common.event.EventLayoutViewAware;
import org.eclipse.hawkbit.ui.common.event.EventView;
import org.eclipse.hawkbit.ui.common.layout.AbstractGridComponentLayout;
import org.eclipse.hawkbit.ui.common.layout.MasterEntityAwareComponent;
import org.eclipse.hawkbit.ui.common.layout.listener.SelectionChangedListener;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus.UIEventBus;

/**
 * Layout responsible for messages-grid and the corresponding header.
 */
public class ActionStatusMsgGridLayout extends AbstractGridComponentLayout {
    private static final long serialVersionUID = 1L;

    private final ActionStatusMsgGridHeader actionStatusMsgHeader;
    private final ActionStatusMsgGrid actionStatusMsgGrid;

    private final transient SelectionChangedListener<ProxyActionStatus> selectionChangedListener;

    /**
     * Constructor for ActionStatusMsgGridLayout
     *
     * @param i18n
     *          VaadinMessageSource
     * @param eventBus
     *          UIEventBus
     * @param deploymentManagement
     *          DeploymentManagement
     */
    public ActionStatusMsgGridLayout(final VaadinMessageSource i18n, final UIEventBus eventBus,
            final DeploymentManagement deploymentManagement) {
        this.actionStatusMsgHeader = new ActionStatusMsgGridHeader(i18n);
        this.actionStatusMsgGrid = new ActionStatusMsgGrid(i18n, eventBus, deploymentManagement);

        final EventLayoutViewAware masterLayoutView = new EventLayoutViewAware(EventLayout.ACTION_HISTORY_STATUS_LIST,
                EventView.DEPLOYMENT);
        this.selectionChangedListener = new SelectionChangedListener<>(eventBus, masterLayoutView,
                getMasterEntityAwareComponents());

        buildLayout(actionStatusMsgHeader, actionStatusMsgGrid);
    }

    private List<MasterEntityAwareComponent<ProxyActionStatus>> getMasterEntityAwareComponents() {
        return Arrays.asList(actionStatusMsgGrid.getMasterEntitySupport(), this::selectActionStatusMsg);
    }

    private void selectActionStatusMsg(final ProxyActionStatus masterAction) {
        if (masterAction == null) {
            actionStatusMsgGrid.getSelectionSupport().deselectAll();
            return;
        }

        actionStatusMsgGrid.getSelectionSupport().selectFirstRow();
    }

    /**
     * Enable the single selection in action status message grid
     */
    public void enableSelection() {
        actionStatusMsgGrid.getSelectionSupport().enableSingleSelection();
    }

    /**
     * Disable the selection in action status message grid
     */
    public void disableSelection() {
        actionStatusMsgGrid.getSelectionSupport().disableSelection();
    }

    /**
     * Unsubscribe the changed listener
     */
    public void unsubscribeListener() {
        selectionChangedListener.unsubscribe();
    }
}
