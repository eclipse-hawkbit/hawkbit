/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.distributions.disttype;

import java.io.Serializable;

import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.ui.common.filterlayout.AbstractFilterSingleButtonClick;
import org.eclipse.hawkbit.ui.distributions.state.ManageDistUIState;
import org.eclipse.hawkbit.ui.management.event.DistributionTableFilterEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.events.EventBus;

import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.Button;

/**
 * Single button click behaviour of filter buttons layout.
 * 
 *
 * 
 */

@SpringComponent
@ViewScope
public class DSTypeFilterButtonClick extends AbstractFilterSingleButtonClick implements Serializable {

    private static final long serialVersionUID = -584783755917528648L;

    @Autowired
    private transient EventBus.SessionEventBus eventBus;

    @Autowired
    private ManageDistUIState manageDistUIState;

    @Autowired
    private transient DistributionSetManagement distributionSetManagement;

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.hawkbit.server.ui.common.filterlayout.
     * AbstractFilterButtonClickBehaviour#filterUnClicked (com.vaadin.ui.Button)
     */
    @Override
    protected void filterUnClicked(final Button clickedButton) {
        manageDistUIState.getManageDistFilters().setClickedDistSetType(null);
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
        final DistributionSetType distSetType = distributionSetManagement
                .findDistributionSetTypeByName(clickedButton.getData().toString());
        manageDistUIState.getManageDistFilters().setClickedDistSetType(distSetType);
        eventBus.publish(this, DistributionTableFilterEvent.FILTER_BY_TAG);

    }

}
