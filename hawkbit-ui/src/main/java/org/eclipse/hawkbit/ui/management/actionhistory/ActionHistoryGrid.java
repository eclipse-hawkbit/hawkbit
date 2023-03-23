/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.actionhistory;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Optional;

import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.exception.CancelActionNotAllowedException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.ConfirmationDialog;
import org.eclipse.hawkbit.ui.common.builder.GridComponentBuilder;
import org.eclipse.hawkbit.ui.common.builder.StatusIconBuilder.ActionStatusIconSupplier;
import org.eclipse.hawkbit.ui.common.builder.StatusIconBuilder.ActionTypeIconSupplier;
import org.eclipse.hawkbit.ui.common.builder.StatusIconBuilder.ActiveStatusIconSupplier;
import org.eclipse.hawkbit.ui.common.builder.StatusIconBuilder.TimeforcedIconSupplier;
import org.eclipse.hawkbit.ui.common.data.mappers.ActionToProxyActionMapper;
import org.eclipse.hawkbit.ui.common.data.providers.ActionDataProvider;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyAction;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTarget;
import org.eclipse.hawkbit.ui.common.event.EntityModifiedEventPayload;
import org.eclipse.hawkbit.ui.common.event.EntityModifiedEventPayload.EntityModifiedEventType;
import org.eclipse.hawkbit.ui.common.event.EventLayout;
import org.eclipse.hawkbit.ui.common.event.EventTopics;
import org.eclipse.hawkbit.ui.common.event.EventView;
import org.eclipse.hawkbit.ui.common.event.FilterType;
import org.eclipse.hawkbit.ui.common.grid.AbstractGrid;
import org.eclipse.hawkbit.ui.common.grid.support.FilterSupport;
import org.eclipse.hawkbit.ui.common.grid.support.MasterEntitySupport;
import org.eclipse.hawkbit.ui.common.grid.support.SelectionSupport;
import org.eclipse.hawkbit.ui.utils.SPDateTimeUtil;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UIMessageIdProvider;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.data.ValueProvider;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.Button;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;

/**
 * This grid presents the action history for a selected target.
 */
public class ActionHistoryGrid extends AbstractGrid<ProxyAction, String> {
    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(ActionHistoryGrid.class);

    private static final String ACTION_ID = "id";
    private static final String DS_NAME_VERSION_ID = "dsNameVersion";
    private static final String ROLLOUT_NAME_ID = "rolloutName";
    private static final String MAINTENANCE_WINDOW_ID = "maintenanceWindow";
    private static final String LAST_MODIFIED_AT_ID = "lastModifiedAt";
    private static final String STATUS_ID = "status";
    private static final String ACTIVE_STATUS_ID = "isActiveDecoration";
    private static final String TYPE_ID = "type";
    private static final String TIME_FORCED_ID = "timeForced";

    private static final String CANCEL_BUTTON_ID = "cancel-action";
    private static final String FORCE_BUTTON_ID = "force-action";
    private static final String FORCE_QUIT_BUTTON_ID = "force-quit-action";

    private final UINotification notification;
    private final transient DeploymentManagement deploymentManagement;
    private final transient ActionToProxyActionMapper actionToProxyActionMapper;

    private final ActionStatusIconSupplier<ProxyAction> actionStatusIconSupplier;
    private final ActiveStatusIconSupplier<ProxyAction> activeStatusIconSupplier;
    private final ActionTypeIconSupplier<ProxyAction> actionTypeIconSupplier;
    private final TimeforcedIconSupplier timeforcedIconSupplier;

    private final transient MasterEntitySupport<ProxyTarget> masterEntitySupport;

