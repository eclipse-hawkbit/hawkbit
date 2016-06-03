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

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.eclipse.hawkbit.repository.TargetFilterQueryManagement;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.eclipse.hawkbit.ui.common.ConfirmationDialog;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleSmallNoBorder;
import org.eclipse.hawkbit.ui.filtermanagement.event.CustomFilterUIEvent;
import org.eclipse.hawkbit.ui.filtermanagement.state.FilterManagementUIState;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.SPUIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.eclipse.hawkbit.ui.utils.TableColumn;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.addons.lazyquerycontainer.BeanQueryFactory;
import org.vaadin.addons.lazyquerycontainer.LazyQueryContainer;
import org.vaadin.addons.lazyquerycontainer.LazyQueryDefinition;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.vaadin.data.Container;
import com.vaadin.data.Item;
import com.vaadin.server.FontAwesome;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Link;
import com.vaadin.ui.Table;
import com.vaadin.ui.UI;
import com.vaadin.ui.themes.ValoTheme;

/**
 *
 *
 */
@SpringComponent
@ViewScope
public class TargetFilterTable extends Table {

    private static final long serialVersionUID = -4307487829435474759L;

    @Autowired
    private I18N i18n;

    @Autowired
    private UINotification notification;

    @Autowired
    private transient EventBus.SessionEventBus eventBus;

    @Autowired
    private FilterManagementUIState filterManagementUIState;

    @Autowired
    private transient TargetFilterQueryManagement targetFilterQueryManagement;

    private Container container;

