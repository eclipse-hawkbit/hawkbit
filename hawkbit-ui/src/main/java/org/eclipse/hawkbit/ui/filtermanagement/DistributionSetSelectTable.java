/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.filtermanagement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.hawkbit.repository.event.remote.DistributionSetDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetCreatedEvent;
import org.eclipse.hawkbit.ui.distributions.dstable.ManageDistBeanQuery;
import org.eclipse.hawkbit.ui.distributions.state.ManageDistUIState;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.eclipse.hawkbit.ui.utils.TableColumn;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.addons.lazyquerycontainer.BeanQueryFactory;
import org.vaadin.addons.lazyquerycontainer.LazyQueryContainer;
import org.vaadin.addons.lazyquerycontainer.LazyQueryDefinition;
import org.vaadin.spring.events.EventBus.UIEventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.vaadin.data.Container;
import com.vaadin.ui.Table;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Table for selecting a distribution set.
 */
public class DistributionSetSelectTable extends Table {

    private static final long serialVersionUID = -4307487829435471759L;

    private final VaadinMessageSource i18n;

    private final ManageDistUIState manageDistUIState;

    private Container container;

    DistributionSetSelectTable(final VaadinMessageSource i18n, final UIEventBus eventBus,
            final ManageDistUIState manageDistUIState) {
        this.i18n = i18n;
        this.manageDistUIState = manageDistUIState;
        setStyleName("sp-table");
        setSizeFull();
        setSelectable(true);
        setMultiSelect(false);
        setImmediate(true);
        addStyleName(ValoTheme.TABLE_NO_VERTICAL_LINES);
        addStyleName(ValoTheme.TABLE_SMALL);
        populateTableData();
        setColumnCollapsingAllowed(false);
        setColumnProperties();
        setId(UIComponentIdProvider.DIST_SET_SELECT_TABLE_ID);
        eventBus.subscribe(this);
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onEvents(final List<?> events) {
        final Object firstEvent = events.get(0);
        if (DistributionSetCreatedEvent.class.isInstance(firstEvent)
                || DistributionSetDeletedEvent.class.isInstance(firstEvent)) {
            refreshDistributions();
        }
    }

    private void populateTableData() {
        container = createContainer();
        addContainerproperties();
        setContainerDataSource(container);
        setColumnProperties();

    }

    protected Container createContainer() {

        final Map<String, Object> queryConfiguration = prepareQueryConfigFilters();
        final BeanQueryFactory<ManageDistBeanQuery> distributionQF = new BeanQueryFactory<>(ManageDistBeanQuery.class);

        distributionQF.setQueryConfiguration(queryConfiguration);
        return new LazyQueryContainer(
                new LazyQueryDefinition(true, SPUIDefinitions.PAGE_SIZE, SPUILabelDefinitions.VAR_ID), distributionQF);
    }

    private void addContainerproperties() {
        /* Create HierarchicalContainer container */
        container.addContainerProperty(SPUILabelDefinitions.NAME, String.class, null);
        container.addContainerProperty(SPUILabelDefinitions.VAR_VERSION, String.class, null);
    }

    private List<TableColumn> getVisbleColumns() {
        final List<TableColumn> columnList = new ArrayList<>(2);
        columnList.add(new TableColumn(SPUILabelDefinitions.NAME, i18n.getMessage("header.name"), 0.6F));
        columnList.add(new TableColumn(SPUILabelDefinitions.VAR_VERSION, i18n.getMessage("header.version"), 0.4F));
        return columnList;

    }

    private void setColumnProperties() {
        setVisibleColumns(getVisbleColumns().stream().map(column -> {
            setColumnHeader(column.getColumnPropertyId(), column.getColumnHeader());
            setColumnExpandRatio(column.getColumnPropertyId(), column.getExpandRatio());
            return column.getColumnPropertyId();
        }).toArray());
    }

    private Map<String, Object> prepareQueryConfigFilters() {
        final Map<String, Object> queryConfig = new HashMap<>();
        manageDistUIState.getManageDistFilters().getSearchText()
                .ifPresent(value -> queryConfig.put(SPUIDefinitions.FILTER_BY_TEXT, value));

        if (null != manageDistUIState.getManageDistFilters().getClickedDistSetType()) {
            queryConfig.put(SPUIDefinitions.FILTER_BY_DISTRIBUTION_SET_TYPE,
                    manageDistUIState.getManageDistFilters().getClickedDistSetType());
        }

        queryConfig.put(SPUIDefinitions.FILTER_BY_DS_COMPLETE, Boolean.TRUE);

        return queryConfig;
    }

    private void refreshDistributions() {
        final LazyQueryContainer dsContainer = (LazyQueryContainer) getContainerDataSource();
        final int size = dsContainer.size();
        if (size < SPUIDefinitions.MAX_TABLE_ENTRIES) {
            refreshTablecontainer();
        }
        if (size != 0) {
            setData(SPUIDefinitions.DATA_AVAILABLE);
        }
    }

    private Object getItemIdToSelect() {
        return manageDistUIState.getSelectedDistributions().isEmpty() ? null
                : manageDistUIState.getSelectedDistributions();
    }

    private void selectRow() {
        setValue(getItemIdToSelect());
    }

    private void refreshTablecontainer() {
        final LazyQueryContainer dsContainer = (LazyQueryContainer) getContainerDataSource();
        dsContainer.refresh();
        selectRow();
    }

}