    public ActionHistoryGrid(final CommonUiDependencies uiDependencies, final DeploymentManagement deploymentManagement,
            final ActionHistoryGridLayoutUiState actionHistoryGridLayoutUiState) {
        super(uiDependencies.getI18n(), uiDependencies.getEventBus(), uiDependencies.getPermChecker());

        this.notification = uiDependencies.getUiNotification();
        this.deploymentManagement = deploymentManagement;
        this.actionToProxyActionMapper = new ActionToProxyActionMapper();

        // currently we do not restore action history selection
        setSelectionSupport(new SelectionSupport<>(this, eventBus, EventLayout.ACTION_HISTORY_LIST,
                EventView.DEPLOYMENT, this::mapIdToProxyEntity, null, null));
        if (actionHistoryGridLayoutUiState.isMaximized()) {
            getSelectionSupport().enableSingleSelection();
        } else {
            getSelectionSupport().disableSelection();
        }

        setFilterSupport(new FilterSupport<>(new ActionDataProvider(deploymentManagement, actionToProxyActionMapper)));
        initFilterMappings();

        this.masterEntitySupport = new MasterEntitySupport<>(getFilterSupport(), ProxyTarget::getControllerId);

        actionStatusIconSupplier = new ActionStatusIconSupplier<>(i18n, ProxyAction::getStatus,
                UIComponentIdProvider.ACTION_HISTORY_TABLE_STATUS_LABEL_ID);
        activeStatusIconSupplier = new ActiveStatusIconSupplier<>(i18n, ProxyAction::getIsActiveDecoration,
                UIComponentIdProvider.ACTION_HISTORY_TABLE_ACTIVESTATE_LABEL_ID);
        actionTypeIconSupplier = new ActionTypeIconSupplier<>(i18n, ProxyAction::getActionType,
                UIComponentIdProvider.ACTION_HISTORY_TABLE_TYPE_LABEL_ID);
        timeforcedIconSupplier = new TimeforcedIconSupplier(i18n,
                UIComponentIdProvider.ACTION_HISTORY_TABLE_TIMEFORCED_LABEL_ID);
        init();
    }

    /**
     * Map entity id to proxy entity
     *
     * @param entityId
     *            Entity id
     *
     * @return Proxy action
     */
    public Optional<ProxyAction> mapIdToProxyEntity(final long entityId) {
        return deploymentManagement.findAction(entityId).map(actionToProxyActionMapper::map);
    }

    private void initFilterMappings() {
        getFilterSupport().<String> addMapping(FilterType.MASTER,
                (filter, masterFilter) -> getFilterSupport().setFilter(masterFilter));
    }

    @Override
    public String getGridId() {
        return UIComponentIdProvider.ACTION_HISTORY_GRID_ID;
    }

    /**
     * Creates the grid content for maximized-state.
     */
    @Override
    public void createMaximizedContent() {
        createMaximizedContent(SelectionMode.SINGLE);
    }

    /**
     * Creates the grid content for normal (minimized) state.
     */
    @Override
    public void createMinimizedContent() {
        createMinimizedContent(SelectionMode.NONE);
    }

    @Override
    public void addColumns() {
        addActiveStatusColumn().setHidable(true);

        addDsColumn().setHidable(true);

        addDateAndTimeColumn().setHidable(true);

        addStatusColumn().setHidable(true);

        addMaintenanceWindowColumn().setHidable(true).setHidden(true);

        GridComponentBuilder.joinToIconColumn(getDefaultHeaderRow(), i18n.getMessage("label.action.type"),
                Arrays.asList(addTypeColumn(), addTimeforcedColumn()));

        GridComponentBuilder.joinToActionColumn(i18n, getDefaultHeaderRow(),
                Arrays.asList(addCancelColumn(), addForceColumn(), addForceQuitColumn()));
    }

    private Column<ProxyAction, Label> addActiveStatusColumn() {
        return GridComponentBuilder.addIconColumn(this, activeStatusIconSupplier::getLabel, ACTIVE_STATUS_ID,
                i18n.getMessage("label.active"));
    }

    private Column<ProxyAction, String> addDsColumn() {
        return GridComponentBuilder.addColumn(this, ProxyAction::getDsNameVersion).setId(DS_NAME_VERSION_ID)
                .setCaption(i18n.getMessage("distribution.details.header"));
    }

    private Column<ProxyAction, String> addDateAndTimeColumn() {
        return GridComponentBuilder
                .addColumn(this,
                        action -> SPDateTimeUtil.getFormattedDate(action.getLastModifiedAt(),
                                SPUIDefinitions.LAST_QUERY_DATE_FORMAT_SHORT))
                .setId(LAST_MODIFIED_AT_ID).setCaption(i18n.getMessage("header.rolloutgroup.target.date"))
                .setDescriptionGenerator(action -> SPDateTimeUtil.getFormattedDate(action.getLastModifiedAt()));
    }

    private Column<ProxyAction, Label> addStatusColumn() {
        return GridComponentBuilder.addIconColumn(this, actionStatusIconSupplier::getLabel, STATUS_ID,
                i18n.getMessage("header.status"));
    }

    private Column<ProxyAction, String> addMaintenanceWindowColumn() {
        return GridComponentBuilder.addColumn(this, ProxyAction::getMaintenanceWindow).setId(MAINTENANCE_WINDOW_ID)
                .setCaption(i18n.getMessage("header.maintenancewindow")).setDescriptionGenerator(
                        action -> getFormattedNextMaintenanceWindow(action.getMaintenanceWindowStartTime()));
    }

