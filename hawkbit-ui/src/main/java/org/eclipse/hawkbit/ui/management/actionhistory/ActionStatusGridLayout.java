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
import org.eclipse.hawkbit.ui.common.UIConfiguration;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyAction;
import org.eclipse.hawkbit.ui.common.event.EventLayout;
import org.eclipse.hawkbit.ui.common.event.EventLayoutViewAware;
import org.eclipse.hawkbit.ui.common.event.EventView;
import org.eclipse.hawkbit.ui.common.event.SelectionChangedEventPayload.SelectionChangedEventType;
import org.eclipse.hawkbit.ui.common.layout.AbstractGridComponentLayout;
import org.eclipse.hawkbit.ui.common.layout.MasterEntityAwareComponent;
import org.eclipse.hawkbit.ui.common.layout.listener.SelectionChangedListener;

/**
 * Layout responsible for action-states-grid and the corresponding header.
 */
public class ActionStatusGridLayout extends AbstractGridComponentLayout {
    private static final long serialVersionUID = 1L;

    private final ActionStatusGridHeader actionStatusGridHeader;
    private final ActionStatusGrid actionStatusGrid;

    private final transient SelectionChangedListener<ProxyAction> selectionChangedListener;

    /**
     * Constructor for ActionStatusGridLayout
     *
     * @param uiConfig
     *            {@link UIConfiguration}
     * @param deploymentManagement
     *            DeploymentManagement
     */
    public ActionStatusGridLayout(final UIConfiguration uiConfig, final DeploymentManagement deploymentManagement) {
        this.actionStatusGridHeader = new ActionStatusGridHeader(uiConfig.getI18n());
        this.actionStatusGrid = new ActionStatusGrid(uiConfig, deploymentManagement);

        final EventLayoutViewAware masterLayoutView = new EventLayoutViewAware(EventLayout.ACTION_HISTORY_LIST,
                EventView.DEPLOYMENT);
        this.selectionChangedListener = new SelectionChangedListener<>(uiConfig.getEventBus(), masterLayoutView,
                getMasterEntityAwareComponents());

        buildLayout(actionStatusGridHeader, actionStatusGrid);
    }

    private List<MasterEntityAwareComponent<ProxyAction>> getMasterEntityAwareComponents() {
        final Long previousMasterActionId = actionStatusGrid.getMasterEntitySupport().getMasterId();

        return Arrays.asList(actionStatusGrid.getMasterEntitySupport(),
                masterAction -> reselectActionStatus(masterAction, previousMasterActionId));
    }

    private void reselectActionStatus(final ProxyAction masterAction, final Long previousMasterActionId) {
        if (masterAction == null) {
            actionStatusGrid.getSelectionSupport().deselectAll();
            return;
        }

        if (masterAction.getId().equals(previousMasterActionId)) {
            updatedActionStatusMessages();
            return;
        }

        actionStatusGrid.getSelectionSupport().selectFirstRow();
    }

    private void updatedActionStatusMessages() {
        actionStatusGrid.getSelectionSupport().getSelectedEntity().ifPresent(selectedActionStatus ->
        // we do not need to fetch the updated action status from backend
        // here, because we only need to refresh messages based on id
        actionStatusGrid.getSelectionSupport().sendSelectionChangedEvent(SelectionChangedEventType.ENTITY_SELECTED,
                selectedActionStatus));
    }

    /**
     * Enable the single selection in action status grid
     */
    public void enableSelection() {
        actionStatusGrid.getSelectionSupport().enableSingleSelection();
    }

    /**
     * Disable the selection in action status grid
     */
    public void disableSelection() {
        actionStatusGrid.getSelectionSupport().disableSelection();
    }

    /**
     * Unsubscribe the changed listener
     */
    public void unsubscribeListener() {
        selectionChangedListener.unsubscribe();
    }
}
