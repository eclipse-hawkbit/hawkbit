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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.filtermanagement.event.CustomFilterUIEvent;
import org.eclipse.hawkbit.ui.filtermanagement.state.FilterManagementUIState;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.SPUIComponetIdProvider;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.eclipse.hawkbit.ui.utils.TableColumn;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.addons.lazyquerycontainer.BeanQueryFactory;
import org.vaadin.addons.lazyquerycontainer.LazyQueryContainer;
import org.vaadin.addons.lazyquerycontainer.LazyQueryDefinition;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.google.common.base.Strings;
import com.vaadin.data.Item;
import com.vaadin.server.FontAwesome;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.Table;
import com.vaadin.ui.UI;
import com.vaadin.ui.themes.ValoTheme;

/**
 *
 *
 */
@SpringComponent
@ViewScope
public class CreateOrUpdateFilterTable extends Table {

    private static final long serialVersionUID = 6887304217281629713L;

    @Autowired
    private I18N i18n;

    @Autowired
    private transient EventBus.SessionEventBus eventBus;

    @Autowired
    private FilterManagementUIState filterManagementUIState;

    private LazyQueryContainer container;

    private static final int PROPERTY_DEPT = 3;

    private static final String ASSIGN_DIST_SET = "assignedDistributionSet";

    private static final String INSTALL_DIST_SET = "installedDistributionSet";

    /**
     * Initialize the Action History Table.
     */
    @PostConstruct
    public void init() {
        setStyleName("sp-table");
        setSizeFull();
        setImmediate(true);
        setHeight(100.0f, Unit.PERCENTAGE);
        addStyleName(ValoTheme.TABLE_NO_VERTICAL_LINES);
        addStyleName(ValoTheme.TABLE_SMALL);
        setColumnCollapsingAllowed(true);
        addCustomGeneratedColumns();
        restoreOnLoad();
        populateTableData();
        setId(SPUIComponetIdProvider.CUSTOM_FILTER_TARGET_TABLE_ID);
        setSelectable(false);
        eventBus.subscribe(this);
        setItemDescriptionGenerator(new TooltipGenerator());
    }

    @PreDestroy
    void destroy() {
        eventBus.unsubscribe(this);
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final CustomFilterUIEvent custFUIEvent) {
        if (custFUIEvent == CustomFilterUIEvent.TARGET_DETAILS_VIEW
                || custFUIEvent == CustomFilterUIEvent.CREATE_NEW_FILTER_CLICK) {
            UI.getCurrent().access(() -> populateTableData());
        } else if (custFUIEvent == CustomFilterUIEvent.FILTER_TARGET_BY_QUERY) {
            UI.getCurrent().access(() -> onQuery());
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
                new LazyQueryDefinition(true, SPUIDefinitions.PAGE_SIZE, SPUILabelDefinitions.VAR_CONT_ID_NAME),
                targetQF);
        targetTableContainer.getQueryView().getQueryDefinition().setMaxNestedPropertyDepth(PROPERTY_DEPT);

        return targetTableContainer;

    }

