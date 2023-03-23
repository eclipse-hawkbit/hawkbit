/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.filtermanagement;

import java.util.Collection;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement;
import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.builder.GridComponentBuilder;
import org.eclipse.hawkbit.ui.common.builder.StatusIconBuilder;
import org.eclipse.hawkbit.ui.common.builder.StatusIconBuilder.ActionTypeIconSupplier;
import org.eclipse.hawkbit.ui.common.data.mappers.TargetFilterQueryToProxyTargetFilterMapper;
import org.eclipse.hawkbit.ui.common.data.providers.TargetFilterQueryDataProvider;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyIdentifiableEntity;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTargetFilterQuery;
import org.eclipse.hawkbit.ui.common.event.CommandTopics;
import org.eclipse.hawkbit.ui.common.event.EntityModifiedEventPayload;
import org.eclipse.hawkbit.ui.common.event.EntityModifiedEventPayload.EntityModifiedEventType;
import org.eclipse.hawkbit.ui.common.event.EventTopics;
import org.eclipse.hawkbit.ui.common.event.EventView;
import org.eclipse.hawkbit.ui.common.event.FilterType;
import org.eclipse.hawkbit.ui.common.event.ShowFormEventPayload;
import org.eclipse.hawkbit.ui.common.event.ShowFormEventPayload.FormType;
import org.eclipse.hawkbit.ui.common.grid.AbstractGrid;
import org.eclipse.hawkbit.ui.common.grid.support.DeleteSupport;
import org.eclipse.hawkbit.ui.common.grid.support.FilterSupport;
import org.eclipse.hawkbit.ui.filtermanagement.state.TargetFilterGridLayoutUiState;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UIMessageIdProvider;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.utils.TenantConfigHelper;
import org.springframework.util.StringUtils;

import com.vaadin.data.ValueProvider;
import com.vaadin.server.Sizeable;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;

import static org.eclipse.hawkbit.ui.utils.UIComponentIdProvider.TARGET_FILTER_TABLE_CONFIRMATION_LABEL_ID;

/**
 * Concrete implementation of TargetFilter grid which is displayed on the
 * Filtermanagement View.
 */
public class TargetFilterGrid extends AbstractGrid<ProxyTargetFilterQuery, String> {
    private static final long serialVersionUID = 1L;

    private static final String FILTER_NAME_ID = "filterName";
    private static final String FILTER_DELETE_BUTTON_ID = "filterDeleteButton";

    private final UINotification notification;
    private final TargetFilterGridLayoutUiState uiState;
    private final transient TargetFilterQueryManagement targetFilterQueryManagement;
    private final transient TenantConfigHelper tenantConfigHelper;

    private final transient AutoAssignmentWindowBuilder autoAssignmentWindowBuilder;

    private final ActionTypeIconSupplier<ProxyTargetFilterQuery> actionTypeIconSupplier;
    private final StatusIconBuilder.ConfirmationIconSupplier confirmationIconSupplier;

    private final transient DeleteSupport<ProxyTargetFilterQuery> targetFilterDeleteSupport;

    /**
     * Constructor for TargetFilterGrid
     *
     * @param uiDependencies
     *            {@link CommonUiDependencies}
     * @param uiState
     *            TargetFilterGridLayoutUiState
     * @param targetFilterQueryManagement
     *            TargetFilterQueryManagement
     * @param autoAssignmentWindowBuilder
     *            AutoAssignmentWindowBuilder
     * @param tenantConfigHelper
     *            TenantConfigHelper
     */
    public TargetFilterGrid(
            final CommonUiDependencies uiDependencies, final TargetFilterGridLayoutUiState uiState,
            final TargetFilterQueryManagement targetFilterQueryManagement,
            final AutoAssignmentWindowBuilder autoAssignmentWindowBuilder,
            final TenantConfigHelper tenantConfigHelper) {
        super(uiDependencies.getI18n(), uiDependencies.getEventBus(), uiDependencies.getPermChecker());

        this.notification = uiDependencies.getUiNotification();
        this.uiState = uiState;
        this.targetFilterQueryManagement = targetFilterQueryManagement;
        this.autoAssignmentWindowBuilder = autoAssignmentWindowBuilder;
        this.tenantConfigHelper = tenantConfigHelper;

        this.targetFilterDeleteSupport = new DeleteSupport<>(this, i18n, notification, "caption.filter.custom",
                "caption.filter.custom.plur", ProxyTargetFilterQuery::getName, this::targetFiltersDeletionCallback,
                UIComponentIdProvider.TARGET_FILTER_DELETE_CONFIRMATION_DIALOG);

        setFilterSupport(new FilterSupport<>(new TargetFilterQueryDataProvider(targetFilterQueryManagement,
                new TargetFilterQueryToProxyTargetFilterMapper())));
        initFilterMappings();

        actionTypeIconSupplier = new ActionTypeIconSupplier<>(i18n, ProxyTargetFilterQuery::getAutoAssignActionType,
              UIComponentIdProvider.TARGET_FILTER_TABLE_TYPE_LABEL_ID);

        confirmationIconSupplier = new StatusIconBuilder.ConfirmationIconSupplier(i18n, 
              TARGET_FILTER_TABLE_CONFIRMATION_LABEL_ID);

        init();
    }

    private void initFilterMappings() {
        getFilterSupport().<String> addMapping(FilterType.SEARCH, (filter, searchText) -> setSearchFilter(searchText),
                uiState.getSearchFilterInput());
    }

