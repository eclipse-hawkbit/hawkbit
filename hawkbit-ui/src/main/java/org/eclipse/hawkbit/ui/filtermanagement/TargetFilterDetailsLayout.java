/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.filtermanagement;

import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.rsql.RsqlValidationOracle;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.UiProperties;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTarget;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTargetFilterQuery;
import org.eclipse.hawkbit.ui.common.event.EventLayout;
import org.eclipse.hawkbit.ui.common.event.EventLayoutViewAware;
import org.eclipse.hawkbit.ui.common.event.EventView;
import org.eclipse.hawkbit.ui.common.event.EventViewAware;
import org.eclipse.hawkbit.ui.common.layout.AbstractGridComponentLayout;
import org.eclipse.hawkbit.ui.common.layout.listener.FilterChangedListener;
import org.eclipse.hawkbit.ui.common.layout.listener.ShowEntityFormLayoutListener;
import org.eclipse.hawkbit.ui.filtermanagement.state.TargetFilterDetailsLayoutUiState;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus.UIEventBus;

/**
 * DistributionSet table layout.
 */
public class TargetFilterDetailsLayout extends AbstractGridComponentLayout {
    private static final long serialVersionUID = 1L;

    private final TargetFilterDetailsGridHeader targetFilterDetailsGridHeader;
    private final TargetFilterTargetGrid targetFilterTargetGrid;
    private final transient TargetFilterCountMessageLabel targetFilterCountMessageLabel;

    private final transient ShowEntityFormLayoutListener<ProxyTargetFilterQuery> showFilterQueryFormListener;
    private final transient FilterChangedListener<ProxyTarget> targetFilterListener;

    /**
     * TargetFilterDetailsLayout constructor
     * 
     * @param i18n
     *            MessageSource
     * @param permChecker
     *            SpPermissionChecker
     * @param eventBus
     *            Bus to publish UI events
     * @param uiNotification
     *            helper to display messages
     * @param uiProperties
     *            properties
     * @param entityFactory
     *            entity factory
     * @param rsqlValidationOracle
     *            to get RSQL validation and suggestions
     * @param targetManagement
     *            management to get targets matching the filters
     * @param targetFilterManagement
     *            management to CRUD target filters
     * @param uiState
     *            to persist the user interaction
     */
    public TargetFilterDetailsLayout(final VaadinMessageSource i18n, final SpPermissionChecker permChecker,
            final UIEventBus eventBus, final UINotification uiNotification, final UiProperties uiProperties,
            final EntityFactory entityFactory, final RsqlValidationOracle rsqlValidationOracle,
            final TargetManagement targetManagement, final TargetFilterQueryManagement targetFilterManagement,
            final TargetFilterDetailsLayoutUiState uiState) {

        this.targetFilterDetailsGridHeader = new TargetFilterDetailsGridHeader(i18n, permChecker, eventBus,
                uiNotification, entityFactory, targetFilterManagement, uiProperties, rsqlValidationOracle, uiState);
        this.targetFilterTargetGrid = new TargetFilterTargetGrid(i18n, eventBus, targetManagement, uiState);
        this.targetFilterCountMessageLabel = new TargetFilterCountMessageLabel(i18n);

        initGridDataUpdatedListener();

        this.showFilterQueryFormListener = new ShowEntityFormLayoutListener<>(eventBus, ProxyTargetFilterQuery.class,
                new EventLayoutViewAware(EventLayout.TARGET_FILTER_QUERY_FORM, EventView.TARGET_FILTER),
                targetFilterDetailsGridHeader::showAddFilterLayout,
                targetFilterDetailsGridHeader::showEditFilterLayout);
        this.targetFilterListener = new FilterChangedListener<>(eventBus, ProxyTarget.class,
                new EventViewAware(EventView.TARGET_FILTER), targetFilterTargetGrid.getFilterSupport());

        buildLayout(targetFilterDetailsGridHeader, targetFilterTargetGrid, targetFilterCountMessageLabel);
    }

    private void initGridDataUpdatedListener() {
        targetFilterTargetGrid.addDataChangedListener(event -> targetFilterCountMessageLabel
                .updateTotalFilteredTargetsCount(targetFilterTargetGrid.getDataSize()));
    }

    /**
     * restore the saved state
     */
    public void restoreState() {
        targetFilterDetailsGridHeader.restoreState();
        if (targetFilterDetailsGridHeader.isFilterQueryValid()) {
            targetFilterTargetGrid.restoreState();
        }
    }

    /**
     * unsubscribe all listener
     */
    public void unsubscribeListener() {
        showFilterQueryFormListener.unsubscribe();
        targetFilterListener.unsubscribe();
    }
}
