/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.dstag;

import org.eclipse.hawkbit.ui.common.filterlayout.AbstractFilterMultiButtonClick;
import org.eclipse.hawkbit.ui.management.event.RefreshDistributionTableByFilterEvent;
import org.eclipse.hawkbit.ui.management.state.ManagementUIState;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.EventBus.UIEventBus;

import com.vaadin.ui.Button;

/**
 * Abstract class for button click behavior of the distribution set's tag
 * buttons. Filters the distribution sets according to the active tags.
 */
public class DistributionTagButtonClick extends AbstractFilterMultiButtonClick {

    private static final long serialVersionUID = 1L;

    private final transient EventBus.UIEventBus eventBus;

    private final ManagementUIState managementUIState;

    DistributionTagButtonClick(final UIEventBus eventBus, final ManagementUIState managementUIState) {
        this.eventBus = eventBus;
        this.managementUIState = managementUIState;
    }

    @Override
    protected void filterUnClicked(final Button clickedButton) {
        if (clickedButton.getData().equals(SPUIDefinitions.NO_TAG_BUTTON_ID)) {
            managementUIState.getDistributionTableFilters().setNoTagSelected(false);
        } else {
            managementUIState.getDistributionTableFilters().getDistSetTags().remove(clickedButton.getId());
        }
        eventBus.publish(this, new RefreshDistributionTableByFilterEvent());
    }

    @Override
    protected void filterClicked(final Button clickedButton) {
        if (clickedButton.getData().equals(SPUIDefinitions.NO_TAG_BUTTON_ID)) {
            managementUIState.getDistributionTableFilters().setNoTagSelected(true);
        } else {
            managementUIState.getDistributionTableFilters().getDistSetTags().add(clickedButton.getId());
        }
        eventBus.publish(this, new RefreshDistributionTableByFilterEvent());
    }

}
