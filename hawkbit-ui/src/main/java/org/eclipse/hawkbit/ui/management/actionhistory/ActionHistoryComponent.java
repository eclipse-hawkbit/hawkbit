/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.actionhistory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.ui.management.event.TargetTableEvent;
import org.eclipse.hawkbit.ui.management.event.TargetTableEvent.TargetComponentEvent;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

/**
 *
 *
 */
@SpringComponent
@ViewScope
public class ActionHistoryComponent extends VerticalLayout {
    private static final long serialVersionUID = -3766179797384539821L;

    @Autowired
    private ActionHistoryHeader actionHistoryHeader;

    @Autowired
    private ActionHistoryTable actionHistoryTable;

    @Autowired
    private transient EventBus.SessionEventBus eventBus;

    /**
     * Initialize the Action History Component.
     */
    @PostConstruct
    public void init() {
        buildLayout();
        setSizeFull();
        setImmediate(true);
        eventBus.subscribe(this);
    }

    @PreDestroy
    void destroy() {
        eventBus.unsubscribe(this);
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final TargetTableEvent targetUIEvent) {
        if (targetUIEvent.getTargetComponentEvent() == TargetComponentEvent.SELECTED_TARGET) {
            setData(SPUIDefinitions.DATA_AVAILABLE);
            UI.getCurrent().access(() -> populateActionHistoryDetails(targetUIEvent.getTarget()));
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
        setExpandRatio(actionHistoryTable, 1.0f);
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