    private String getFormattedNextMaintenanceWindow(final ZonedDateTime nextAt) {
        if (nextAt == null) {
            return "";
        }

        final long nextAtMilli = nextAt.toInstant().toEpochMilli();
        return i18n.getMessage(UIMessageIdProvider.TOOLTIP_NEXT_MAINTENANCE_WINDOW,
                SPDateTimeUtil.getFormattedDate(nextAtMilli, SPUIDefinitions.LAST_QUERY_DATE_FORMAT_SHORT));
    }

    private Column<ProxyAction, Label> addTypeColumn() {
        return GridComponentBuilder.addIconColumn(this, actionTypeIconSupplier::getLabel, TYPE_ID, null);
    }

    private Column<ProxyAction, Label> addTimeforcedColumn() {
        return GridComponentBuilder.addIconColumn(this, timeforcedIconSupplier::getLabel, TIME_FORCED_ID, null);
    }

    private Column<ProxyAction, Button> addCancelColumn() {
        final ValueProvider<ProxyAction, Button> buttonProvider = action -> GridComponentBuilder.buildActionButton(i18n,
                clickEvent -> confirmAndCancelAction(action.getId()), VaadinIcons.CLOSE_SMALL, "message.cancel.action",
                SPUIStyleDefinitions.STATUS_ICON_NEUTRAL,
                UIComponentIdProvider.ACTION_HISTORY_TABLE_CANCEL_ID + "." + action.getId(),
                action.isActive() && !action.isCancelingOrCanceled() && permissionChecker.hasUpdateTargetPermission());
        return GridComponentBuilder.addIconColumn(this, buttonProvider, CANCEL_BUTTON_ID, null);
    }

    /**
     * Show confirmation window and if ok then only, cancel the action.
     *
     * @param actionId
     *            as Id if the action needs to be cancelled.
     */
    private void confirmAndCancelAction(final Long actionId) {
        final ConfirmationDialog confirmDialog = ConfirmationDialog
                .newBuilder(i18n, UIComponentIdProvider.CONFIRMATION_POPUP_ID)
                .caption(i18n.getMessage("caption.cancel.action.confirmbox"))
                .question(i18n.getMessage("message.cancel.action.confirm"))
                .onSaveOrUpdate(() -> cancelActiveAction(actionId)).build();
        UI.getCurrent().addWindow(confirmDialog.getWindow());
        confirmDialog.getWindow().bringToFront();
    }

    private void cancelActiveAction(final Long actionId) {
        try {
            deploymentManagement.cancelAction(actionId);

            notification.displaySuccess(i18n.getMessage("message.cancel.action.success"));
            publishEntityModifiedEvent(actionId);
        } catch (final CancelActionNotAllowedException e) {
            LOG.trace("Cancel action not allowed exception: {}", e.getMessage());
            notification.displayValidationError(i18n.getMessage("message.cancel.action.failed"));
        }
    }

    private void publishEntityModifiedEvent(final Long actionId) {
        if (masterEntitySupport.getMasterId() != null) {
            eventBus.publish(EventTopics.ENTITY_MODIFIED, this,
                    new EntityModifiedEventPayload(EntityModifiedEventType.ENTITY_UPDATED, ProxyTarget.class,
                            masterEntitySupport.getMasterId(), ProxyAction.class, actionId));
        }
    }

    private Column<ProxyAction, Button> addForceColumn() {
        final ValueProvider<ProxyAction, Button> buttonProvider = action -> GridComponentBuilder.buildActionButton(i18n,
                clickEvent -> confirmAndForceAction(action.getId()), VaadinIcons.BOLT, "message.force.action",
                SPUIStyleDefinitions.STATUS_ICON_NEUTRAL,
                UIComponentIdProvider.ACTION_HISTORY_TABLE_FORCE_ID + "." + action.getId(),
                action.isActive() && !action.isForce() && !action.isCancelingOrCanceled()
                        && permissionChecker.hasUpdateTargetPermission());
        return GridComponentBuilder.addIconColumn(this, buttonProvider, FORCE_BUTTON_ID, null);
    }

