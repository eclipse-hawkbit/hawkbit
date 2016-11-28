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
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.SpPermissionChecker;
import org.eclipse.hawkbit.repository.TagManagement;
import org.eclipse.hawkbit.ui.common.filterlayout.AbstractFilterLayout;
import org.eclipse.hawkbit.ui.management.event.ManagementUIEvent;
import org.eclipse.hawkbit.ui.management.event.ManagementViewAcceptCriteria;
import org.eclipse.hawkbit.ui.management.state.DistributionTableFilters;
import org.eclipse.hawkbit.ui.management.state.ManagementUIState;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.vaadin.spring.events.EventBus.UIEventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

/**
 *
 *
 */
public class DistributionTagLayout extends AbstractFilterLayout {

    private static final long serialVersionUID = 4363033587261057567L;

    private final ManagementUIState managementUIState;

    public DistributionTagLayout(final UIEventBus eventbus, final ManagementUIState managementUIState, final I18N i18n,
            final SpPermissionChecker permChecker, final UIEventBus eventBus, final TagManagement tagManagement,
            final EntityFactory entityFactory, final UINotification uiNotification,
            final DistributionTableFilters distFilterParameters,
            final DistributionSetManagement distributionSetManagement,
            final ManagementViewAcceptCriteria managementViewAcceptCriteria) {

        super(new DistributionTagHeader(i18n, managementUIState, permChecker, eventBus, tagManagement, entityFactory,
                uiNotification),
                new DistributionTagButtons(eventBus, managementUIState, entityFactory, i18n, uiNotification,
                        permChecker, distFilterParameters, distributionSetManagement, managementViewAcceptCriteria));
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

    @Override
    public Boolean onLoadIsTypeFilterIsClosed() {
        return managementUIState.isDistTagFilterClosed();
    }

}
