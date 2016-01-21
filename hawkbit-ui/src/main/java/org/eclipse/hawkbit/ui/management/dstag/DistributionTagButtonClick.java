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
import org.eclipse.hawkbit.ui.management.event.DistributionTableFilterEvent;
import org.eclipse.hawkbit.ui.management.state.ManagementUIState;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.events.EventBus;

import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.Button;

/**
 *
 *
 */
@SpringComponent
@ViewScope
public class DistributionTagButtonClick extends AbstractFilterMultiButtonClick {

    /**
    * 
    */
    private static final long serialVersionUID = 4120296456125178019L;

    @Autowired
    private transient EventBus.SessionEventBus eventBus;

    @Autowired
    private ManagementUIState managementUIState;

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.hawkbit.server.ui.common.filterlayout.
     * AbstractFilterButtonClickBehaviour#filterUnClicked (com.vaadin.ui.Button)
     */
    @Override
    protected void filterUnClicked(final Button clickedButton) {
        if (clickedButton.getData().equals(SPUIDefinitions.NO_TAG_BUTTON_ID)) {
            managementUIState.getDistributionTableFilters().setNoTagSelected(false);
        } else {
            managementUIState.getDistributionTableFilters().getDistSetTags().remove(clickedButton.getId());
        }
        eventBus.publish(this, DistributionTableFilterEvent.FILTER_BY_TAG);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.hawkbit.server.ui.common.filterlayout.
     * AbstractFilterButtonClickBehaviour#filterClicked (com.vaadin.ui.Button)
     */
    @Override
    protected void filterClicked(final Button clickedButton) {
        if (clickedButton.getData().equals(SPUIDefinitions.NO_TAG_BUTTON_ID)) {
            managementUIState.getDistributionTableFilters().setNoTagSelected(true);
        } else {
            managementUIState.getDistributionTableFilters().getDistSetTags().add(clickedButton.getId());
        }

        eventBus.publish(this, DistributionTableFilterEvent.FILTER_BY_TAG);
    }

}