    private void setSearchFilter(final String searchText) {
        getFilterSupport().setFilter(!StringUtils.isEmpty(searchText) ? String.format("%%%s%%", searchText) : null);
    }

    @Override
    public String getGridId() {
        return UIComponentIdProvider.TARGET_FILTER_TABLE_ID;
    }

    private boolean targetFiltersDeletionCallback(final Collection<ProxyTargetFilterQuery> targetFiltersToBeDeleted) {
        final Collection<Long> targetFilterIdsToBeDeleted = targetFiltersToBeDeleted.stream()
                .map(ProxyIdentifiableEntity::getId).collect(Collectors.toList());
        targetFilterIdsToBeDeleted.forEach(targetFilterQueryManagement::delete);

        eventBus.publish(EventTopics.ENTITY_MODIFIED, this, new EntityModifiedEventPayload(
                EntityModifiedEventType.ENTITY_REMOVED, ProxyTargetFilterQuery.class, targetFilterIdsToBeDeleted));

        return true;
    }

    @Override
    public void addColumns() {
        final Column<ProxyTargetFilterQuery, Button> nameColumn = GridComponentBuilder
                .addComponentColumn(this, this::buildFilterLink).setId(FILTER_NAME_ID)
                .setCaption(i18n.getMessage("header.name"));
        GridComponentBuilder.setColumnSortable(nameColumn, "name");


        GridComponentBuilder.addCreatedAndModifiedColumns(this, i18n);

        addAutoAssignmentColumns();

        GridComponentBuilder.addDeleteColumn(this, i18n, FILTER_DELETE_BUTTON_ID, targetFilterDeleteSupport,
                UIComponentIdProvider.CUSTOM_FILTER_DELETE_ICON, e -> permissionChecker.hasDeleteTargetPermission());

        getColumns().forEach(column -> column.setHidable(true));
    }

    private void addAutoAssignmentColumns() {
        final ValueProvider<ProxyTargetFilterQuery, HorizontalLayout> autoAssignmentProvider = filter -> {
            final HorizontalLayout horizontalLayout = new HorizontalLayout();
            horizontalLayout.setWidthUndefined();
            horizontalLayout.setDefaultComponentAlignment(Alignment.MIDDLE_LEFT);

            final Label icon = actionTypeIconSupplier.getLabel(filter);
            horizontalLayout.addComponent(icon);

            if (tenantConfigHelper.isConfirmationFlowEnabled()) {
                final Label confirmationIcon = confirmationIconSupplier.getLabel(filter);
                horizontalLayout.addComponent(confirmationIcon);
            }

            final Button link = buildAutoAssignmentLink(filter);
            horizontalLayout.addComponent(link);

            return horizontalLayout;
        };
        GridComponentBuilder.addComponentColumn(this, autoAssignmentProvider)
                .setCaption(i18n.getMessage("header.auto.assignment.ds")).setWidthUndefined();
    }

    private Button buildFilterLink(final ProxyTargetFilterQuery targetFilter) {
        final String caption = targetFilter.getName();
        final String description = i18n.getMessage(UIMessageIdProvider.TOOLTIP_UPDATE_CUSTOM_FILTER);
        final Button link = GridComponentBuilder.buildLink(targetFilter,
                UIComponentIdProvider.CUSTOM_FILTER_DETAIL_LINK, caption, true,
                clickEvent -> onClickOfFilterName(targetFilter));
        link.setDescription(description);
        return link;
    }

    private void onClickOfFilterName(final ProxyTargetFilterQuery targetFilter) {
        eventBus.publish(CommandTopics.SHOW_ENTITY_FORM_LAYOUT, this,
                new ShowFormEventPayload<ProxyTargetFilterQuery>(FormType.EDIT, targetFilter, EventView.TARGET_FILTER));
    }

    private Button buildAutoAssignmentLink(final ProxyTargetFilterQuery targetFilter) {
        final String caption;
        if (targetFilter.isAutoAssignmentEnabled() && targetFilter.getDistributionSetInfo() != null) {
            caption = targetFilter.getDistributionSetInfo().getNameVersion();
        } else {
            caption = i18n.getMessage(UIMessageIdProvider.BUTTON_NO_AUTO_ASSIGNMENT);
        }

        final Button link = GridComponentBuilder.buildLink(targetFilter, "distSetButton", caption,
                permissionChecker.hasAutoAssignmentUpdatePermission(),
                clickEvent -> onClickOfAutoAssignmentLink(targetFilter));

        final String description = i18n.getMessage(UIMessageIdProvider.BUTTON_AUTO_ASSIGNMENT_DESCRIPTION);
        link.setDescription(description);

        return link;
    }

    private void onClickOfAutoAssignmentLink(final ProxyTargetFilterQuery targetFilter) {
        if (permissionChecker.hasReadRepositoryPermission()) {
            final Window autoAssignmentWindow = autoAssignmentWindowBuilder.getWindowForAutoAssignment(targetFilter);

            autoAssignmentWindow.setCaption(i18n.getMessage(UIMessageIdProvider.CAPTION_SELECT_AUTO_ASSIGN_DS));
            autoAssignmentWindow.setWidth(40.0F, Sizeable.Unit.PERCENTAGE);

            UI.getCurrent().addWindow(autoAssignmentWindow);
            autoAssignmentWindow.setVisible(Boolean.TRUE);
        } else {
            notification.displayValidationError(i18n.getMessage(
                    UIMessageIdProvider.MESSAGE_ERROR_PERMISSION_INSUFFICIENT, SpPermission.READ_REPOSITORY));
        }
    }
}
