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

import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.common.ConfirmationDialog;
import org.eclipse.hawkbit.ui.components.ProxyDistribution;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleSmallNoBorder;
import org.eclipse.hawkbit.ui.distributions.state.ManageDistUIState;
import org.eclipse.hawkbit.ui.filtermanagement.event.CustomFilterUIEvent;
import org.eclipse.hawkbit.ui.filtermanagement.state.FilterManagementUIState;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.eclipse.hawkbit.ui.utils.TableColumn;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.addons.lazyquerycontainer.BeanQueryFactory;
import org.vaadin.addons.lazyquerycontainer.LazyQueryContainer;
import org.vaadin.addons.lazyquerycontainer.LazyQueryDefinition;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.EventBus.UIEventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.google.common.collect.Maps;
import com.vaadin.data.Container;
import com.vaadin.data.Item;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Link;
import com.vaadin.ui.Table;
import com.vaadin.ui.UI;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Displays list of target filter queries
 *
 */
public class TargetFilterTable extends Table {

    private static final long serialVersionUID = -4307487829435474759L;

    private final VaadinMessageSource i18n;

    private final UINotification notification;

    private final transient EventBus.UIEventBus eventBus;

    private final FilterManagementUIState filterManagementUIState;

    private final transient TargetFilterQueryManagement targetFilterQueryManagement;

    private final DistributionSetSelectWindow dsSelectWindow;

    private final SpPermissionChecker permChecker;

    private Container container;

    private static final int PROPERTY_DEPT = 3;

    public TargetFilterTable(final VaadinMessageSource i18n, final UINotification notification,
            final UIEventBus eventBus, final FilterManagementUIState filterManagementUIState,
            final TargetFilterQueryManagement targetFilterQueryManagement, final ManageDistUIState manageDistUIState,
            final TargetManagement targetManagement, final SpPermissionChecker permChecker) {
        this.i18n = i18n;
        this.notification = notification;
        this.eventBus = eventBus;
        this.filterManagementUIState = filterManagementUIState;
        this.targetFilterQueryManagement = targetFilterQueryManagement;
        this.permChecker = permChecker;

        this.dsSelectWindow = new DistributionSetSelectWindow(i18n, eventBus, targetManagement,
                targetFilterQueryManagement, manageDistUIState);

        setStyleName("sp-table");
        setSizeFull();
        setImmediate(true);
        setHeight(100.0F, Unit.PERCENTAGE);
        addStyleName(ValoTheme.TABLE_NO_VERTICAL_LINES);
        addStyleName(ValoTheme.TABLE_SMALL);
        addCustomGeneratedColumns();
        populateTableData();
        setColumnCollapsingAllowed(true);
        setColumnProperties();
        setId(UIComponentIdProvider.TARGET_FILTER_TABLE_ID);
        eventBus.subscribe(this);
    }

