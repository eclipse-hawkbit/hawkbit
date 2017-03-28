/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.actionhistory;

import java.util.Map;

import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.ui.common.grid.AbstractGrid;
import org.eclipse.hawkbit.ui.customrenderers.renderers.HtmlLabelRenderer;
import org.eclipse.hawkbit.ui.management.actionhistory.ActionHistoryGrid.LabelConfig;
import org.eclipse.hawkbit.ui.rollout.StatusFontIcon;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.addons.lazyquerycontainer.BeanQueryFactory;
import org.vaadin.addons.lazyquerycontainer.LazyQueryContainer;
import org.vaadin.addons.lazyquerycontainer.LazyQueryDefinition;
import org.vaadin.spring.events.EventBus.UIEventBus;

import com.google.common.collect.Maps;

/**
 * This grid presents the action states for a selected action.
 */
public class ActionStatusGrid extends AbstractGrid<LazyQueryContainer> {
    private static final long serialVersionUID = 1L;

    private static final String[] leftAlignedColumns = new String[] { ProxyActionStatus.PXY_AS_CREATED_AT };

    private static final String[] centerAlignedColumns = new String[] { ProxyActionStatus.PXY_AS_STATUS };

    private final AlignCellStyleGenerator alignGenerator;
    private final ModifiedTimeTooltipGenerator modTimetooltipGenerator;

    private final Map<Action.Status, StatusFontIcon> states;

    private final BeanQueryFactory<ActionStatusBeanQuery> targetQF = new BeanQueryFactory<>(
            ActionStatusBeanQuery.class);

    /**
     * Constructor.
     *
     * @param i18n
     * @param eventBus
     */
    protected ActionStatusGrid(final VaadinMessageSource i18n, final UIEventBus eventBus) {
        super(i18n, eventBus, null);

        setSingleSelectionSupport(new SingleSelectionSupport());
        setDetailsSupport(new DetailsSupport());

        final LabelConfig conf = new ActionHistoryGrid.LabelConfig();
        states = conf.createStatusLabelConfig(i18n, UIComponentIdProvider.ACTION_STATUS_GRID_STATUS_LABEL_ID);
        alignGenerator = new AlignCellStyleGenerator(leftAlignedColumns, centerAlignedColumns, null);
        modTimetooltipGenerator = new ModifiedTimeTooltipGenerator(ProxyActionStatus.PXY_AS_CREATED_AT);

        init();
    }

    @Override
    protected LazyQueryContainer createContainer() {
        configureQueryFactory();
        return new LazyQueryContainer(
                new LazyQueryDefinition(true, SPUIDefinitions.PAGE_SIZE, ProxyActionStatus.PXY_AS_ID), targetQF);
    }

    @Override
    public void refreshContainer() {
        configureQueryFactory();
        super.refreshContainer();
    }

    protected void configureQueryFactory() {
        // ADD all the filters to the query config
        final Map<String, Object> queryConfig = Maps.newHashMapWithExpectedSize(1);
        queryConfig.put(SPUIDefinitions.ACTIONSTATES_BY_ACTION, getDetailsSupport().getMasterDataId());
        // Create ActionBeanQuery factory with the query config.
        targetQF.setQueryConfiguration(queryConfig);
    }

    /**
     * Gets type-save access to LazyQueryContainer.
     *
     * @return LazyQueryContainer
     */
    private LazyQueryContainer getLazyQueryContainer() {
        return (LazyQueryContainer) getContainerDataSource();
    }

    @Override
    protected void addContainerProperties() {
        final LazyQueryContainer lqContainer = getLazyQueryContainer();
        lqContainer.addContainerProperty(ProxyActionStatus.PXY_AS_CREATED_AT, Long.class, null, true, true);
        lqContainer.addContainerProperty(ProxyActionStatus.PXY_AS_STATUS, Action.Status.class, null, true, false);
    }

    @Override
    protected void setColumnExpandRatio() {
        getColumn(ProxyActionStatus.PXY_AS_STATUS).setMinimumWidth(53);
        getColumn(ProxyActionStatus.PXY_AS_STATUS).setMaximumWidth(55);
        getColumn(ProxyActionStatus.PXY_AS_CREATED_AT).setMinimumWidth(100);
        getColumn(ProxyActionStatus.PXY_AS_CREATED_AT).setMaximumWidth(400);
    }

    @Override
    protected void setColumnHeaderNames() {
        getColumn(ProxyActionStatus.PXY_AS_STATUS).setHeaderCaption(SPUIDefinitions.ACTION_HIS_TBL_STATUS);
        getColumn(ProxyActionStatus.PXY_AS_CREATED_AT).setHeaderCaption(SPUIDefinitions.ACTION_HIS_TBL_DATETIME);
    }

    @Override
    protected String getGridId() {
        return UIComponentIdProvider.ACTION_HISTORY_DETAILS_GRID_ID;
    }

    @Override
    protected void setColumnProperties() {
        clearSortOrder();
        setColumns(ProxyActionStatus.PXY_AS_STATUS, ProxyActionStatus.PXY_AS_CREATED_AT);
        alignColumns();
    }

    @Override
    protected void addColumnRenderes() {
        getColumn(ProxyActionStatus.PXY_AS_STATUS).setRenderer(new HtmlLabelRenderer(),
                new HtmlStatusLabelConverter(this::createStatusLabelMetadata));
        getColumn(ProxyActionStatus.PXY_AS_CREATED_AT).setConverter(new LongToFormattedDateStringConverter());
    }

    private StatusFontIcon createStatusLabelMetadata(final Action.Status status) {
        return states.get(status);
    }

    @Override
    protected void setHiddenColumns() {
        getColumn(ProxyActionStatus.PXY_AS_STATUS).setHidable(false);
        getColumn(ProxyActionStatus.PXY_AS_CREATED_AT).setHidable(false);
    }

    /**
     * Sets the alignment cell-style-generator that handles the alignment for
     * the grid cells.
     */
    private void alignColumns() {
        setCellStyleGenerator(alignGenerator);
    }

    @Override
    protected CellDescriptionGenerator getDescriptionGenerator() {
        return modTimetooltipGenerator;
    }

}
