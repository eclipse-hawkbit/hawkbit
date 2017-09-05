/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.distributions.smtype;

import org.eclipse.hawkbit.repository.SoftwareModuleTypeManagement;
import org.eclipse.hawkbit.ui.artifacts.event.RefreshSoftwareModuleByFilterEvent;
import org.eclipse.hawkbit.ui.common.filterlayout.AbstractFilterSingleButtonClick;
import org.eclipse.hawkbit.ui.distributions.state.ManageDistUIState;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.EventBus.UIEventBus;

import com.vaadin.ui.Button;

/**
 * Single button click behaviour of filter buttons layout.
 */
public class DistSMTypeFilterButtonClick extends AbstractFilterSingleButtonClick {

    private static final long serialVersionUID = -4166632002904286983L;

    private final transient EventBus.UIEventBus eventBus;

    private final ManageDistUIState manageDistUIState;

    private final transient SoftwareModuleTypeManagement softwareModuleTypeManagement;

    DistSMTypeFilterButtonClick(final UIEventBus eventBus, final ManageDistUIState manageDistUIState,
            final SoftwareModuleTypeManagement softwareModuleTypeManagement) {
        this.eventBus = eventBus;
        this.manageDistUIState = manageDistUIState;
        this.softwareModuleTypeManagement = softwareModuleTypeManagement;
    }

    @Override
    protected void filterUnClicked(final Button clickedButton) {
        manageDistUIState.getSoftwareModuleFilters().setSoftwareModuleType(null);
        eventBus.publish(this, new RefreshSoftwareModuleByFilterEvent());
    }

    @Override
    protected void filterClicked(final Button clickedButton) {
        softwareModuleTypeManagement.getByName(clickedButton.getData().toString())
                .ifPresent(smType -> {
                    manageDistUIState.getSoftwareModuleFilters().setSoftwareModuleType(smType);
                    eventBus.publish(this, new RefreshSoftwareModuleByFilterEvent());
                });
    }

}
