/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.targettag.filter;

import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement;
import org.eclipse.hawkbit.repository.TargetTagManagement;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.common.event.FilterHeaderEvent.FilterHeaderEnum;
import org.eclipse.hawkbit.ui.common.event.TargetTagFilterHeaderEvent;
import org.eclipse.hawkbit.ui.components.RefreshableContainer;
import org.eclipse.hawkbit.ui.dd.criteria.ManagementViewClientCriterion;
import org.eclipse.hawkbit.ui.management.event.ManagementUIEvent;
import org.eclipse.hawkbit.ui.management.event.TargetTagTableEvent;
import org.eclipse.hawkbit.ui.management.state.ManagementUIState;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus.UIEventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

/**
 * Target Tag filter layout.
 */
public class TargetTagFilterLayout extends AbstractTargetTagFilterLayout implements RefreshableContainer {

    private static final long serialVersionUID = 1L;

    private final transient UIEventBus eventBus;

    /**
     * Constructor
     * 
     * @param i18n
     *            VaadinMessageSource
     * @param managementUIState
     *            ManagementUIState
     * @param managementViewClientCriterion
     *            ManagementViewClientCriterion
     * @param permChecker
     *            SpPermissionChecker
     * @param eventBus
     *            UIEventBus
     * @param notification
     *            UINotification
     * @param entityFactory
     *            EntityFactory
     * @param targetFilterQueryManagement
     *            TargetFilterQueryManagement
     * @param targetTagManagement
     *            TargetTagManagement
     */
    public TargetTagFilterLayout(final VaadinMessageSource i18n, final ManagementUIState managementUIState,
            final ManagementViewClientCriterion managementViewClientCriterion, final SpPermissionChecker permChecker,
            final UIEventBus eventBus, final UINotification notification, final EntityFactory entityFactory,
            final TargetFilterQueryManagement targetFilterQueryManagement,
            final TargetTagManagement targetTagManagement) {
        super(new TargetTagFilterHeader(i18n, managementUIState, permChecker, eventBus),
                new MultipleTargetFilter(permChecker, managementUIState, i18n, eventBus, managementViewClientCriterion,
                        notification, entityFactory, targetFilterQueryManagement, targetTagManagement),
                managementUIState);
        this.eventBus = eventBus;
        eventBus.subscribe(this);
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onEvent(final ManagementUIEvent event) {
        if (event == ManagementUIEvent.HIDE_TARGET_TAG_LAYOUT) {
            setVisible(false);
        }
        if (event == ManagementUIEvent.SHOW_TARGET_TAG_LAYOUT) {
            setVisible(true);
        }
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onTargetTagTableEvent(final TargetTagTableEvent tableEvent) {
        refreshContainer();
        eventBus.publish(this, new TargetTagFilterHeaderEvent(FilterHeaderEnum.SHOW_MENUBAR));
    }

    @Override
    public Boolean onLoadIsTypeFilterIsClosed() {
        return managementUIState.isTargetTagFilterClosed();
    }

    @Override
    public void refreshContainer() {
        getMultipleFilterTabs().getFilterByButtons().refreshTable();
    }
}
