/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.actionhistory;

import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.ui.common.table.BaseEntityEventType;
import org.eclipse.hawkbit.ui.management.event.TargetTableEvent;
import org.eclipse.hawkbit.ui.management.state.ManagementUIState;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.EventBus.UIEventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

/**
 *
 *
 */
public class ActionHistoryComponent extends VerticalLayout {
    private static final long serialVersionUID = -3766179797384539821L;

    private final ActionHistoryHeader actionHistoryHeader;
    private final ActionHistoryTable actionHistoryTable;
    private final EventBus.UIEventBus eventBus;

    public ActionHistoryComponent(final I18N i18n, final DeploymentManagement deploymentManagement,
            final UIEventBus eventBus, final UINotification notification, final ManagementUIState managementUIState) {
        this.actionHistoryHeader = new ActionHistoryHeader(eventBus, managementUIState);
        this.actionHistoryTable = new ActionHistoryTable(i18n, deploymentManagement, eventBus, notification,
                managementUIState);
        this.eventBus = eventBus;
        buildLayout();
        setSizeFull();
        setImmediate(true);
        eventBus.subscribe(this);
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onEvent(final TargetTableEvent targetUIEvent) {
        if (BaseEntityEventType.SELECTED_ENTITY == targetUIEvent.getEventType()) {
            setData(SPUIDefinitions.DATA_AVAILABLE);
            UI.getCurrent().access(() -> populateActionHistoryDetails(targetUIEvent.getEntity()));
        }
    }

    private void buildLayout() {
        setSizeFull();
        setSpacing(false);
        setMargin(false);
        addStyleName("table-layout");
        addComponents(actionHistoryHeader, actionHistoryTable);
        setComponentAlignment(actionHistoryHeader, Alignment.TOP_CENTER);
        setComponentAlignment(actionHistoryTable, Alignment.TOP_CENTER);
        setExpandRatio(actionHistoryTable, 1.0F);
    }

    /**
     * populate action header and table for the target.
     * 
     * @param target
     *            the target
     */
    public void populateActionHistoryDetails(final Target target) {
        if (null != target) {
            actionHistoryHeader.populateHeader(target.getName());
            actionHistoryTable.setAlreadyHasMessages(false);
            actionHistoryTable.populateTableData(target);
        } else {
            actionHistoryHeader.updateActionHistoryHeader(" ");
            actionHistoryTable.setAlreadyHasMessages(false);
            actionHistoryTable.clearContainerData();
        }

    }
}