    private static final int PROPERTY_DEPT = 3;

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
        addCustomGeneratedColumns();
        populateTableData();
        setColumnCollapsingAllowed(true);
        setColumnProperties();
        setId(SPUIComponentIdProvider.TAEGET_FILTER_TABLE_ID);
        eventBus.subscribe(this);
    }

    @PreDestroy
    void destroy() {
        eventBus.unsubscribe(this);
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final CustomFilterUIEvent filterEvent) {
        if (filterEvent == CustomFilterUIEvent.FILTER_BY_CUST_FILTER_TEXT
                || filterEvent == CustomFilterUIEvent.FILTER_BY_CUST_FILTER_TEXT_REMOVE
                || filterEvent == CustomFilterUIEvent.CREATE_TARGET_FILTER_QUERY
                || filterEvent == CustomFilterUIEvent.UPDATED_TARGET_FILTER_QUERY) {
            UI.getCurrent().access(() -> refreshContainer());
        }
    }

    private Container createContainer() {
        final Map<String, Object> queryConfig = prepareQueryConfigFilters();
        final BeanQueryFactory<TargetFilterBeanQuery> targetQF = new BeanQueryFactory<>(TargetFilterBeanQuery.class);

        targetQF.setQueryConfiguration(queryConfig);
        // create lazy query container with lazy defination and query
        final LazyQueryContainer targetFilterContainer = new LazyQueryContainer(
                new LazyQueryDefinition(true, SPUIDefinitions.PAGE_SIZE, SPUILabelDefinitions.VAR_ID), targetQF);
        targetFilterContainer.getQueryView().getQueryDefinition().setMaxNestedPropertyDepth(PROPERTY_DEPT);

        return targetFilterContainer;

    }

    private Map<String, Object> prepareQueryConfigFilters() {
        final Map<String, Object> queryConfig = new HashMap<>();
        filterManagementUIState.getCustomFilterSearchText()
                .ifPresent(value -> queryConfig.put(SPUIDefinitions.FILTER_BY_TEXT, value));
        return queryConfig;
    }

    private void addContainerproperties() {
        /* Create HierarchicalContainer container */
        container.addContainerProperty(SPUILabelDefinitions.NAME, Link.class, null);
        container.addContainerProperty(SPUILabelDefinitions.VAR_CREATED_USER, String.class, null);
        container.addContainerProperty(SPUILabelDefinitions.VAR_CREATED_DATE, Date.class, null);
        container.addContainerProperty(SPUILabelDefinitions.VAR_MODIFIED_DATE, Date.class, null);
        container.addContainerProperty(SPUILabelDefinitions.VAR_MODIFIED_BY, String.class, null);
    }

    private List<TableColumn> getVisbleColumns() {
        final List<TableColumn> columnList = new ArrayList<>();
        columnList.add(new TableColumn(SPUILabelDefinitions.NAME, i18n.get("header.name"), 0.2F));
        columnList.add(new TableColumn(SPUILabelDefinitions.VAR_CREATED_USER, i18n.get("header.createdBy"), 0.15F));
        columnList.add(new TableColumn(SPUILabelDefinitions.VAR_CREATED_DATE, i18n.get("header.createdDate"), 0.2F));
        columnList.add(new TableColumn(SPUILabelDefinitions.VAR_MODIFIED_BY, i18n.get("header.modifiedBy"), 0.15F));
        columnList.add(new TableColumn(SPUILabelDefinitions.VAR_MODIFIED_DATE, i18n.get("header.modifiedDate"), 0.2F));
        columnList.add(new TableColumn(SPUIDefinitions.CUSTOM_FILTER_DELETE, i18n.get("header.delete"), 0.1F));
        return columnList;

    }

    private void refreshContainer() {
        populateTableData();

    }

    private Button getDeleteButton(final Long itemId) {
        final Item row = getItem(itemId);
        final String tfName = (String) row.getItemProperty(SPUILabelDefinitions.NAME).getValue();
        final Button deleteIcon = SPUIComponentProvider.getButton(getDeleteIconId(tfName), "",
                SPUILabelDefinitions.DELETE_CUSTOM_FILTER, ValoTheme.BUTTON_TINY + " " + "redicon", true,
                FontAwesome.TRASH_O, SPUIButtonStyleSmallNoBorder.class);
        deleteIcon.setData(itemId);
        deleteIcon.addClickListener(this::onDelete);
        return deleteIcon;
    }

    private String getDeleteIconId(final String targetFilterName) {
        return new StringBuilder(SPUIComponentIdProvider.CUSTOM_FILTER_DELETE_ICON).append('.').append(targetFilterName)
                .toString();
    }

    private void onDelete(final ClickEvent event) {
        /* Display the confirmation */
        final ConfirmationDialog confirmDialog = new ConfirmationDialog(i18n.get("caption.filter.delete.confirmbox"),
                i18n.get("message.delete.filter.confirm"), i18n.get("button.ok"), i18n.get("button.cancel"), ok -> {
                    if (ok) {
                        final Long rowId = (Long) ((Button) event.getComponent()).getData();
                        final String deletedFilterName = targetFilterQueryManagement.findTargetFilterQueryById(rowId)
                                .getName();
                        targetFilterQueryManagement.deleteTargetFilterQuery(rowId);

                        /*
                         * Refresh the custom filter table to show latest change
                         * of the deleted custom filter.
                         */

                        notification.displaySuccess(
                                i18n.get("message.delete.filter.success", new Object[] { deletedFilterName }));
                        refreshContainer();
                    }
                });
        UI.getCurrent().addWindow(confirmDialog.getWindow());
        confirmDialog.getWindow().bringToFront();
    }

    protected void addCustomGeneratedColumns() {
        addGeneratedColumn(SPUIDefinitions.CUSTOM_FILTER_DELETE,
                (source, itemId, columnId) -> getDeleteButton((Long) itemId));

        addGeneratedColumn(SPUILabelDefinitions.NAME,
                (source, itemId, columnId) -> customFilterDetailButton((Long) itemId));

    }

    private Button customFilterDetailButton(final Long itemId) {
        final Item row1 = getItem(itemId);
        final String tfName = (String) row1.getItemProperty(SPUILabelDefinitions.NAME).getValue();

        final Button updateIcon = SPUIComponentProvider.getButton(getDetailLinkId(tfName), tfName,
                SPUILabelDefinitions.UPDATE_CUSTOM_FILTER, null, false, null, SPUIButtonStyleSmallNoBorder.class);
        updateIcon.setData(tfName);
        updateIcon.addStyleName(ValoTheme.LINK_SMALL + " " + "on-focus-no-border link");
        updateIcon.addClickListener(this::onClickOfDetailButton);
        return updateIcon;
    }

    private void onClickOfDetailButton(final ClickEvent event) {
        final String targetFilterName = (String) ((Button) event.getComponent()).getData();
        final TargetFilterQuery targetFilterQuery = targetFilterQueryManagement
                .findTargetFilterQueryByName(targetFilterName);
        filterManagementUIState.setTfQuery(targetFilterQuery);
        filterManagementUIState.setFilterQueryValue(targetFilterQuery.getQuery());

        filterManagementUIState.setEditViewDisplayed(true);
        eventBus.publish(this, CustomFilterUIEvent.TARGET_FILTER_DETAIL_VIEW);
    }

    private void populateTableData() {
        container = createContainer();
        addContainerproperties();
        setContainerDataSource(container);
        setColumnProperties();

    }

    private static String getDetailLinkId(final String filterName) {
        return new StringBuilder(SPUIComponentIdProvider.CUSTOM_FILTER_DETAIL_LINK).append('.').append(filterName)
                .toString();
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

}
