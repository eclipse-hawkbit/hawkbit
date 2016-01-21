/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.dstag;

import javax.annotation.PostConstruct;

import org.eclipse.hawkbit.repository.SpPermissionChecker;
import org.eclipse.hawkbit.ui.common.filterlayout.AbstractFilterHeader;
import org.eclipse.hawkbit.ui.management.event.ManagementUIEvent;
import org.eclipse.hawkbit.ui.management.state.ManagementUIState;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.SPUIComponetIdProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.events.EventBus;

import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;

/**
 *
 *
 */
@SpringComponent
@ViewScope
public class DistributionTagHeader extends AbstractFilterHeader {

    private static final long serialVersionUID = -1439667766337270066L;

    @Autowired
    private I18N i18n;

    @Autowired
    private SpPermissionChecker permChecker;

    @Autowired
    private transient EventBus.SessionEventBus eventbus;

    @Autowired
    private ManagementUIState managementUIState;

    @Autowired
    private CreateUpdateDistributionTagLayoutWindow createORUpdateDistributionTagLayout;

    /**
     * Initialize the components.
     */
    @PostConstruct
    public void init() {
        super.init();
        if (hasCreateUpdatePermission()) {
            createORUpdateDistributionTagLayout.initDistTagLayout();
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
        return "hide.distribution.tags";
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
        return i18n.get("header.filter.tag", new Object[] {});
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
        final Window addUpdateWindow = createORUpdateDistributionTagLayout.getWindow();
        UI.getCurrent().addWindow(addUpdateWindow);
        addUpdateWindow.setModal(true);
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
        return true;
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
        managementUIState.setDistTagFilterClosed(true);
        eventbus.publish(this, ManagementUIEvent.HIDE_DISTRIBUTION_TAG_LAYOUT);
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
        return SPUIComponetIdProvider.ADD_DISTRIBUTION_TAG;
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
