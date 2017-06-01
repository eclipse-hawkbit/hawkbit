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
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.ui.common.builder.LabelBuilder;
import org.eclipse.hawkbit.ui.filtermanagement.event.CustomFilterUIEvent;
import org.eclipse.hawkbit.ui.filtermanagement.state.FilterManagementUIState;
import org.eclipse.hawkbit.ui.utils.AssignInstalledDSTooltipGenerator;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.TableColumn;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.springframework.util.StringUtils;
import org.vaadin.addons.lazyquerycontainer.BeanQueryFactory;
import org.vaadin.addons.lazyquerycontainer.LazyQueryContainer;
import org.vaadin.addons.lazyquerycontainer.LazyQueryDefinition;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.EventBus.UIEventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.vaadin.data.Item;
import com.vaadin.server.FontAwesome;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.Table;
import com.vaadin.ui.UI;
import com.vaadin.ui.themes.ValoTheme;

/**
 *
 *
 */
public class CreateOrUpdateFilterTable extends Table {

    private static final long serialVersionUID = 6887304217281629713L;

    private final VaadinMessageSource i18n;

    private final FilterManagementUIState filterManagementUIState;

    private LazyQueryContainer container;

    private final transient EventBus.UIEventBus eventBus;

    private static final int PROPERTY_DEPT = 3;

    private static final String ASSIGN_DIST_SET = "assignedDistributionSet";

    private static final String INSTALL_DIST_SET = "installedDistributionSet";

