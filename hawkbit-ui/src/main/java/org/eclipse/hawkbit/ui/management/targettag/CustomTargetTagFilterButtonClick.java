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

import org.eclipse.hawkbit.repository.TargetFilterQueryManagement;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.eclipse.hawkbit.ui.common.filterlayout.AbstractFilterSingleButtonClick;
import org.eclipse.hawkbit.ui.management.event.TargetFilterEvent;
import org.eclipse.hawkbit.ui.management.state.ManagementUIState;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.events.EventBus;

import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;

/**
 * Single button click behaviour of custom target filter buttons layout.
 *
 *
 *
 */
@SpringComponent
@ViewScope
public class CustomTargetTagFilterButtonClick extends AbstractFilterSingleButtonClick implements Serializable {

    private static final long serialVersionUID = -6173433602055291533L;

    @Autowired
    private transient EventBus.SessionEventBus eventBus;

    @Autowired
    private transient ManagementUIState managementUIState;

    @Autowired
    private transient TargetFilterQueryManagement targetFilterQueryManagement;

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.hawkbit.server.ui.common.filterlayout.
     * AbstractFilterButtonClickBehaviour#filterUnClicked (com.vaadin.ui.Button)
     */
    @Override
    protected void filterUnClicked(final Button clickedButton) {
        this.managementUIState.getTargetTableFilters().setTargetFilterQuery(null);
        this.eventBus.publish(this, TargetFilterEvent.REMOVE_FILTER_BY_TARGET_FILTER_QUERY);
    }

    /*
     * (non-Javadoc)
     *
     * @see hawkbit.server.ui.layouts.SPFilterButtonClickBehaviour#filterClicked
     * (com.vaadin.ui.Button )
     */
    @Override
    protected void filterClicked(final Button clickedButton) {
        final TargetFilterQuery targetFilterQuery = this.targetFilterQueryManagement
                .findTargetFilterQueryById((Long) clickedButton.getData());
        this.managementUIState.getTargetTableFilters().setTargetFilterQuery(targetFilterQuery);
        this.eventBus.publish(this, TargetFilterEvent.FILTER_BY_TARGET_FILTER_QUERY);
    }

    protected void processButtonClick(final ClickEvent event) {
        processFilterButtonClick(event);
    }

    protected void clearAppliedTargetFilterQuery() {
        if (getAlreadyClickedButton() != null) {
            getAlreadyClickedButton().removeStyleName(SPUIStyleDefinitions.SP_FILTER_BTN_CLICKED_STYLE);
            setAlreadyClickedButton(null);
        }
        this.managementUIState.getTargetTableFilters().setTargetFilterQuery(null);
        this.eventBus.publish(this, TargetFilterEvent.REMOVE_FILTER_BY_TARGET_FILTER_QUERY);
    }

    protected void setDefaultButtonClicked(final Button button) {
        super.setDefaultClickedButton(button);
    }
}
