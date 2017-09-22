/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.distributions.smtype;

import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.SoftwareModuleTypeManagement;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.common.filterlayout.AbstractFilterLayout;
import org.eclipse.hawkbit.ui.dd.criteria.DistributionsViewClientCriterion;
import org.eclipse.hawkbit.ui.distributions.event.DistributionsUIEvent;
import org.eclipse.hawkbit.ui.distributions.state.ManageDistUIState;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus.UIEventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

/**
 * Software Module Type filter layout.
 */
public class DistSMTypeFilterLayout extends AbstractFilterLayout {

    private static final long serialVersionUID = 3042297420534417538L;

    private final ManageDistUIState manageDistUIState;

    public DistSMTypeFilterLayout(final UIEventBus eventBus, final VaadinMessageSource i18n,
            final SpPermissionChecker permChecker, final ManageDistUIState manageDistUIState,
            final EntityFactory entityFactory, final UINotification uiNotification,
            final SoftwareModuleTypeManagement softwareModuleTypeManagement,
            final DistributionsViewClientCriterion distributionsViewClientCriterion) {
        super(new DistSMTypeFilterHeader(i18n, permChecker, eventBus, manageDistUIState, entityFactory, uiNotification,
                softwareModuleTypeManagement),
                new DistSMTypeFilterButtons(eventBus, manageDistUIState, distributionsViewClientCriterion,
                        softwareModuleTypeManagement));
        this.manageDistUIState = manageDistUIState;

        restoreState();
        eventBus.subscribe(this);
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onEvent(final DistributionsUIEvent event) {
        if (event == DistributionsUIEvent.HIDE_SM_FILTER_BY_TYPE) {
            setVisible(false);
        }
        if (event == DistributionsUIEvent.SHOW_SM_FILTER_BY_TYPE) {
            setVisible(true);
        }
    }

    @Override
    public Boolean onLoadIsTypeFilterIsClosed() {

        return manageDistUIState.isSwTypeFilterClosed();
    }

}