    CreateOrUpdateFilterTable(final VaadinMessageSource i18n, final UIEventBus eventBus,
            final FilterManagementUIState filterManagementUIState) {
        this.i18n = i18n;
        this.filterManagementUIState = filterManagementUIState;
        this.eventBus = eventBus;

        setStyleName("sp-table");
        setSizeFull();
        setImmediate(true);
        setHeight(100.0F, Unit.PERCENTAGE);
        addStyleName(ValoTheme.TABLE_NO_VERTICAL_LINES);
        addStyleName(ValoTheme.TABLE_SMALL);
        setColumnCollapsingAllowed(true);
        addCustomGeneratedColumns();
        restoreOnLoad();
        populateTableData();
        setId(UIComponentIdProvider.CUSTOM_FILTER_TARGET_TABLE_ID);
        setSelectable(false);
        eventBus.subscribe(this);
        setItemDescriptionGenerator(new AssignInstalledDSTooltipGenerator());
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onEvent(final CustomFilterUIEvent custFUIEvent) {
        if (custFUIEvent == CustomFilterUIEvent.TARGET_DETAILS_VIEW
                || custFUIEvent == CustomFilterUIEvent.CREATE_NEW_FILTER_CLICK) {
            UI.getCurrent().access(this::populateTableData);
        } else if (custFUIEvent == CustomFilterUIEvent.FILTER_TARGET_BY_QUERY) {
            UI.getCurrent().access(this::onQuery);
        }
    }

    private void restoreOnLoad() {
        if (filterManagementUIState.isCreateFilterViewDisplayed()) {
            filterManagementUIState.setFilterQueryValue(null);
        } else {
            filterManagementUIState.getTfQuery()
                    .ifPresent(value -> filterManagementUIState.setFilterQueryValue(value.getQuery()));
        }
    }

    /**
     * Create a empty HierarchicalContainer.
     */
    private LazyQueryContainer createContainer() {
        // ADD all the filters to the query config
        final Map<String, Object> queryConfig = prepareQueryConfigFilters();

        // Create TargetBeanQuery factory with the query config.
        final BeanQueryFactory<CustomTargetBeanQuery> targetQF = new BeanQueryFactory<>(CustomTargetBeanQuery.class);
        targetQF.setQueryConfiguration(queryConfig);

        // create lazy query container with lazy defination and query
        final LazyQueryContainer targetTableContainer = new LazyQueryContainer(
                new LazyQueryDefinition(true, SPUIDefinitions.PAGE_SIZE, SPUILabelDefinitions.VAR_ID), targetQF);
        targetTableContainer.getQueryView().getQueryDefinition().setMaxNestedPropertyDepth(PROPERTY_DEPT);

        return targetTableContainer;

    }

    private Map<String, Object> prepareQueryConfigFilters() {
        final Map<String, Object> queryConfig = Maps.newHashMapWithExpectedSize(2);
        if (!StringUtils.isEmpty(filterManagementUIState.getFilterQueryValue())) {
            queryConfig.put(SPUIDefinitions.FILTER_BY_QUERY, filterManagementUIState.getFilterQueryValue());
        }
        queryConfig.put(SPUIDefinitions.FILTER_BY_INVALID_QUERY,
                filterManagementUIState.getIsFilterByInvalidFilterQuery());
        return queryConfig;
    }

    /**
     * populate campaign data.
     * 
     */
    public void populateTableData() {
        container = createContainer();
        setContainerDataSource(container);
        addContainerproperties();
        setColumnProperties();
        setPageLength(30);
        setCollapsibleColumns();
    }

    private void setCollapsibleColumns() {
        setColumnCollapsed(SPUILabelDefinitions.VAR_LAST_MODIFIED_BY, true);
        setColumnCollapsed(SPUILabelDefinitions.VAR_LAST_MODIFIED_DATE, true);

        setColumnCollapsed(ASSIGN_DIST_SET, true);
        setColumnCollapsed(INSTALL_DIST_SET, true);
    }

    /**
     * Create a empty HierarchicalContainer.
     */
    private void addContainerproperties() {
        /* Create HierarchicalContainer container */
        container.addContainerProperty(SPUILabelDefinitions.NAME, String.class, null);
        container.addContainerProperty(SPUILabelDefinitions.VAR_CREATED_BY, String.class, null);
        container.addContainerProperty(SPUILabelDefinitions.VAR_CREATED_DATE, Date.class, null);
        container.addContainerProperty(SPUILabelDefinitions.VAR_LAST_MODIFIED_BY, String.class, null, false, true);
        container.addContainerProperty(SPUILabelDefinitions.VAR_LAST_MODIFIED_DATE, String.class, null, false, true);
        container.addContainerProperty(SPUILabelDefinitions.VAR_TARGET_STATUS, TargetUpdateStatus.class, null);
        container.addContainerProperty(SPUILabelDefinitions.VAR_DESC, String.class, "", false, true);

        container.addContainerProperty(ASSIGN_DIST_SET, DistributionSet.class, null, false, true);
        container.addContainerProperty(INSTALL_DIST_SET, DistributionSet.class, null, false, true);
        container.addContainerProperty(SPUILabelDefinitions.ASSIGNED_DISTRIBUTION_NAME_VER, String.class, "");
        container.addContainerProperty(SPUILabelDefinitions.INSTALLED_DISTRIBUTION_NAME_VER, String.class, null);
    }

    private List<TableColumn> getVisbleColumns() {
        final List<TableColumn> columnList = Lists.newArrayListWithExpectedSize(7);
        columnList.add(new TableColumn(SPUILabelDefinitions.NAME, i18n.getMessage("header.name"), 0.15F));
        columnList.add(new TableColumn(SPUILabelDefinitions.VAR_CREATED_BY, i18n.getMessage("header.createdBy"), 0.1F));
        columnList.add(
                new TableColumn(SPUILabelDefinitions.VAR_CREATED_DATE, i18n.getMessage("header.createdDate"), 0.1F));
        columnList.add(
                new TableColumn(SPUILabelDefinitions.VAR_LAST_MODIFIED_BY, i18n.getMessage("header.modifiedBy"), 0.1F));
        columnList.add(new TableColumn(SPUILabelDefinitions.VAR_LAST_MODIFIED_DATE,
                i18n.getMessage("header.modifiedDate"), 0.1F));
        columnList.add(new TableColumn(SPUILabelDefinitions.VAR_DESC, i18n.getMessage("header.description"), 0.1F));
        columnList.add(new TableColumn(SPUILabelDefinitions.STATUS_ICON, i18n.getMessage("header.status"), 0.1F));

        return columnList;
    }

    private Component getStatusIcon(final Object itemId) {
        final Item row1 = getItem(itemId);
        final TargetUpdateStatus targetStatus = (TargetUpdateStatus) row1
                .getItemProperty(SPUILabelDefinitions.VAR_TARGET_STATUS).getValue();
        final Label label = new LabelBuilder().name("").buildLabel();
        label.setContentMode(ContentMode.HTML);
        if (targetStatus == TargetUpdateStatus.PENDING) {
            label.setDescription("Pending");
            label.setStyleName(SPUIStyleDefinitions.STATUS_ICON_YELLOW);
            label.setValue(FontAwesome.ADJUST.getHtml());
        } else if (targetStatus == TargetUpdateStatus.REGISTERED) {
            label.setDescription("Registered");
            label.setStyleName(SPUIStyleDefinitions.STATUS_ICON_LIGHT_BLUE);
            label.setValue(FontAwesome.DOT_CIRCLE_O.getHtml());
        } else if (targetStatus == TargetUpdateStatus.ERROR) {
            label.setDescription(i18n.getMessage("label.error"));
            label.setStyleName(SPUIStyleDefinitions.STATUS_ICON_RED);
            label.setValue(FontAwesome.EXCLAMATION_CIRCLE.getHtml());
        } else if (targetStatus == TargetUpdateStatus.IN_SYNC) {
            label.setStyleName(SPUIStyleDefinitions.STATUS_ICON_GREEN);
            label.setDescription("In-Synch");
            label.setValue(FontAwesome.CHECK_CIRCLE.getHtml());
        } else if (targetStatus == TargetUpdateStatus.UNKNOWN) {
            label.setStyleName(SPUIStyleDefinitions.STATUS_ICON_BLUE);
            label.setDescription(i18n.getMessage("label.unknown"));
            label.setValue(FontAwesome.QUESTION_CIRCLE.getHtml());
        }
        return label;
    }

    private void setColumnProperties() {
        final List<TableColumn> columnList = getVisbleColumns();
        final List<Object> swColumnIds = new ArrayList<>();
        for (final TableColumn column : columnList) {
            setColumnHeader(column.getColumnPropertyId(), column.getColumnHeader());
            setColumnExpandRatio(column.getColumnPropertyId(), column.getExpandRatio());
            swColumnIds.add(column.getColumnPropertyId());
        }
        setVisibleColumns(swColumnIds.toArray());
    }

    protected void addCustomGeneratedColumns() {
        addGeneratedColumn(SPUILabelDefinitions.STATUS_ICON, (source, itemId, columnId) -> getStatusIcon(itemId));
    }

    private void onQuery() {
        populateTableData();
        eventBus.publish(this, CustomFilterUIEvent.UPDATE_TARGET_FILTER_SEARCH_ICON);
    }

}
