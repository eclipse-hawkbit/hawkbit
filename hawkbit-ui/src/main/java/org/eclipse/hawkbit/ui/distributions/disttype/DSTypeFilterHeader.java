/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.distributions.disttype;

import java.util.Arrays;

import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.DistributionSetTypeManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.SoftwareModuleTypeManagement;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.common.filterlayout.AbstractFilterHeader;
import org.eclipse.hawkbit.ui.distributions.event.DistributionsUIEvent;
import org.eclipse.hawkbit.ui.distributions.state.ManageDistUIState;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus.UIEventBus;

import com.vaadin.ui.MenuBar;
import com.vaadin.ui.MenuBar.Command;
import com.vaadin.ui.MenuBar.MenuItem;

/**
 * Distribution Set Type filter buttons header.
 */
public class DSTypeFilterHeader extends AbstractFilterHeader {

    private static final long serialVersionUID = 1L;

    private final ManageDistUIState manageDistUIState;

    VaadinMessageSource i18n;

    SpPermissionChecker permChecker;

    UIEventBus eventBus;

    EntityFactory entityFactory;

    UINotification uiNotification;

    SoftwareModuleTypeManagement softwareModuleTypeManagement;

    DistributionSetTypeManagement distributionSetTypeManagement;

    DistributionSetManagement distributionSetManagement;

    DSTypeFilterHeader(final VaadinMessageSource i18n, final SpPermissionChecker permChecker, final UIEventBus eventBus,
            final ManageDistUIState manageDistUIState, final EntityFactory entityFactory,
            final UINotification uiNotification, final SoftwareModuleTypeManagement softwareModuleTypeManagement,
            final DistributionSetTypeManagement distributionSetTypeManagement,
            final DistributionSetManagement distributionSetManagement) {
        super(permChecker, eventBus, i18n);
        this.manageDistUIState = manageDistUIState;
        this.i18n = i18n;
        this.permChecker = permChecker;
        this.eventBus = eventBus;
        this.entityFactory = entityFactory;
        this.softwareModuleTypeManagement = softwareModuleTypeManagement;
        this.distributionSetTypeManagement = distributionSetTypeManagement;
        this.uiNotification = uiNotification;
        this.distributionSetManagement = distributionSetManagement;
    }

    @Override
    protected String getTitle() {
        return SPUILabelDefinitions.TYPE;
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

        return UIComponentIdProvider.ADD_DISTRIBUTION_TYPE_TAG;
    }

    @Override
    protected String getHideButtonId() {
        return UIComponentIdProvider.HIDE_FILTER_DIST_TYPE;
    }

    @Override
    protected boolean isAddTagRequired() {
        return true;
    }

    @Override
    protected Command getAddButtonCommand() {
        return new MenuBar.Command() {

            private static final long serialVersionUID = 1L;

            @Override
            public void menuSelected(final MenuItem selectedItem) {
                new CreateDistributionSetTypeLayout(i18n, entityFactory, eventBus, permChecker, uiNotification,
                        softwareModuleTypeManagement, distributionSetTypeManagement);
            }
        };
    }

    @Override
    protected Command getDeleteButtonCommand() {
        return new MenuBar.Command() {

            private static final long serialVersionUID = 1L;

            @Override
            public void menuSelected(final MenuItem selectedItem) {
                new DeleteDistributionSetTypeLayout(i18n, entityFactory, eventBus, permChecker, uiNotification,
                        softwareModuleTypeManagement, distributionSetTypeManagement, distributionSetManagement,
                        Arrays.asList(manageDistUIState.getManageDistFilters().getClickedDistSetType()));
            }
        };
    }

    @Override
    protected Command getUpdateButtonCommand() {
        return new MenuBar.Command() {

            private static final long serialVersionUID = 1L;

            @Override
            public void menuSelected(final MenuItem selectedItem) {
                new UpdateDistributionSetTypeLayout(i18n, entityFactory, eventBus, permChecker, uiNotification,
                        softwareModuleTypeManagement, distributionSetTypeManagement, distributionSetManagement);
            }
        };
    }

}
