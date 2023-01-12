/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.rollout.rolloutgrouptargets;

import org.eclipse.hawkbit.repository.RolloutGroupManagement;
import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.builder.GridComponentBuilder;
import org.eclipse.hawkbit.ui.common.builder.StatusIconBuilder.RolloutActionStatusIconSupplier;
import org.eclipse.hawkbit.ui.common.data.mappers.TargetWithActionStatusToProxyTargetMapper;
import org.eclipse.hawkbit.ui.common.data.providers.RolloutGroupTargetsDataProvider;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyRolloutGroup;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTarget;
import org.eclipse.hawkbit.ui.common.event.FilterType;
import org.eclipse.hawkbit.ui.common.grid.AbstractGrid;
import org.eclipse.hawkbit.ui.common.grid.support.FilterSupport;
import org.eclipse.hawkbit.ui.common.grid.support.MasterEntitySupport;
import org.eclipse.hawkbit.ui.rollout.RolloutManagementUIState;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;

import com.vaadin.ui.Label;

/**
 * Grid component with targets of rollout group.
 */
public class RolloutGroupTargetGrid extends AbstractGrid<ProxyTarget, Long> {
    private static final long serialVersionUID = 1L;

    private static final String TARGET_CONTROLLER_ID = "targetControllerId";

    private final RolloutManagementUIState rolloutManagementUIState;

    private final RolloutActionStatusIconSupplier<ProxyTarget> actionStatusIconSupplier;

    private final transient MasterEntitySupport<ProxyRolloutGroup> masterEntitySupport;

    RolloutGroupTargetGrid(final CommonUiDependencies uiDependencies, final RolloutGroupManagement rolloutGroupManagement,
            final RolloutManagementUIState rolloutManagementUIState) {
        super(uiDependencies.getI18n(), uiDependencies.getEventBus(), uiDependencies.getPermChecker());
        this.rolloutManagementUIState = rolloutManagementUIState;

        setFilterSupport(new FilterSupport<>(new RolloutGroupTargetsDataProvider(rolloutGroupManagement,
                new TargetWithActionStatusToProxyTargetMapper())));
        initFilterMappings();

        this.masterEntitySupport = new MasterEntitySupport<>(getFilterSupport());

        actionStatusIconSupplier = new RolloutActionStatusIconSupplier<>(i18n, ProxyTarget::getStatus,
                UIComponentIdProvider.ROLLOUT_GROUP_TARGET_STATUS_LABEL_ID, rolloutGroupManagement,
                rolloutManagementUIState);

        // the min push size is set to 40 by default. This value is set as page
        // size in the page request and is in most cases to
        // small and would result in multiple DB calls to fetch more data.
        // Because retrieving actions is an expensive operation we want to make
        // only one DB call.
        // On the other hand the window size could not be retrieved at this
        // point in time to calculate how many rows can be displayed so
        // set it to a fixed value is a compromise here.
        // Value 250 was chosen because with this value in fullscreen on a 4k
        // display Vaadin creates one call to data provider.
        getDataCommunicator().setMinPushSize(250);

        init();

    }

    private void initFilterMappings() {
        getFilterSupport().<Long> addMapping(FilterType.MASTER,
                (filter, masterFilter) -> getFilterSupport().setFilter(masterFilter));
    }

    @Override
    public String getGridId() {
        return UIComponentIdProvider.ROLLOUT_GROUP_TARGETS_LIST_GRID_ID;
    }

    @Override
    public void addColumns() {
        GridComponentBuilder.addControllerIdColumn(this, i18n, TARGET_CONTROLLER_ID).setExpandRatio(2);

        GridComponentBuilder.addNameColumn(this, i18n, SPUILabelDefinitions.VAR_NAME).setExpandRatio(2);

        GridComponentBuilder.addDescriptionColumn(this, i18n, SPUILabelDefinitions.VAR_DESC).setExpandRatio(2);

        final Column<ProxyTarget, Label> statusColumn = GridComponentBuilder.addIconColumn(this,
                actionStatusIconSupplier::getLabel, SPUILabelDefinitions.VAR_STATUS,
                i18n.getMessage("header.status"));
        GridComponentBuilder.setColumnSortable(statusColumn, "status");

        final Column<ProxyTarget, Integer> statusCodeColumn = GridComponentBuilder.addColumn(this,
                ProxyTarget::getLastActionStatusCode).setId(SPUILabelDefinitions.VAR_STATUS_CODE)
                .setCaption(i18n.getMessage("header.status.code"));
        GridComponentBuilder.setColumnSortable(statusCodeColumn, "lastActionStatusCode");

        GridComponentBuilder.addCreatedAndModifiedColumns(this, i18n);

        getColumns().forEach(column -> column.setHidable(true));
    }

    @Override
    public void restoreState() {
        final Long masterEntityId = rolloutManagementUIState.getSelectedRolloutGroupId();
        if (masterEntityId != null) {
            getMasterEntitySupport().masterEntityChanged(new ProxyRolloutGroup(masterEntityId));
        }
    }

    /**
     * @return Rollout group master entity support
     */
    public MasterEntitySupport<ProxyRolloutGroup> getMasterEntitySupport() {
        return masterEntitySupport;
    }

}
