/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.dstag;

import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.DistributionSetTagManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.common.filterlayout.AbstractFilterButtons;
import org.eclipse.hawkbit.ui.common.filterlayout.AbstractFilterHeader;
import org.eclipse.hawkbit.ui.common.filterlayout.AbstractFilterLayout;
import org.eclipse.hawkbit.ui.common.table.BaseEntityEventType;
import org.eclipse.hawkbit.ui.components.RefreshableContainer;
import org.eclipse.hawkbit.ui.dd.criteria.ManagementViewClientCriterion;
import org.eclipse.hawkbit.ui.management.event.DistributionSetTagTableEvent;
import org.eclipse.hawkbit.ui.management.event.ManagementUIEvent;
import org.eclipse.hawkbit.ui.management.state.DistributionTableFilters;
import org.eclipse.hawkbit.ui.management.state.ManagementUIState;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus.UIEventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

/**
 *
 *
 */
public class DistributionTagLayout extends AbstractFilterLayout implements RefreshableContainer {

    private static final long serialVersionUID = 4363033587261057567L;

    private final ManagementUIState managementUIState;

    public DistributionTagLayout(final UIEventBus eventbus, final ManagementUIState managementUIState,
            final VaadinMessageSource i18n, final SpPermissionChecker permChecker, final UIEventBus eventBus,
            final DistributionSetTagManagement distributionSetTagManagement, final EntityFactory entityFactory,
            final UINotification uiNotification, final DistributionTableFilters distFilterParameters,
            final DistributionSetManagement distributionSetManagement,
            final ManagementViewClientCriterion managementViewClientCriterion) {

        super(new DistributionTagHeader(i18n, managementUIState, permChecker, eventBus, distributionSetTagManagement,
                entityFactory, uiNotification),
                new DistributionTagButtons(eventBus, managementUIState, entityFactory, i18n, uiNotification,
                        permChecker, distFilterParameters, distributionSetManagement, managementViewClientCriterion));
        this.managementUIState = managementUIState;

        restoreState();
        eventbus.subscribe(this);
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
        if (BaseEntityEventType.ADD_ENTITY != distributionSetTagTableEvent.getEventType()
                && BaseEntityEventType.REMOVE_ENTITY != distributionSetTagTableEvent.getEventType()) {
            return;
        }
        refreshContainer();
    }

    @Override
    public Boolean onLoadIsTypeFilterIsClosed() {
        return managementUIState.isDistTagFilterClosed();
    }

    @Override
    public void refreshContainer() {
        final AbstractFilterButtons filterButtons = getFilterButtons();
        if (filterButtons instanceof RefreshableContainer) {
            ((RefreshableContainer) filterButtons).refreshContainer();
        }

        final AbstractFilterHeader filterHeader = getFilterHeader();
        if (filterHeader instanceof RefreshableContainer) {
            ((RefreshableContainer) filterHeader).refreshContainer();
        }

    }

}
