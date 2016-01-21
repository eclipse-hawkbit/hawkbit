/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.targettag;

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

/**
 * Target Tag filter by Tag Header.
 * 
 *
 *
 */
@SpringComponent
@ViewScope
public class TargetTagFilterHeader extends AbstractFilterHeader {

    private static final long serialVersionUID = 3046367045669148009L;

    @Autowired
    private SpPermissionChecker permChecker;

    @Autowired
    private I18N i18n;

    @Autowired
    private transient EventBus.SessionEventBus eventbus;

    @Autowired
    private CreateUpdateTargetTagLayout createUpdateTargetTagLayout;

    @Autowired
    private ManagementUIState managementUIState;

    /**
     * Initialize Tag Header.
     */
    @PostConstruct
    public void init() {
        super.init();
        if (permChecker.hasCreateTargetPermission() || permChecker.hasUpdateTargetPermission()) {
            createUpdateTargetTagLayout.init();
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

        return SPUIComponetIdProvider.HIDE_TARGET_TAGS;
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

        return permChecker.hasCreateTargetPermission() || permChecker.hasUpdateTargetPermission();
    }

    /*
     * (non-Javadoc)
     * 
     * @see hawkbit.server.ui.common.filterlayout.AbstractFilterHeader#getTitle(
     * )
     */
    @Override
    protected String getTitle() {
        return i18n.get("header.target.filter.tag", new Object[] {});
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
        /**
         * Add tag icon not displayed.
         */
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
        managementUIState.setTargetTagFilterClosed(true);
        eventbus.publish(this, ManagementUIEvent.HIDE_TARGET_TAG_LAYOUT);
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
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.hawkbit.ui.common.filterlayout.AbstractFilterHeader#
     * isAddTagRequired()
     */
    @Override
    protected boolean isAddTagRequired() {
        return false;
    }

}
