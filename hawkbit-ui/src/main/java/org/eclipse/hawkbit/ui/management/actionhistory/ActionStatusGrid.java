/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.actionhistory;

import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.builder.GridComponentBuilder;
import org.eclipse.hawkbit.ui.common.builder.StatusIconBuilder.ActionStatusIconSupplier;
import org.eclipse.hawkbit.ui.common.data.mappers.ActionStatusToProxyActionStatusMapper;
import org.eclipse.hawkbit.ui.common.data.providers.ActionStatusDataProvider;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyAction;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyActionStatus;
import org.eclipse.hawkbit.ui.common.event.EventLayout;
import org.eclipse.hawkbit.ui.common.event.EventView;
import org.eclipse.hawkbit.ui.common.event.FilterType;
import org.eclipse.hawkbit.ui.common.grid.AbstractGrid;
import org.eclipse.hawkbit.ui.common.grid.support.FilterSupport;
import org.eclipse.hawkbit.ui.common.grid.support.MasterEntitySupport;
import org.eclipse.hawkbit.ui.common.grid.support.SelectionSupport;
import org.eclipse.hawkbit.ui.utils.SPDateTimeUtil;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;

/**
 * This grid presents the action states for a selected action.
 */
public class ActionStatusGrid extends AbstractGrid<ProxyActionStatus, Long> {
    private static final long serialVersionUID = 1L;

    private static final String STATUS_ID = "status";
    private static final String CREATED_AT_ID = "createdAt";

    private final ActionStatusIconSupplier<ProxyActionStatus> actionStatusIconSupplier;

    private final transient MasterEntitySupport<ProxyAction> masterEntitySupport;

    /**
     * Constructor.
     *
     * @param uiDependencies
     *            {@link CommonUiDependencies}
     * @param deploymentManagement
     *            deploymentManagement
     */
    public ActionStatusGrid(final CommonUiDependencies uiDependencies,
            final DeploymentManagement deploymentManagement) {
        super(uiDependencies.getI18n(), uiDependencies.getEventBus(), uiDependencies.getPermChecker());

        setSelectionSupport(new SelectionSupport<>(this, eventBus, EventLayout.ACTION_HISTORY_STATUS_LIST,
                EventView.DEPLOYMENT, null, null, null));
        getSelectionSupport().enableSingleSelection();

        setFilterSupport(new FilterSupport<>(
                new ActionStatusDataProvider(deploymentManagement, new ActionStatusToProxyActionStatusMapper())));
        initFilterMappings();

        this.masterEntitySupport = new MasterEntitySupport<>(getFilterSupport());

        actionStatusIconSupplier = new ActionStatusIconSupplier<>(i18n, ProxyActionStatus::getStatus,
                UIComponentIdProvider.ACTION_STATUS_GRID_STATUS_LABEL_ID);

        init();
    }

    private void initFilterMappings() {
        getFilterSupport().<Long> addMapping(FilterType.MASTER,
                (filter, masterFilter) -> getFilterSupport().setFilter(masterFilter));
    }

    @Override
    public String getGridId() {
        return UIComponentIdProvider.ACTION_HISTORY_DETAILS_GRID_ID;
    }

    @Override
    public void addColumns() {
        GridComponentBuilder
                .addIconColumn(this, actionStatusIconSupplier::getLabel, STATUS_ID, i18n.getMessage("header.status"))
                .setHidable(false).setHidden(false);

        GridComponentBuilder
                .addColumn(this,
                        actionStatus -> SPDateTimeUtil.getFormattedDate(actionStatus.getCreatedAt(),
                                SPUIDefinitions.LAST_QUERY_DATE_FORMAT_SHORT))
                .setId(CREATED_AT_ID).setCaption(i18n.getMessage("header.rolloutgroup.target.date"))
                .setDescriptionGenerator(actionStatus -> SPDateTimeUtil.getFormattedDate(actionStatus.getCreatedAt()))
                .setHidable(false).setHidden(false);
    }

    /**
     * @return Master entity support
     */
    public MasterEntitySupport<ProxyAction> getMasterEntitySupport() {
        return masterEntitySupport;
    }
}
