/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.distributions.dstable;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.DistributionSetTypeManagement;
import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.repository.SoftwareModuleTypeManagement;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.common.data.filters.DsDistributionsFilterParams;
import org.eclipse.hawkbit.ui.common.data.providers.DistributionSetDistributionsStateDataProvider;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyDistributionSet;
import org.eclipse.hawkbit.ui.common.distributionset.AbstractDsGrid;
import org.eclipse.hawkbit.ui.common.event.EventView;
import org.eclipse.hawkbit.ui.common.event.FilterType;
import org.eclipse.hawkbit.ui.common.grid.support.DragAndDropSupport;
import org.eclipse.hawkbit.ui.common.grid.support.FilterSupport;
import org.eclipse.hawkbit.ui.common.grid.support.assignment.AssignmentSupport;
import org.eclipse.hawkbit.ui.common.grid.support.assignment.SwModulesToDistributionSetAssignmentSupport;
import org.eclipse.hawkbit.ui.common.state.GridLayoutUiState;
import org.eclipse.hawkbit.ui.common.state.TypeFilterLayoutUiState;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus.UIEventBus;

/**
 * Distribution set grid which is shown on the Distributions View.
 */
public class DistributionSetGrid extends AbstractDsGrid<DsDistributionsFilterParams> {
    private static final long serialVersionUID = 1L;

    private final TypeFilterLayoutUiState dSTypeFilterLayoutUiState;

    /**
     * Constructor for DistributionSetGrid
     *
     * @param eventBus
     *          UIEventBus
     * @param i18n
 *              VaadinMessageSource
     * @param permissionChecker
     *          SpPermissionChecker
     * @param notification
     *          UINotification
     * @param targetManagement
     *          TargetManagement
     * @param dsManagement
     *          DistributionSetManagement
     * @param smManagement
     *          SoftwareModuleManagement
     * @param dsTypeManagement
     *          DistributionSetTypeManagement
     * @param smTypeManagement
     *          SoftwareModuleTypeManagement
     * @param dSTypeFilterLayoutUiState
     *          TypeFilterLayoutUiState
     * @param distributionSetGridLayoutUiState
     *          GridLayoutUiState
     */
    public DistributionSetGrid(final UIEventBus eventBus, final VaadinMessageSource i18n,
            final SpPermissionChecker permissionChecker, final UINotification notification,
            final TargetManagement targetManagement, final DistributionSetManagement dsManagement,
            final SoftwareModuleManagement smManagement, final DistributionSetTypeManagement dsTypeManagement,
            final SoftwareModuleTypeManagement smTypeManagement,
            final TypeFilterLayoutUiState dSTypeFilterLayoutUiState,
            final GridLayoutUiState distributionSetGridLayoutUiState) {
        super(i18n, eventBus, permissionChecker, notification, dsManagement, distributionSetGridLayoutUiState,
                EventView.DISTRIBUTIONS);

        this.dSTypeFilterLayoutUiState = dSTypeFilterLayoutUiState;

        final Map<String, AssignmentSupport<?, ProxyDistributionSet>> sourceTargetAssignmentStrategies = new HashMap<>();
        final SwModulesToDistributionSetAssignmentSupport swModulesToDsAssignment = new SwModulesToDistributionSetAssignmentSupport(
                notification, i18n, eventBus, permissionChecker, targetManagement, dsManagement, smManagement,
                dsTypeManagement, smTypeManagement);
        sourceTargetAssignmentStrategies.put(UIComponentIdProvider.SOFTWARE_MODULE_TABLE, swModulesToDsAssignment);

        setDragAndDropSupportSupport(
                new DragAndDropSupport<>(this, i18n, notification, sourceTargetAssignmentStrategies, eventBus));
        if (!distributionSetGridLayoutUiState.isMaximized()) {
            getDragAndDropSupportSupport().addDropTarget();
        }

        setFilterSupport(new FilterSupport<>(new DistributionSetDistributionsStateDataProvider(dsManagement,
                dsTypeManagement, dsToProxyDistributionMapper), getSelectionSupport()::deselectAll));
        initFilterMappings();
        getFilterSupport().setFilter(new DsDistributionsFilterParams());

        initIsCompleteStyleGenerator();
        init();
    }

    private void initFilterMappings() {
        getFilterSupport().addMapping(FilterType.SEARCH, DsDistributionsFilterParams::setSearchText,
                distributionSetGridLayoutUiState.getSearchFilter());
        getFilterSupport().addMapping(FilterType.TYPE, DsDistributionsFilterParams::setDsTypeId,
                dSTypeFilterLayoutUiState.getClickedTypeId());
    }

    private void initIsCompleteStyleGenerator() {
        setStyleGenerator(ds -> ds.getIsComplete() ? null : SPUIDefinitions.DISABLE_DISTRIBUTION);
    }

    @Override
    public String getGridId() {
        return UIComponentIdProvider.DIST_SET_TABLE_ID;
    }

    @Override
    public void addColumns() {
        addNameColumn().setExpandRatio(2);
        addVersionColumn();
        addDeleteColumn();
    }
}
