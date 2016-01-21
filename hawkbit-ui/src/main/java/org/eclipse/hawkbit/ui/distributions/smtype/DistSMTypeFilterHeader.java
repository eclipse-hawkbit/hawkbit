/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.distributions.smtype;

import javax.annotation.PostConstruct;

import org.eclipse.hawkbit.repository.SpPermissionChecker;
import org.eclipse.hawkbit.ui.artifacts.smtype.CreateUpdateSoftwareTypeLayout;
import org.eclipse.hawkbit.ui.common.filterlayout.AbstractFilterHeader;
import org.eclipse.hawkbit.ui.distributions.event.DistributionsUIEvent;
import org.eclipse.hawkbit.ui.distributions.state.ManageDistUIState;
import org.eclipse.hawkbit.ui.utils.SPUIComponetIdProvider;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.events.EventBus;

import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;

/**
 * Software Module Type filter buttons header.
 * 
 *
 * 
 */
@SpringComponent
@ViewScope
public class DistSMTypeFilterHeader extends AbstractFilterHeader {

    private static final long serialVersionUID = -8763788280848718344L;

    @Autowired
    private SpPermissionChecker permChecker;

    @Autowired
    private transient EventBus.SessionEventBus eventBus;

    @Autowired
    private ManageDistUIState manageDistUIState;

    @Autowired
    private CreateUpdateSoftwareTypeLayout createUpdateSWTypeLayout;

    /**
     * Initialize the components.
     */
    @PostConstruct
    public void init() {
        super.init();
        if (hasCreateUpdatePermission()) {
            createUpdateSWTypeLayout.init();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.hawkbit.server.ui.common.filterlayout.AbstractFilterHeader#
     * getHideButtonId()
     */
    @Override
    protected String getHideButtonId() {
        return SPUIComponetIdProvider.SM_SHOW_FILTER_BUTTON_ID;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.hawkbit.server.ui.common.filterlayout.AbstractFilterHeader#
     * hasCreateUpdatePermission()
     */
    @Override
    protected boolean hasCreateUpdatePermission() {

        return permChecker.hasCreateDistributionPermission() || permChecker.hasUpdateDistributionPermission();
    }

    /*
     * (non-Javadoc)
     * 
     * @see hawkbit.server.ui.common.filterlayout.AbstractFilterHeader#getTitle(
     * )
     */
    @Override
    protected String getTitle() {

        return SPUILabelDefinitions.TYPE;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.hawkbit.server.ui.common.filterlayout.AbstractFilterHeader#
     * settingsIconClicked(com.vaadin .ui.Button.ClickEvent)
     */
    @Override
    protected void settingsIconClicked(final ClickEvent event) {
        final Window addUpdateWindow = createUpdateSWTypeLayout.getWindow();
        UI.getCurrent().addWindow(addUpdateWindow);
        addUpdateWindow.setVisible(Boolean.TRUE);

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.hawkbit.server.ui.common.filterlayout.AbstractFilterHeader#
     * dropHitsRequired()
     */
    @Override
    protected boolean dropHitsRequired() {

        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.hawkbit.server.ui.common.filterlayout.AbstractFilterHeader#
     * hideFilterButtonLayout()
     */
    @Override
    protected void hideFilterButtonLayout() {
        manageDistUIState.setSwTypeFilterClosed(true);
        eventBus.publish(this, DistributionsUIEvent.HIDE_SM_FILTER_BY_TYPE);

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.hawkbit.server.ui.common.filterlayout.AbstractFilterHeader#
     * getConfigureFilterButtonId()
     */
    @Override
    protected String getConfigureFilterButtonId() {
        return SPUIDefinitions.ADD_SOFTWARE_MODULE_TYPE;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.hawkbit.ui.common.filterlayout.AbstractFilterHeader#
     * isAddTagRequired()
     */
    @Override
    protected boolean isAddTagRequired() {
        return true;
    }

}
