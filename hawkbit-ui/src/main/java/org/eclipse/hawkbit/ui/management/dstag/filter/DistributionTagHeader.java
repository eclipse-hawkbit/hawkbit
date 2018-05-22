/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.dstag.filter;

import org.eclipse.hawkbit.repository.DistributionSetTagManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.common.event.FilterHeaderEvent;
import org.eclipse.hawkbit.ui.common.event.FilterHeaderEvent.FilterHeaderEnum;
import org.eclipse.hawkbit.ui.common.filterlayout.AbstractFilterHeader;
import org.eclipse.hawkbit.ui.management.dstag.CreateDistributionSetTagLayout;
import org.eclipse.hawkbit.ui.management.event.ManagementUIEvent;
import org.eclipse.hawkbit.ui.management.state.ManagementUIState;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus.UIEventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.MenuBar.Command;
import com.vaadin.ui.MenuBar.MenuItem;

/**
 * Table header for filtering distribution set tags
 *
 */
public class DistributionTagHeader extends AbstractFilterHeader {

    private static final long serialVersionUID = 1L;

    private final ManagementUIState managementUIState;

    private final transient EntityFactory entityFactory;

    private final UINotification uiNotification;

    private final transient DistributionSetTagManagement distributionSetTagManagement;

    private final DistributionTagButtons distributionTagButtons;

    DistributionTagHeader(final VaadinMessageSource i18n, final ManagementUIState managementUIState,
            final SpPermissionChecker permChecker, final UIEventBus eventBus,
            final DistributionSetTagManagement distributionSetTagManagement, final EntityFactory entityFactory,
            final UINotification uiNotification, final DistributionTagButtons distributionTagButtons) {
        super(permChecker, eventBus, i18n);
        this.entityFactory = entityFactory;
        this.managementUIState = managementUIState;
        this.uiNotification = uiNotification;
        this.distributionSetTagManagement = distributionSetTagManagement;
        this.distributionTagButtons = distributionTagButtons;
    }

    @Override
    protected String getHideButtonId() {
        return "hide.distribution.tags";
    }

    @Override
    protected String getTitle() {
        return getI18n().getMessage("header.filter.tag");
    }

    @Override
    protected boolean dropHitsRequired() {
        return true;
    }

    @Override
    protected void hideFilterButtonLayout() {
        managementUIState.setDistTagFilterClosed(true);
        getEventBus().publish(this, ManagementUIEvent.HIDE_DISTRIBUTION_TAG_LAYOUT);
    }

    @Override
    protected String getConfigureFilterButtonId() {
        return UIComponentIdProvider.ADD_DISTRIBUTION_TAG;
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
                new CreateDistributionSetTagLayout(getI18n(), distributionSetTagManagement, entityFactory,
                        getEventBus(), getPermChecker(), uiNotification);
            }
        };
    }

    @Override
    protected Command getDeleteButtonCommand() {
        return new MenuBar.Command() {

            private static final long serialVersionUID = 1L;

            @Override
            public void menuSelected(final MenuItem selectedItem) {
                distributionTagButtons.addDeleteColumn();
                getEventBus().publish(this, new FilterHeaderEvent<DistributionSetTag>(
                        FilterHeaderEnum.SHOW_CANCEL_BUTTON, DistributionSetTag.class));
            }
        };
    }

    @Override
    protected Command getUpdateButtonCommand() {
        return new MenuBar.Command() {

            private static final long serialVersionUID = 1L;

            @Override
            public void menuSelected(final MenuItem selectedItem) {
                distributionTagButtons.addEditColumn();
                getEventBus().publish(this, new FilterHeaderEvent<DistributionSetTag>(
                        FilterHeaderEnum.SHOW_CANCEL_BUTTON, DistributionSetTag.class));
            }
        };
    }

    @Override
    protected void cancelUpdateOrDeleteTag(final ClickEvent event) {
        super.cancelUpdateOrDeleteTag(event);
        distributionTagButtons.removeEditAndDeleteColumn();
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    private void onEvent(final FilterHeaderEvent<DistributionSetTag> event) {
        if (DistributionSetTag.class == event.getEntityType()) {
            processFilterHeaderEvent(event);
        }
    }

}
