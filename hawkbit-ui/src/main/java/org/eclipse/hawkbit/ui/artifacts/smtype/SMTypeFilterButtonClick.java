/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts.smtype;

import java.io.Serializable;

import org.eclipse.hawkbit.repository.SoftwareManagement;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.ui.artifacts.event.SMFilterEvent;
import org.eclipse.hawkbit.ui.artifacts.state.ArtifactUploadState;
import org.eclipse.hawkbit.ui.common.filterlayout.AbstractFilterSingleButtonClick;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.events.EventBus;

import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.UIScope;
import com.vaadin.ui.Button;

/**
 * Single button click behaviour of filter buttons layout.
 * 
 *
 * 
 */
@SpringComponent
@UIScope
public class SMTypeFilterButtonClick extends AbstractFilterSingleButtonClick implements Serializable {

    private static final long serialVersionUID = 3707945900524967887L;

    @Autowired
    private transient EventBus.SessionEventBus eventBus;

    @Autowired
    private ArtifactUploadState artifactUploadState;

    @Autowired
    private transient SoftwareManagement softwareManagement;

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.hawkbit.server.ui.layouts.SPFilterButtonClickBehaviour#
     * filterUnClicked(com.vaadin.ui. Button)
     */
    @Override
    protected void filterUnClicked(final Button clickedButton) {
        artifactUploadState.getSoftwareModuleFilters().setSoftwareModuleType(null);
        eventBus.publish(this, SMFilterEvent.FILTER_BY_TYPE);
    }

    /*
     * (non-Javadoc)
     * 
     * @see hawkbit.server.ui.layouts.SPFilterButtonClickBehaviour#filterClicked
     * (com.vaadin.ui.Button )
     */
    @Override
    protected void filterClicked(final Button clickedButton) {
        final SoftwareModuleType softwareModuleType = softwareManagement
                .findSoftwareModuleTypeByName(clickedButton.getData().toString());
        artifactUploadState.getSoftwareModuleFilters().setSoftwareModuleType(softwareModuleType);
        eventBus.publish(this, SMFilterEvent.FILTER_BY_TYPE);
    }

}
