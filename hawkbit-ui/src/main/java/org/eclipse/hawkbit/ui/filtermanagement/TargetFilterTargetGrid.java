/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.filtermanagement;

import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.builder.GridComponentBuilder;
import org.eclipse.hawkbit.ui.common.builder.StatusIconBuilder.TargetStatusIconSupplier;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTarget;
import org.eclipse.hawkbit.ui.common.data.suppliers.TargetFilterStateDataSupplier;
import org.eclipse.hawkbit.ui.common.event.FilterType;
import org.eclipse.hawkbit.ui.common.grid.AbstractGrid;
import org.eclipse.hawkbit.ui.common.grid.support.FilterSupport;
import org.eclipse.hawkbit.ui.filtermanagement.state.TargetFilterDetailsLayoutUiState;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;

import com.vaadin.ui.Label;

/**
 * Shows the targets as a result of the executed filter query.
 */
public class TargetFilterTargetGrid extends AbstractGrid<ProxyTarget, String> {
    private static final long serialVersionUID = 1L;

    private static final String TARGET_CONTROLLER_ID = "targetControllerId";
    private static final String TARGET_NAME_ID = "targetName";
    private static final String TARGET_DESCRIPTION_ID = "targetDescription";
    private static final String TARGET_STATUS_ID = "targetStatus";

    private final TargetStatusIconSupplier<ProxyTarget> targetStatusIconSupplier;

    private final TargetFilterDetailsLayoutUiState uiState;

    /**
     * TargetFilterTargetGrid constructor.
     *
     * @param uiDependencies
     *            {@link CommonUiDependencies}
     * @param targetFilterStateDataSupplier
     *            data supplier
     * @param uiState
     *            layout state
     */
    public TargetFilterTargetGrid(final CommonUiDependencies uiDependencies,
            final TargetFilterStateDataSupplier targetFilterStateDataSupplier,
            final TargetFilterDetailsLayoutUiState uiState) {
        super(uiDependencies.getI18n(), uiDependencies.getEventBus(), null,
                targetFilterStateDataSupplier.dataCommunicator());

        this.uiState = uiState;

        setFilterSupport(new FilterSupport<>(targetFilterStateDataSupplier.dataProvider()));
        initFilterMappings();

        targetStatusIconSupplier = new TargetStatusIconSupplier<>(i18n, ProxyTarget::getUpdateStatus, TARGET_STATUS_ID);
        init();
    }

    private void initFilterMappings() {
        getFilterSupport().<String> addMapping(FilterType.QUERY, (filter, queryText) -> setQueryFilter(queryText),
                uiState.getFilterQueryValueInput());
    }

    private void setQueryFilter(final String queryText) {
        getFilterSupport().setFilter(queryText);
    }

    @Override
    public void init() {
        super.init();

        addStyleName("grid-row-border");
    }

    @Override
    public String getGridId() {
        return UIComponentIdProvider.CUSTOM_FILTER_TARGET_TABLE_ID;
    }

    @Override
    public void addColumns() {
        GridComponentBuilder.addControllerIdColumn(this, i18n, TARGET_CONTROLLER_ID);

        GridComponentBuilder.addNameColumn(this, i18n, TARGET_NAME_ID);

        GridComponentBuilder.addDescriptionColumn(this, i18n, TARGET_DESCRIPTION_ID);

        final Column<ProxyTarget, Label> statusColumn = GridComponentBuilder.addIconColumn(this,
                targetStatusIconSupplier::getLabel, TARGET_STATUS_ID,
                i18n.getMessage("header.status"));
        GridComponentBuilder.setColumnSortable(statusColumn, "updateStatus");

        GridComponentBuilder.addCreatedAndModifiedColumns(this, i18n);

        getColumns().forEach(column -> column.setHidable(true));
    }

}