    /**
     * Show confirmation window and if ok then only, force the action.
     *
     * @param actionId
     *            as Id if the action needs to be forced.
     */
    private void confirmAndForceAction(final Long actionId) {
        final ConfirmationDialog confirmDialog = ConfirmationDialog
                .newBuilder(i18n, UIComponentIdProvider.CONFIRMATION_POPUP_ID)
                .caption(i18n.getMessage("caption.force.action.confirmbox"))
                .question(i18n.getMessage("message.force.action.confirm"))
                .onSaveOrUpdate(() -> forceActiveAction(actionId)).build();
        UI.getCurrent().addWindow(confirmDialog.getWindow());

        confirmDialog.getWindow().bringToFront();
    }

    private void forceActiveAction(final Long actionId) {
        try {
            deploymentManagement.forceTargetAction(actionId);

            notification.displaySuccess(i18n.getMessage("message.force.action.success"));
            publishEntityModifiedEvent(actionId);
        } catch (final EntityNotFoundException e) {
            LOG.trace("Action was not found during force command: {}", e.getMessage());
            notification.displayValidationError(i18n.getMessage("message.force.action.failed"));
        }
    }

    private Column<ProxyAction, Button> addForceQuitColumn() {
        final ValueProvider<ProxyAction, Button> buttonProvider = action -> GridComponentBuilder.buildActionButton(i18n,
                clickEvent -> confirmAndForceQuitAction(action.getId()), VaadinIcons.CLOSE_SMALL,
                "message.forcequit.action", SPUIStyleDefinitions.STATUS_ICON_RED,
                UIComponentIdProvider.ACTION_HISTORY_TABLE_FORCE_QUIT_ID + "." + action.getId(),
                action.isActive() && action.isCancelingOrCanceled() && permissionChecker.hasUpdateTargetPermission());
        return GridComponentBuilder.addIconColumn(this, buttonProvider, FORCE_QUIT_BUTTON_ID, null);
    }

    /**
     * Show confirmation window and if ok then only, force quit action.
     *
     * @param actionId
     *            as Id if the action needs to be forced.
     */
    private void confirmAndForceQuitAction(final Long actionId) {
        final ConfirmationDialog confirmDialog = ConfirmationDialog
                .newBuilder(i18n, UIComponentIdProvider.CONFIRMATION_POPUP_ID)
                .caption(i18n.getMessage("caption.forcequit.action.confirmbox"))
                .question(i18n.getMessage("message.forcequit.action.confirm")).icon(VaadinIcons.WARNING)
                .onSaveOrUpdate(() -> forceQuitActiveAction(actionId)).build();
        UI.getCurrent().addWindow(confirmDialog.getWindow());

        confirmDialog.getWindow().bringToFront();
    }

    private void forceQuitActiveAction(final Long actionId) {
        try {
            deploymentManagement.forceQuitAction(actionId);

            notification.displaySuccess(i18n.getMessage("message.forcequit.action.success"));
            publishEntityModifiedEvent(actionId);
        } catch (final CancelActionNotAllowedException e) {
            LOG.trace("Force Cancel action not allowed exception: {}", e.getMessage());
            notification.displayValidationError(i18n.getMessage("message.forcequit.action.failed"));
        }
    }

    @Override
    protected void addMaxColumns() {
        addActiveStatusColumn().setHidable(true);

        addActionIdColumn().setExpandRatio(0).setHidable(true);

        addDsColumn().setHidable(true);

        addDateAndTimeColumn().setHidable(true);

        addStatusColumn().setHidable(true);

        addMaintenanceWindowColumn().setHidable(true).setHidden(true);

        addRolloutNameColumn().setHidable(true);

        GridComponentBuilder.joinToIconColumn(getDefaultHeaderRow(), i18n.getMessage("label.action.type"),
                Arrays.asList(addTypeColumn(), addTimeforcedColumn()));

        GridComponentBuilder.joinToActionColumn(i18n, getDefaultHeaderRow(),
                Arrays.asList(addCancelColumn(), addForceColumn(), addForceQuitColumn()));
    }

    private Column<ProxyAction, Long> addActionIdColumn() {
        return GridComponentBuilder.addColumn(this, ProxyAction::getId).setId(ACTION_ID)
                .setCaption(i18n.getMessage("label.action.id")).setMinimumWidthFromContent(true);
    }

    private Column<ProxyAction, String> addRolloutNameColumn() {
        return GridComponentBuilder.addColumn(this, ProxyAction::getRolloutName).setId(ROLLOUT_NAME_ID)
                .setCaption(i18n.getMessage("caption.rollout.name"));
    }

    /**
     * Gets the master entity support
     *
     * @return Master entity support
     */
    public MasterEntitySupport<ProxyTarget> getMasterEntitySupport() {
        return masterEntitySupport;
    }
}
