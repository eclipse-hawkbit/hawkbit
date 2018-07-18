/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.distributions.disttype.filter;

import org.eclipse.hawkbit.repository.DistributionSetTypeManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.SoftwareModuleTypeManagement;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.common.event.DistributionSetTypeFilterHeaderEvent;
import org.eclipse.hawkbit.ui.common.event.FilterHeaderEvent.FilterHeaderEnum;
import org.eclipse.hawkbit.ui.common.filterlayout.AbstractFilterHeader;
import org.eclipse.hawkbit.ui.distributions.disttype.CreateDistributionSetTypeLayout;
import org.eclipse.hawkbit.ui.distributions.event.DistributionsUIEvent;
import org.eclipse.hawkbit.ui.distributions.state.ManageDistUIState;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UIMessageIdProvider;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus.UIEventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.MenuBar.Command;

/**
 * Distribution Set Type filter buttons header.
 */
public class DSTypeFilterHeader extends AbstractFilterHeader {

    private static final long serialVersionUID = 1L;

    private final ManageDistUIState manageDistUIState;

    private final transient EntityFactory entityFactory;

    private final transient UINotification uiNotification;

    private final transient SoftwareModuleTypeManagement softwareModuleTypeManagement;

    private final transient DistributionSetTypeManagement distributionSetTypeManagement;

    private final DSTypeFilterButtons dSTypeFilterButtons;

    /**
     * Constructor
     * 
     * @param i18n
     *            VaadinMessageSource
     * @param permChecker
     *            SpPermissionChecker
     * @param eventBus
     *            UIEventBus
     * @param manageDistUIState
     *            ManageDistUIState
     * @param entityFactory
     *            EntityFactory
     * @param uiNotification
     *            UINotification
     * @param softwareModuleTypeManagement
     *            SoftwareModuleTypeManagement
     * @param distributionSetTypeManagement
     *            DistributionSetTypeManagement
     * @param dSTypeFilterButtons
     *            DSTypeFilterButtons
     */
    DSTypeFilterHeader(final VaadinMessageSource i18n, final SpPermissionChecker permChecker, final UIEventBus eventBus,
            final ManageDistUIState manageDistUIState, final EntityFactory entityFactory,
            final UINotification uiNotification, final SoftwareModuleTypeManagement softwareModuleTypeManagement,
            final DistributionSetTypeManagement distributionSetTypeManagement,
            final DSTypeFilterButtons dSTypeFilterButtons) {
        super(permChecker, eventBus, i18n);
        this.manageDistUIState = manageDistUIState;
        this.entityFactory = entityFactory;
        this.softwareModuleTypeManagement = softwareModuleTypeManagement;
        this.distributionSetTypeManagement = distributionSetTypeManagement;
        this.uiNotification = uiNotification;
        this.dSTypeFilterButtons = dSTypeFilterButtons;
    }

    @Override
    protected String getTitle() {
        return getI18n().getMessage(UIMessageIdProvider.CAPTION_FILTER_BY_TYPE);
    }

    @Override
    protected boolean dropHitsRequired() {
        return false;
    }

    @Override
    protected void hideFilterButtonLayout() {
        manageDistUIState.setDistTypeFilterClosed(true);
        getEventBus().publish(this, DistributionsUIEvent.HIDE_DIST_FILTER_BY_TYPE);
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
        return command -> new CreateDistributionSetTypeLayout(getI18n(), entityFactory, getEventBus(), getPermChecker(),
                uiNotification, softwareModuleTypeManagement, distributionSetTypeManagement);
    }

    @Override
    protected Command getDeleteButtonCommand() {
        return command -> {
            dSTypeFilterButtons.addDeleteColumn();
            getEventBus().publish(this, new DistributionSetTypeFilterHeaderEvent(FilterHeaderEnum.SHOW_CANCEL_BUTTON));
        };
    }

    @Override
    protected Command getUpdateButtonCommand() {
        return command -> {
            dSTypeFilterButtons.addUpdateColumn();
            getEventBus().publish(this, new DistributionSetTypeFilterHeaderEvent(FilterHeaderEnum.SHOW_CANCEL_BUTTON));
        };
    }

    @Override
    protected void cancelUpdateOrDeleteTag(final ClickEvent event) {
        super.cancelUpdateOrDeleteTag(event);
        dSTypeFilterButtons.removeUpdateAndDeleteColumn();
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    private void onEvent(final DistributionSetTypeFilterHeaderEvent event) {
        processFilterHeaderEvent(event);
    }

    @Override
    protected String getMenuBarId() {
        return UIComponentIdProvider.DIST_TAG_MENU_BAR_ID;
    }

}
