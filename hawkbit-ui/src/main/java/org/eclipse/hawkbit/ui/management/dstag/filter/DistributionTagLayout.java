/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.dstag.filter;

import org.eclipse.hawkbit.repository.DistributionSetTagManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.common.event.DistributionSetTagFilterHeaderEvent;
import org.eclipse.hawkbit.ui.common.event.FilterHeaderEvent.FilterHeaderEnum;
import org.eclipse.hawkbit.ui.common.filterlayout.AbstractFilterLayout;
import org.eclipse.hawkbit.ui.components.RefreshableContainer;
import org.eclipse.hawkbit.ui.management.event.DistributionSetTagTableEvent;
import org.eclipse.hawkbit.ui.management.event.ManagementUIEvent;
import org.eclipse.hawkbit.ui.management.state.ManagementUIState;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus.UIEventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

/**
 * Layout for Distribution Tags
 *
 */
public class DistributionTagLayout extends AbstractFilterLayout implements RefreshableContainer {

    private static final long serialVersionUID = 1L;

    private final ManagementUIState managementUIState;

    /**
     * Constructor
     * 
     * @param eventBus
     *            UIEventBus
     * @param managementUIState
     *            ManagementUIState
     * @param i18n
     *            VaadinMessageSource
     * @param permChecker
     *            SpPermissionChecker
     * @param distributionSetTagManagement
     *            DistributionSetTagManagement
     * @param entityFactory
     *            EntityFactory
     * @param uiNotification
     *            UINotification
     * @param distributionTagButtons
     *            DistributionTagButtons
     */
    public DistributionTagLayout(final UIEventBus eventBus, final ManagementUIState managementUIState,
            final VaadinMessageSource i18n, final SpPermissionChecker permChecker,
            final DistributionSetTagManagement distributionSetTagManagement, final EntityFactory entityFactory,
            final UINotification uiNotification, final DistributionTagButtons distributionTagButtons) {

        super(new DistributionTagFilterHeader(i18n, managementUIState, permChecker, eventBus, distributionSetTagManagement,
                entityFactory, uiNotification, distributionTagButtons), distributionTagButtons, eventBus);
        this.managementUIState = managementUIState;

        restoreState();
        eventBus.subscribe(this);
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onEvent(final ManagementUIEvent event) {
        if (event == ManagementUIEvent.HIDE_DISTRIBUTION_TAG_LAYOUT) {
            managementUIState.setDistTagFilterClosed(true);
            setVisible(false);
        }
        if (event == ManagementUIEvent.SHOW_DISTRIBUTION_TAG_LAYOUT) {
            managementUIState.setDistTagFilterClosed(false);
            setVisible(true);
        }
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onDistributionSetTagTableEvent(final DistributionSetTagTableEvent distributionSetTagTableEvent) {
        refreshContainer();
        getEventBus().publish(this, new DistributionSetTagFilterHeaderEvent(FilterHeaderEnum.SHOW_MENUBAR));
    }

    @Override
    public Boolean onLoadIsTypeFilterIsClosed() {
        return managementUIState.isDistTagFilterClosed();
    }

    @Override
    public void refreshContainer() {
        getFilterButtons().refreshTable();
    }

}