    @EventBusListenerMethod(scope = EventScope.UI)
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
        final Map<String, Object> queryConfig = Maps.newHashMapWithExpectedSize(1);
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
        container.addContainerProperty(SPUILabelDefinitions.AUTO_ASSIGN_DISTRIBUTION_SET, String.class, null);
    }

    private List<TableColumn> getVisbleColumns() {
        final List<TableColumn> columnList = new ArrayList<>(7);
        columnList.add(new TableColumn(SPUILabelDefinitions.NAME, i18n.getMessage("header.name"), 0.2F));
        columnList
                .add(new TableColumn(SPUILabelDefinitions.VAR_CREATED_USER, i18n.getMessage("header.createdBy"), 0.1F));
        columnList.add(
                new TableColumn(SPUILabelDefinitions.VAR_CREATED_DATE, i18n.getMessage("header.createdDate"), 0.2F));
        columnList
                .add(new TableColumn(SPUILabelDefinitions.VAR_MODIFIED_BY, i18n.getMessage("header.modifiedBy"), 0.1F));
        columnList.add(
                new TableColumn(SPUILabelDefinitions.VAR_MODIFIED_DATE, i18n.getMessage("header.modifiedDate"), 0.2F));
        columnList.add(new TableColumn(SPUILabelDefinitions.AUTO_ASSIGN_DISTRIBUTION_SET,
                i18n.getMessage("header.auto.assignment.ds"), 0.1F));
        columnList.add(new TableColumn(SPUIDefinitions.CUSTOM_FILTER_DELETE, i18n.getMessage("header.delete"), 0.1F));
        return columnList;

    }

    private void refreshContainer() {
        populateTableData();

    }

    private Button getDeleteButton(final Long itemId) {
        final Item row = getItem(itemId);
        final String tfName = (String) row.getItemProperty(SPUILabelDefinitions.NAME).getValue();
        final Button deleteIcon = SPUIComponentProvider.getButton(getDeleteIconId(tfName), "",
                SPUILabelDefinitions.DELETE_CUSTOM_FILTER, ValoTheme.BUTTON_TINY + " " + "blueicon", true,
                FontAwesome.TRASH_O, SPUIButtonStyleSmallNoBorder.class);
        deleteIcon.setData(itemId);
        deleteIcon.addClickListener(this::onDelete);
        return deleteIcon;
    }

    private static String getDeleteIconId(final String targetFilterName) {
        return new StringBuilder(UIComponentIdProvider.CUSTOM_FILTER_DELETE_ICON).append('.').append(targetFilterName)
                .toString();
    }

    private void onDelete(final ClickEvent event) {
        /* Display the confirmation */
        final ConfirmationDialog confirmDialog = new ConfirmationDialog(
                i18n.getMessage("caption.filter.delete.confirmbox"), i18n.getMessage("message.delete.filter.confirm"),
                i18n.getMessage("button.ok"), i18n.getMessage("button.cancel"), ok -> {
                    if (ok) {
                        final Long rowId = (Long) ((Button) event.getComponent()).getData();
                        final String deletedFilterName = targetFilterQueryManagement.get(rowId).get().getName();
                        targetFilterQueryManagement.delete(rowId);

                        /*
                         * Refresh the custom filter table to show latest change
                         * of the deleted custom filter.
                         */

                        notification.displaySuccess(
                                i18n.getMessage("message.delete.filter.success", new Object[] { deletedFilterName }));
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

        addGeneratedColumn(SPUILabelDefinitions.AUTO_ASSIGN_DISTRIBUTION_SET,
                (source, itemId, columnId) -> customFilterDistributionSetButton((Long) itemId));

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

    private Button customFilterDistributionSetButton(final Long itemId) {
        final Item row1 = getItem(itemId);
        final ProxyDistribution distSet = (ProxyDistribution) row1
                .getItemProperty(SPUILabelDefinitions.AUTO_ASSIGN_DISTRIBUTION_SET).getValue();
        final String buttonId = "distSetButton";
        Button updateIcon;
        if (distSet == null) {
            updateIcon = SPUIComponentProvider.getButton(buttonId, i18n.getMessage("button.no.auto.assignment"),
                    i18n.getMessage("button.auto.assignment.desc"), null, false, null,
                    SPUIButtonStyleSmallNoBorder.class);
        } else {
            updateIcon = SPUIComponentProvider.getButton(buttonId, distSet.getNameVersion(),
                    i18n.getMessage("button.auto.assignment.desc"), null, false, null,
                    SPUIButtonStyleSmallNoBorder.class);
        }

        updateIcon.addClickListener(this::onClickOfDistributionSetButton);
        updateIcon.setData(row1);
        updateIcon.addStyleName(ValoTheme.LINK_SMALL + " " + "on-focus-no-border link");

        return updateIcon;
    }

    private void onClickOfDistributionSetButton(final ClickEvent event) {
        final Item item = (Item) ((Button) event.getComponent()).getData();
        final Long tfqId = (Long) item.getItemProperty(SPUILabelDefinitions.VAR_ID).getValue();

        if (permChecker.hasReadRepositoryPermission()) {
            dsSelectWindow.showForTargetFilter(tfqId);
        } else {
            notification.displayValidationError(
                    i18n.getMessage("message.permission.insufficient", SpPermission.READ_REPOSITORY));
        }

    }

    private void onClickOfDetailButton(final ClickEvent event) {
        final String targetFilterName = (String) ((Button) event.getComponent()).getData();
        targetFilterQueryManagement.getByName(targetFilterName).ifPresent(targetFilterQuery -> {
            filterManagementUIState.setFilterQueryValue(targetFilterQuery.getQuery());
            filterManagementUIState.setTfQuery(targetFilterQuery);
            filterManagementUIState.setEditViewDisplayed(true);
            eventBus.publish(this, CustomFilterUIEvent.TARGET_FILTER_DETAIL_VIEW);
        });
    }

    private void populateTableData() {
        container = createContainer();
        addContainerproperties();
        setContainerDataSource(container);
        setColumnProperties();

    }

    private static String getDetailLinkId(final String filterName) {
        return new StringBuilder(UIComponentIdProvider.CUSTOM_FILTER_DETAIL_LINK).append('.').append(filterName)
                .toString();
    }

    private void setColumnProperties() {
        setVisibleColumns(getVisbleColumns().stream().map(column -> {
            setColumnHeader(column.getColumnPropertyId(), column.getColumnHeader());
            setColumnExpandRatio(column.getColumnPropertyId(), column.getExpandRatio());
            return column.getColumnPropertyId();
        }).toArray());
    }

}
