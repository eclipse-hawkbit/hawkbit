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

import org.eclipse.hawkbit.ui.artifacts.smtype.CreateUpdateSoftwareTypeLayout;
import org.eclipse.hawkbit.ui.common.filterlayout.AbstractFilterHeader;
import org.eclipse.hawkbit.ui.distributions.event.DistributionsUIEvent;
import org.eclipse.hawkbit.ui.distributions.state.ManageDistUIState;
import org.eclipse.hawkbit.ui.utils.SPUIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;

/**
 * Software Module Type filter buttons header.
 */
@SpringComponent
@ViewScope
public class DistSMTypeFilterHeader extends AbstractFilterHeader {

    private static final long serialVersionUID = -8763788280848718344L;

    @Autowired
    private ManageDistUIState manageDistUIState;

    @Autowired
    private CreateUpdateSoftwareTypeLayout createUpdateSWTypeLayout;

    @Override
    @PostConstruct
    public void init() {
        super.init();
        if (hasCreateUpdatePermission()) {
            createUpdateSWTypeLayout.init();
        }
    }

    @Override
    protected String getHideButtonId() {
        return SPUIComponentIdProvider.SM_SHOW_FILTER_BUTTON_ID;
    }

    @Override
    protected boolean hasCreateUpdatePermission() {
        return permChecker.hasCreateDistributionPermission() || permChecker.hasUpdateDistributionPermission();
    }

    @Override
    protected String getTitle() {
        return SPUILabelDefinitions.TYPE;
    }

    @Override
    protected void settingsIconClicked(final ClickEvent event) {
        final Window addUpdateWindow = createUpdateSWTypeLayout.getWindow();
        UI.getCurrent().addWindow(addUpdateWindow);
        addUpdateWindow.setVisible(Boolean.TRUE);

    }

    @Override
    protected boolean dropHitsRequired() {
        return false;
    }

    @Override
    protected void hideFilterButtonLayout() {
        manageDistUIState.setSwTypeFilterClosed(true);
        eventBus.publish(this, DistributionsUIEvent.HIDE_SM_FILTER_BY_TYPE);
    }

    @Override
    protected String getConfigureFilterButtonId() {
        return SPUIDefinitions.ADD_SOFTWARE_MODULE_TYPE;
    }

    @Override
    protected boolean isAddTagRequired() {
        return true;
    }

}