    private Map<String, Object> prepareQueryConfigFilters() {
        final Map<String, Object> queryConfig = new HashMap<>();
        if (!Strings.isNullOrEmpty(filterManagementUIState.getFilterQueryValue())) {
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
        container.addContainerProperty(SPUILabelDefinitions.ASSIGNED_DISTRIBUTION_NAME_VER, String.class, "");
        container.addContainerProperty(SPUILabelDefinitions.INSTALLED_DISTRIBUTION_NAME_VER, String.class, null);
        container.addContainerProperty(SPUILabelDefinitions.VAR_TARGET_STATUS, TargetUpdateStatus.class, null);
        container.addContainerProperty(SPUILabelDefinitions.VAR_DESC, String.class, "", false, true);

        container.addContainerProperty(ASSIGN_DIST_SET, DistributionSet.class, null, false, true);
        container.addContainerProperty(INSTALL_DIST_SET, DistributionSet.class, null, false, true);
    }

    private List<TableColumn> getVisbleColumns() {
        final List<TableColumn> columnList = new ArrayList<>();
        columnList.add(new TableColumn(SPUILabelDefinitions.NAME, i18n.get("header.name"), 0.15f));
        columnList.add(new TableColumn(SPUILabelDefinitions.VAR_CREATED_BY, i18n.get("header.createdBy"), 0.1f));
        columnList.add(new TableColumn(SPUILabelDefinitions.VAR_CREATED_DATE, i18n.get("header.createdDate"), 0.1F));
        columnList.add(new TableColumn(SPUILabelDefinitions.VAR_LAST_MODIFIED_BY, i18n.get("header.modifiedBy"), 0.1F));
        columnList.add(
                new TableColumn(SPUILabelDefinitions.VAR_LAST_MODIFIED_DATE, i18n.get("header.modifiedDate"), 0.1F));
        columnList.add(new TableColumn(SPUILabelDefinitions.ASSIGNED_DISTRIBUTION_NAME_VER,
                i18n.get("header.assigned.ds"), 0.125F));
        columnList.add(new TableColumn(SPUILabelDefinitions.INSTALLED_DISTRIBUTION_NAME_VER,
                i18n.get("header.installed.ds"), 0.125F));
        columnList.add(new TableColumn(SPUILabelDefinitions.VAR_DESC, i18n.get("header.description"), 0.1F));
        columnList.add(new TableColumn(SPUILabelDefinitions.STATUS_ICON, i18n.get("header.status"), 0.1F));
        return columnList;
    }

    private Component getStatusIcon(final Object itemId) {
        final Item row1 = getItem(itemId);
        final TargetUpdateStatus targetStatus = (TargetUpdateStatus) row1
                .getItemProperty(SPUILabelDefinitions.VAR_TARGET_STATUS).getValue();
        final Label label = SPUIComponentProvider.getLabel("", SPUILabelDefinitions.SP_LABEL_SIMPLE);
        label.setContentMode(ContentMode.HTML);
        if (targetStatus == TargetUpdateStatus.PENDING) {
            label.setDescription("Pending");
            label.setStyleName("statusIconYellow");
            label.setValue(FontAwesome.ADJUST.getHtml());
        } else if (targetStatus == TargetUpdateStatus.REGISTERED) {
            label.setDescription("Registered");
            label.setStyleName("statusIconLightBlue");
            label.setValue(FontAwesome.DOT_CIRCLE_O.getHtml());
        } else if (targetStatus == TargetUpdateStatus.ERROR) {
            label.setDescription(i18n.get("label.error"));
            label.setStyleName("statusIconRed");
            label.setValue(FontAwesome.EXCLAMATION_CIRCLE.getHtml());
        } else if (targetStatus == TargetUpdateStatus.IN_SYNC) {
            label.setStyleName("statusIconGreen");
            label.setDescription("In-Synch");
            label.setValue(FontAwesome.CHECK_CIRCLE.getHtml());
        } else if (targetStatus == TargetUpdateStatus.UNKNOWN) {
            label.setStyleName("statusIconBlue");
            label.setDescription(i18n.get("label.unknown"));
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

    public class TooltipGenerator implements ItemDescriptionGenerator {
        private static final long serialVersionUID = 688730421728162456L;

        @Override
        public String generateDescription(Component source, Object itemId, Object propertyId) {
            final DistributionSet distributionSet;
            final Item item = getItem(itemId);
            if (propertyId != null) {
                if (propertyId.equals(SPUILabelDefinitions.ASSIGNED_DISTRIBUTION_NAME_VER)) {
                    distributionSet = (DistributionSet) item.getItemProperty(ASSIGN_DIST_SET).getValue();
                    return getDSDetails(distributionSet);
                } else if (propertyId.equals(SPUILabelDefinitions.INSTALLED_DISTRIBUTION_NAME_VER)) {
                    distributionSet = (DistributionSet) item.getItemProperty(INSTALL_DIST_SET).getValue();
                    return getDSDetails(distributionSet);
                }
            }
            return null;
        }

        private String getDSDetails(final DistributionSet distributionSet) {
            StringBuilder swModuleNames = new StringBuilder();
            StringBuilder swModuleVendors = new StringBuilder();
            final Set<SoftwareModule> swModules = (Set<SoftwareModule>) distributionSet.getModules();
            swModules.forEach(swModule -> {
                swModuleNames.append(swModule.getName());
                swModuleNames.append(" , ");
                swModuleVendors.append(swModule.getVendor());
                swModuleVendors.append(" , ");
            });
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("<ul>");
            stringBuilder.append("<li>");
            stringBuilder.append(" DistributionSet Description : ").append((String) distributionSet.getDescription());
            stringBuilder.append("</li>");
            stringBuilder.append("<li>");
            stringBuilder.append(" DistributionSet Type : ").append((distributionSet.getType()).getName());
            stringBuilder.append("</li>");
            stringBuilder.append("<li>");
            stringBuilder.append(" Required Migration step : ")
                    .append(distributionSet.isRequiredMigrationStep() ? "Yes" : "No");
            stringBuilder.append("</li>");
            stringBuilder.append("<li>");
            stringBuilder.append("SoftWare Modules : ").append(swModuleNames.toString());
            stringBuilder.append("</li>");
            stringBuilder.append("<li>");
            stringBuilder.append("Vendor(s) : ").append(swModuleVendors.toString());
            stringBuilder.append("</li>");

            stringBuilder.append("</ul>");
            return stringBuilder.toString();
        }

    }
}
