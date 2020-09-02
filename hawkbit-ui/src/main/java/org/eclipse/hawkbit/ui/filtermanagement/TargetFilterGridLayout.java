/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.filtermanagement;

import java.util.Collections;
import java.util.List;

import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTargetFilterQuery;
import org.eclipse.hawkbit.ui.common.event.EventView;
import org.eclipse.hawkbit.ui.common.event.EventViewAware;
import org.eclipse.hawkbit.ui.common.layout.AbstractGridComponentLayout;
import org.eclipse.hawkbit.ui.common.layout.listener.EntityModifiedListener;
import org.eclipse.hawkbit.ui.common.layout.listener.EntityModifiedListener.EntityModifiedAwareSupport;
import org.eclipse.hawkbit.ui.common.layout.listener.FilterChangedListener;
import org.eclipse.hawkbit.ui.common.layout.listener.support.EntityModifiedGridRefreshAwareSupport;
import org.eclipse.hawkbit.ui.filtermanagement.state.FilterManagementUIState;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus.UIEventBus;

/**
 * TargetFilter table layout.
 */
public class TargetFilterGridLayout extends AbstractGridComponentLayout {
    private static final long serialVersionUID = 1L;

    private final TargetFilterGridHeader targetFilterGridHeader;
    private final TargetFilterGrid targetFilterGrid;

    private final transient FilterChangedListener<ProxyTargetFilterQuery> targetQueryFilterListener;
    private final transient EntityModifiedListener<ProxyTargetFilterQuery> filterQueryModifiedListener;

    /**
     * TargetFilterGridLayout constructor
     * 
     * @param i18n
     *            MessageSource
     * @param eventBus
     *            Bus to publish UI events
     * @param permissionChecker
     *            Checker for user permissions
     * @param notification
     *            helper to display messages
     * @param entityFactory
     *            entity factory
     * @param targetFilterQueryManagement
     *            management to CRUD target filters
     * @param targetManagement
     *            management to get targets matching the filters
     * @param distributionSetManagement
     *            management to get distribution sets for auto-assignment
     * @param filterManagementUIState
     *            to persist the user interaction
     */
    public TargetFilterGridLayout(final VaadinMessageSource i18n, final UIEventBus eventBus,
            final SpPermissionChecker permissionChecker, final UINotification notification,
            final EntityFactory entityFactory, final TargetFilterQueryManagement targetFilterQueryManagement,
            final TargetManagement targetManagement, final DistributionSetManagement distributionSetManagement,
            final FilterManagementUIState filterManagementUIState) {
        this.targetFilterGridHeader = new TargetFilterGridHeader(eventBus,
                filterManagementUIState.getGridLayoutUiState(), permissionChecker, i18n);

        final AutoAssignmentWindowBuilder autoAssignmentWindowBuilder = new AutoAssignmentWindowBuilder(i18n, eventBus,
                notification, entityFactory, targetManagement, targetFilterQueryManagement, distributionSetManagement);

        this.targetFilterGrid = new TargetFilterGrid(i18n, notification, eventBus,
                filterManagementUIState.getGridLayoutUiState(), targetFilterQueryManagement, permissionChecker,
                autoAssignmentWindowBuilder);

        this.targetQueryFilterListener = new FilterChangedListener<>(eventBus, ProxyTargetFilterQuery.class,
                new EventViewAware(EventView.TARGET_FILTER), targetFilterGrid.getFilterSupport());
        this.filterQueryModifiedListener = new EntityModifiedListener.Builder<>(eventBus, ProxyTargetFilterQuery.class)
                .entityModifiedAwareSupports(getFilterQueryModifiedAwareSupports()).build();

        buildLayout(targetFilterGridHeader, targetFilterGrid);
    }

    private List<EntityModifiedAwareSupport> getFilterQueryModifiedAwareSupports() {
        return Collections.singletonList(EntityModifiedGridRefreshAwareSupport.of(targetFilterGrid::refreshAll));
    }

    /**
     * restore the saved state
     */
    public void restoreState() {
        targetFilterGridHeader.restoreState();
        targetFilterGrid.restoreState();
    }

    /**
     * unsubscribe all listener
     */
    public void unsubscribeListener() {
        targetQueryFilterListener.unsubscribe();
        filterQueryModifiedListener.unsubscribe();
    }
}
