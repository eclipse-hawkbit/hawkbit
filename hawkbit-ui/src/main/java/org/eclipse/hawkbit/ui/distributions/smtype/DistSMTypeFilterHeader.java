/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.distributions.smtype;

import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.SoftwareModuleTypeManagement;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.artifacts.smtype.CreateSoftwareTypeLayout;
import org.eclipse.hawkbit.ui.artifacts.smtype.DeleteSoftwareTypeLayout;
import org.eclipse.hawkbit.ui.artifacts.smtype.UpdateSoftwareTypeLayout;
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
 * Software Module Type filter buttons header.
 */
public class DistSMTypeFilterHeader extends AbstractFilterHeader {

    private static final long serialVersionUID = 1L;

    private final ManageDistUIState manageDistUIState;

    EntityFactory entityFactory;

    UINotification uiNotification;

    SoftwareModuleTypeManagement softwareModuleTypeManagement;

    DistSMTypeFilterHeader(final VaadinMessageSource i18n, final SpPermissionChecker permChecker,
            final UIEventBus eventBus, final ManageDistUIState manageDistUIState, final EntityFactory entityFactory,
            final UINotification uiNotification, final SoftwareModuleTypeManagement softwareModuleTypeManagement) {
        super(permChecker, eventBus, i18n);
        this.manageDistUIState = manageDistUIState;
        this.entityFactory = entityFactory;
        this.uiNotification = uiNotification;
        this.softwareModuleTypeManagement = softwareModuleTypeManagement;

        // if (hasCreateUpdatePermission()) {
        // createUpdateSWTypeLayout.init();
        // }
    }

    @Override
    protected String getHideButtonId() {
        return UIComponentIdProvider.SM_SHOW_FILTER_BUTTON_ID;
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
        manageDistUIState.setSwTypeFilterClosed(true);
        eventBus.publish(this, DistributionsUIEvent.HIDE_SM_FILTER_BY_TYPE);
    }

    @Override
    protected String getConfigureFilterButtonId() {
        return UIComponentIdProvider.ADD_SOFTWARE_MODULE_TYPE;
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
                new CreateSoftwareTypeLayout(i18n, entityFactory, eventBus, permChecker, uiNotification,
                        softwareModuleTypeManagement);
            }
        };
    }

    @Override
    protected Command getDeleteButtonCommand() {
        return new MenuBar.Command() {

            private static final long serialVersionUID = 1L;

            @Override
            public void menuSelected(final MenuItem selectedItem) {
                new DeleteSoftwareTypeLayout(i18n, entityFactory, eventBus, permChecker, uiNotification,
                        softwareModuleTypeManagement, manageDistUIState.getSelectedDeleteSWModuleTypes());
            }
        };
    }

    @Override
    protected Command getUpdateButtonCommand() {
        return new MenuBar.Command() {

            private static final long serialVersionUID = 1L;

            @Override
            public void menuSelected(final MenuItem selectedItem) {
                new UpdateSoftwareTypeLayout(i18n, entityFactory, eventBus, permChecker, uiNotification,
                        softwareModuleTypeManagement);
            }
        };
    }

}
