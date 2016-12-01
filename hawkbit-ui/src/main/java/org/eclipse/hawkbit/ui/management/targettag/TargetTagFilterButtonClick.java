/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.targettag;

import java.io.Serializable;

import org.eclipse.hawkbit.ui.common.filterlayout.AbstractFilterMultiButtonClick;
import org.eclipse.hawkbit.ui.management.event.TargetFilterEvent;
import org.eclipse.hawkbit.ui.management.state.ManagementUIState;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.EventBus.UIEventBus;

import com.vaadin.ui.Button;

/**
 * Multi button click behaviour of filter buttons layout.
 */
public class TargetTagFilterButtonClick extends AbstractFilterMultiButtonClick implements Serializable {

    private static final long serialVersionUID = -6173433602055291533L;

    private final transient EventBus.UIEventBus eventBus;

    private final ManagementUIState managementUIState;

    TargetTagFilterButtonClick(final UIEventBus eventBus, final ManagementUIState managementUIState) {
        this.eventBus = eventBus;
        this.managementUIState = managementUIState;
    }

    @Override
    protected void filterUnClicked(final Button clickedButton) {
        if (clickedButton.getData().equals(SPUIDefinitions.NO_TAG_BUTTON_ID)) {
            if (managementUIState.getTargetTableFilters().isNoTagSelected()) {
                managementUIState.getTargetTableFilters().setNoTagSelected(false);
                eventBus.publish(this, TargetFilterEvent.FILTER_BY_TAG);
            }
        } else {
            if (null != managementUIState.getTargetTableFilters().getClickedTargetTags() && managementUIState
                    .getTargetTableFilters().getClickedTargetTags().contains(clickedButton.getId())) {
                managementUIState.getTargetTableFilters().getClickedTargetTags().remove(clickedButton.getId());
                eventBus.publish(this, TargetFilterEvent.FILTER_BY_TAG);
            }

        }
    }

    @Override
    protected void filterClicked(final Button clickedButton) {
        if (clickedButton.getData().equals(SPUIDefinitions.NO_TAG_BUTTON_ID)) {
            managementUIState.getTargetTableFilters().setNoTagSelected(true);
            eventBus.publish(this, TargetFilterEvent.FILTER_BY_TAG);
        } else {
            managementUIState.getTargetTableFilters().getClickedTargetTags().add(clickedButton.getId());
            eventBus.publish(this, TargetFilterEvent.FILTER_BY_TAG);
        }
    }

    protected void clearTargetTagFilters() {
        for (final Button button : alreadyClickedButtons) {
            button.removeStyleName(SPUIStyleDefinitions.SP_FILTER_BTN_CLICKED_STYLE);
        }
        alreadyClickedButtons.clear();
        managementUIState.getTargetTableFilters().getClickedTargetTags().clear();
        eventBus.publish(this, TargetFilterEvent.FILTER_BY_TAG);
    }
}
