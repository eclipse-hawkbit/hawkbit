/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.distributions.disttype;

import javax.annotation.PostConstruct;

import org.eclipse.hawkbit.ui.common.CommonDialogWindow;
import org.eclipse.hawkbit.ui.common.filterlayout.AbstractFilterHeader;
import org.eclipse.hawkbit.ui.distributions.event.DistributionsUIEvent;
import org.eclipse.hawkbit.ui.distributions.state.ManageDistUIState;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.UI;

/**
 * Distribution Set Type filter buttons header.
 */
@SpringComponent
@ViewScope
public class DSTypeFilterHeader extends AbstractFilterHeader {

    private static final long serialVersionUID = 3433417459392880222L;

    @Autowired
    private ManageDistUIState manageDistUIState;

    @Autowired
    private CreateUpdateDistSetTypeLayout createUpdateDistSetTypeLayout;

    private CommonDialogWindow addUpdateWindow;

    @Override
    @PostConstruct
    public void init() {
        super.init();
        if (hasCreateUpdatePermission()) {
            createUpdateDistSetTypeLayout.init();
        }
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
        addUpdateWindow = createUpdateDistSetTypeLayout.getWindow();
        UI.getCurrent().addWindow(addUpdateWindow);
    }

    @Override
    protected boolean dropHitsRequired() {
        return false;
    }

    @Override
    protected void hideFilterButtonLayout() {
        manageDistUIState.setDistTypeFilterClosed(true);
        eventBus.publish(this, DistributionsUIEvent.HIDE_DIST_FILTER_BY_TYPE);
    }

    @Override
    protected String getConfigureFilterButtonId() {

        return SPUIDefinitions.ADD_DISTRIBUTION_TYPE_TAG;
    }

    @Override
    protected String getHideButtonId() {
        return SPUIDefinitions.HIDE_FILTER_DIST_TYPE;
    }

    @Override
    protected boolean isAddTagRequired() {
        return true;
    }

}
