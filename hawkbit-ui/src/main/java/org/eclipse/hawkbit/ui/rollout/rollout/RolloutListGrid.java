/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.rollout.rollout;

import static org.eclipse.hawkbit.ui.rollout.DistributionBarHelper.getTooltip;
import static org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil.HTML_LI_CLOSE_TAG;
import static org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil.HTML_LI_OPEN_TAG;
import static org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil.HTML_UL_CLOSE_TAG;
import static org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil.HTML_UL_OPEN_TAG;
import static org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions.ACTION;
import static org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions.VAR_CREATED_DATE;
import static org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions.VAR_CREATED_USER;
import static org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions.VAR_DESC;
import static org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions.VAR_DIST_NAME_VERSION;
import static org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions.VAR_ID;
import static org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions.VAR_MODIFIED_BY;
import static org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions.VAR_MODIFIED_DATE;
import static org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions.VAR_NAME;
import static org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions.VAR_NUMBER_OF_GROUPS;
import static org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions.VAR_STATUS;
import static org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions.VAR_TOTAL_TARGETS;
import static org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions.VAR_TOTAL_TARGETS_COUNT_STATUS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.RolloutManagement;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.Rollout.RolloutStatus;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.TotalTargetCountStatus;
import org.eclipse.hawkbit.repository.model.TotalTargetCountStatus.Status;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.UiProperties;
import org.eclipse.hawkbit.ui.common.CommonDialogWindow;
import org.eclipse.hawkbit.ui.common.ConfirmationDialog;
import org.eclipse.hawkbit.ui.common.grid.AbstractGrid;
import org.eclipse.hawkbit.ui.customrenderers.client.renderers.RolloutRendererData;
import org.eclipse.hawkbit.ui.customrenderers.renderers.HtmlButtonRenderer;
import org.eclipse.hawkbit.ui.customrenderers.renderers.HtmlLabelRenderer;
import org.eclipse.hawkbit.ui.customrenderers.renderers.RolloutRenderer;
import org.eclipse.hawkbit.ui.push.RolloutChangeEventContainer;
import org.eclipse.hawkbit.ui.push.RolloutDeleteEventContainer;
import org.eclipse.hawkbit.ui.push.event.RolloutChangeEvent;
import org.eclipse.hawkbit.ui.push.event.RolloutDeleteEvent;
import org.eclipse.hawkbit.ui.rollout.DistributionBarHelper;
import org.eclipse.hawkbit.ui.rollout.StatusFontIcon;
import org.eclipse.hawkbit.ui.rollout.event.RolloutEvent;
import org.eclipse.hawkbit.ui.rollout.state.RolloutUIState;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.vaadin.addons.lazyquerycontainer.BeanQueryFactory;
import org.vaadin.addons.lazyquerycontainer.LazyQueryContainer;
import org.vaadin.addons.lazyquerycontainer.LazyQueryDefinition;
import org.vaadin.spring.events.EventBus.UIEventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.google.common.collect.Maps;
import com.vaadin.data.Container;
import com.vaadin.data.Container.Indexed;
import com.vaadin.data.Item;
import com.vaadin.data.util.converter.Converter;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.UI;
import com.vaadin.ui.renderers.ClickableRenderer.RendererClickEvent;
import com.vaadin.ui.renderers.HtmlRenderer;

/**
 * Rollout list grid component.
 */
public class RolloutListGrid extends AbstractGrid {

    private static final long serialVersionUID = 4060904914954370524L;

    private static final String UPDATE_OPTION = "Update";

    private static final String COPY_OPTION = "Copy";

    private static final String PAUSE_OPTION = "Pause";

    private static final String RUN_OPTION = "Run";

    private static final String DELETE_OPTION = "Delete";

    private static final String DS_TYPE = "type";

    private static final String SW_MODULES = "swModules";

    private static final String IS_REQUIRED_MIGRATION_STEP = "isRequiredMigrationStep";

    private static final String ROLLOUT_RENDERER_DATA = "rolloutRendererData";

    private final transient RolloutManagement rolloutManagement;

    private final AddUpdateRolloutWindowLayout addUpdateRolloutWindow;

    private final UINotification uiNotification;

    private final RolloutUIState rolloutUIState;

    private static final Map<RolloutStatus, StatusFontIcon> statusIconMap = new EnumMap<>(RolloutStatus.class);

    static {
        statusIconMap.put(RolloutStatus.FINISHED,
                new StatusFontIcon(FontAwesome.CHECK_CIRCLE, SPUIStyleDefinitions.STATUS_ICON_GREEN));
        statusIconMap.put(RolloutStatus.PAUSED,
                new StatusFontIcon(FontAwesome.PAUSE, SPUIStyleDefinitions.STATUS_ICON_BLUE));
        statusIconMap.put(RolloutStatus.RUNNING, new StatusFontIcon(null, SPUIStyleDefinitions.STATUS_SPINNER_YELLOW));
        statusIconMap.put(RolloutStatus.READY,
                new StatusFontIcon(FontAwesome.DOT_CIRCLE_O, SPUIStyleDefinitions.STATUS_ICON_LIGHT_BLUE));
        statusIconMap.put(RolloutStatus.STOPPED,
                new StatusFontIcon(FontAwesome.STOP, SPUIStyleDefinitions.STATUS_ICON_RED));
        statusIconMap.put(RolloutStatus.CREATING, new StatusFontIcon(null, SPUIStyleDefinitions.STATUS_SPINNER_GREY));
        statusIconMap.put(RolloutStatus.STARTING, new StatusFontIcon(null, SPUIStyleDefinitions.STATUS_SPINNER_BLUE));
        statusIconMap.put(RolloutStatus.ERROR_CREATING,
                new StatusFontIcon(FontAwesome.EXCLAMATION_CIRCLE, SPUIStyleDefinitions.STATUS_ICON_RED));
        statusIconMap.put(RolloutStatus.ERROR_STARTING,
                new StatusFontIcon(FontAwesome.EXCLAMATION_CIRCLE, SPUIStyleDefinitions.STATUS_ICON_RED));
        statusIconMap.put(RolloutStatus.DELETING, new StatusFontIcon(null, SPUIStyleDefinitions.STATUS_SPINNER_RED));
    }

    RolloutListGrid(final I18N i18n, final UIEventBus eventBus, final RolloutManagement rolloutManagement,
            final UINotification uiNotification, final RolloutUIState rolloutUIState,
            final SpPermissionChecker permissionChecker, final TargetManagement targetManagement,
            final EntityFactory entityFactory, final UiProperties uiProperties,
            final TargetFilterQueryManagement targetFilterQueryManagement) {
        super(i18n, eventBus, permissionChecker);
        this.rolloutManagement = rolloutManagement;
        this.addUpdateRolloutWindow = new AddUpdateRolloutWindowLayout(rolloutManagement, targetManagement,
                uiNotification, uiProperties, entityFactory, i18n, eventBus, targetFilterQueryManagement);
        this.uiNotification = uiNotification;
        this.rolloutUIState = rolloutUIState;
        handleNoData(rolloutUIState);
    }

    @Override
    protected void handleNoData(final RolloutUIState rolloutUIState) {
        int size = 0;
        final Indexed container = getContainerDataSource();
        if (container != null) {
            size = container.size();
        }
        if (size == 0 && rolloutUIState.isShowRollOuts()) {
            setData(SPUIDefinitions.NO_DATA);
        }
    }

    /**
     * Handles the RolloutEvent to refresh Grid.
     *
     */
    @EventBusListenerMethod(scope = EventScope.UI)
    void onEvent(final RolloutEvent event) {
        switch (event) {
        case FILTER_BY_TEXT:
        case CREATE_ROLLOUT:
        case UPDATE_ROLLOUT:
        case SHOW_ROLLOUTS:
            refreshContainer();
            break;
        default:
            return;
        }
    }

    /**
     * Handles the RolloutDeleteEvent to refresh the grid.
     *
     * @param eventContainer
     *            container which holds the rollout delete event
     */
    @SuppressWarnings("unchecked")
    @EventBusListenerMethod(scope = EventScope.UI)
    public void onRolloutDeleteEvent(final RolloutDeleteEventContainer eventContainer) {
        eventContainer.getEvents().forEach(this::handleEvent);
    }

    private void handleEvent(final RolloutDeleteEvent rolloutDeleteEvent) {
        refreshContainer();
    }

    /**
     * Handles the RolloutChangeEvent to refresh the item in the grid.
     *
     * @param eventContainer
     *            container which holds the rollout change event
     */
    @SuppressWarnings("unchecked")
    @EventBusListenerMethod(scope = EventScope.UI)
    public void onRolloutChangeEvent(final RolloutChangeEventContainer eventContainer) {
        eventContainer.getEvents().forEach(this::handleEvent);
    }

    private void handleEvent(final RolloutChangeEvent rolloutChangeEvent) {
        if (!rolloutUIState.isShowRollOuts()) {
            return;
        }
        final Rollout rollout = rolloutManagement.findRolloutWithDetailedStatus(rolloutChangeEvent.getRolloutId(),
                false);

        // rollout is null if rollout was deleted
        if (rollout != null) {
            final TotalTargetCountStatus totalTargetCountStatus = rollout.getTotalTargetCountStatus();
            final LazyQueryContainer rolloutContainer = (LazyQueryContainer) getContainerDataSource();
            final Item item = rolloutContainer.getItem(rolloutChangeEvent.getRolloutId());
            if (item == null) {
                refreshContainer();
                return;
            }
            item.getItemProperty(VAR_STATUS).setValue(rollout.getStatus());
            item.getItemProperty(VAR_TOTAL_TARGETS_COUNT_STATUS).setValue(totalTargetCountStatus);
            final Long groupCount = (Long) item.getItemProperty(VAR_NUMBER_OF_GROUPS).getValue();
            final int groupsCreated = rollout.getRolloutGroupsCreated();
            if (groupsCreated != 0) {
                item.getItemProperty(VAR_NUMBER_OF_GROUPS).setValue(Long.valueOf(groupsCreated));
            } else if (rollout.getRolloutGroups() != null && groupCount != rollout.getRolloutGroups().size()) {
                item.getItemProperty(VAR_NUMBER_OF_GROUPS).setValue(Long.valueOf(rollout.getRolloutGroups().size()));
            }
            item.getItemProperty(ROLLOUT_RENDERER_DATA)
                    .setValue(new RolloutRendererData(rollout.getName(), rollout.getStatus().toString()));
        }
    }

    @Override
    protected Container createContainer() {
        final BeanQueryFactory<RolloutBeanQuery> rolloutQf = new BeanQueryFactory<>(RolloutBeanQuery.class);
        return new LazyQueryContainer(new LazyQueryDefinition(true, SPUIDefinitions.PAGE_SIZE, VAR_ID), rolloutQf);
    }

    @Override
    protected void addContainerProperties() {
        final LazyQueryContainer rolloutGridContainer = (LazyQueryContainer) getContainerDataSource();
        rolloutGridContainer.addContainerProperty(VAR_NAME, String.class, "", false, false);
        rolloutGridContainer.addContainerProperty(DS_TYPE, String.class, null, false, false);
        rolloutGridContainer.addContainerProperty(SW_MODULES, Set.class, null, false, false);
        rolloutGridContainer.addContainerProperty(ROLLOUT_RENDERER_DATA, RolloutRendererData.class, null, false, false);
        rolloutGridContainer.addContainerProperty(VAR_DESC, String.class, null, false, false);
        rolloutGridContainer.addContainerProperty(IS_REQUIRED_MIGRATION_STEP, boolean.class, null, false, false);
        rolloutGridContainer.addContainerProperty(VAR_STATUS, RolloutStatus.class, null, false, false);
        rolloutGridContainer.addContainerProperty(VAR_DIST_NAME_VERSION, String.class, null, false, false);
        rolloutGridContainer.addContainerProperty(VAR_CREATED_DATE, String.class, null, false, false);

        rolloutGridContainer.addContainerProperty(VAR_MODIFIED_DATE, String.class, null, false, false);
        rolloutGridContainer.addContainerProperty(VAR_CREATED_USER, String.class, null, false, false);
        rolloutGridContainer.addContainerProperty(VAR_MODIFIED_BY, String.class, null, false, false);
        rolloutGridContainer.addContainerProperty(VAR_NUMBER_OF_GROUPS, Long.class, 0, false, false);
        rolloutGridContainer.addContainerProperty(VAR_TOTAL_TARGETS, String.class, "0", false, false);
        rolloutGridContainer.addContainerProperty(VAR_TOTAL_TARGETS_COUNT_STATUS, TotalTargetCountStatus.class, null,
                false, false);

        rolloutGridContainer.addContainerProperty(RUN_OPTION, String.class, FontAwesome.PLAY.getHtml(), false, false);
        rolloutGridContainer.addContainerProperty(PAUSE_OPTION, String.class, FontAwesome.PAUSE.getHtml(), false,
                false);

        if (permissionChecker.hasRolloutUpdatePermission()) {
            rolloutGridContainer.addContainerProperty(UPDATE_OPTION, String.class, FontAwesome.EDIT.getHtml(), false,
                    false);
        }
        if (permissionChecker.hasRolloutCreatePermission()) {
            rolloutGridContainer.addContainerProperty(COPY_OPTION, String.class, FontAwesome.COPY.getHtml(), false,
                    false);
        }
        rolloutGridContainer.addContainerProperty(DELETE_OPTION, String.class, FontAwesome.TRASH.getHtml(), false,
                false);
    }

    @Override
    protected void setColumnExpandRatio() {

        getColumn(ROLLOUT_RENDERER_DATA).setMinimumWidth(40);
        getColumn(ROLLOUT_RENDERER_DATA).setMaximumWidth(150);

        getColumn(VAR_DIST_NAME_VERSION).setMinimumWidth(40);
        getColumn(VAR_DIST_NAME_VERSION).setMaximumWidth(150);

        getColumn(VAR_STATUS).setMinimumWidth(75);
        getColumn(VAR_STATUS).setMaximumWidth(75);

        getColumn(VAR_TOTAL_TARGETS).setMinimumWidth(40);
        getColumn(VAR_TOTAL_TARGETS).setMaximumWidth(100);

        getColumn(VAR_NUMBER_OF_GROUPS).setMinimumWidth(40);
        getColumn(VAR_NUMBER_OF_GROUPS).setMaximumWidth(100);

        getColumn(RUN_OPTION).setMinimumWidth(25);
        getColumn(RUN_OPTION).setMaximumWidth(25);

        getColumn(PAUSE_OPTION).setMinimumWidth(25);
        getColumn(PAUSE_OPTION).setMaximumWidth(25);

        if (permissionChecker.hasRolloutUpdatePermission()) {
            getColumn(UPDATE_OPTION).setMinimumWidth(25);
            getColumn(UPDATE_OPTION).setMaximumWidth(25);
        }

        if (permissionChecker.hasRolloutCreatePermission()) {
            getColumn(COPY_OPTION).setMinimumWidth(25);
            getColumn(COPY_OPTION).setMaximumWidth(25);
        }

        getColumn(DELETE_OPTION).setMinimumWidth(25);
        getColumn(DELETE_OPTION).setMaximumWidth(40);

        getColumn(VAR_TOTAL_TARGETS_COUNT_STATUS).setMinimumWidth(280);
    }

    @Override
    protected void setColumnHeaderNames() {
        getColumn(ROLLOUT_RENDERER_DATA).setHeaderCaption(i18n.get("header.name"));
        getColumn(DS_TYPE).setHeaderCaption(i18n.get("header.type"));
        getColumn(SW_MODULES).setHeaderCaption(i18n.get("header.swmodules"));
        getColumn(IS_REQUIRED_MIGRATION_STEP).setHeaderCaption(i18n.get("header.migrations.step"));
        getColumn(VAR_DIST_NAME_VERSION).setHeaderCaption(i18n.get("header.distributionset"));
        getColumn(VAR_NUMBER_OF_GROUPS).setHeaderCaption(i18n.get("header.numberofgroups"));
        getColumn(VAR_TOTAL_TARGETS).setHeaderCaption(i18n.get("header.total.targets"));
        getColumn(VAR_CREATED_DATE).setHeaderCaption(i18n.get("header.createdDate"));
        getColumn(VAR_CREATED_USER).setHeaderCaption(i18n.get("header.createdBy"));
        getColumn(VAR_MODIFIED_DATE).setHeaderCaption(i18n.get("header.modifiedDate"));
        getColumn(VAR_MODIFIED_BY).setHeaderCaption(i18n.get("header.modifiedBy"));
        getColumn(VAR_DESC).setHeaderCaption(i18n.get("header.description"));
        getColumn(VAR_TOTAL_TARGETS_COUNT_STATUS).setHeaderCaption(i18n.get("header.detail.status"));
        getColumn(VAR_STATUS).setHeaderCaption(i18n.get("header.status"));

        getColumn(RUN_OPTION).setHeaderCaption(i18n.get("header.action.run"));
        getColumn(PAUSE_OPTION).setHeaderCaption(i18n.get("header.action.pause"));

        if (permissionChecker.hasRolloutUpdatePermission()) {
            getColumn(UPDATE_OPTION).setHeaderCaption(i18n.get("header.action.update"));
            getColumn(DELETE_OPTION).setHeaderCaption(i18n.get("header.action.delete"));
        }

        if (permissionChecker.hasRolloutCreatePermission()) {
            getColumn(COPY_OPTION).setHeaderCaption(i18n.get("header.action.copy"));
        }
        if (permissionChecker.hasRolloutCreatePermission()) {
            getColumn(COPY_OPTION).setHeaderCaption(i18n.get("header.action.copy"));
        }

        final HeaderCell join = joinColumns();
        join.setText(i18n.get("header.action"));
    }

    private HeaderCell joinColumns() {
        HeaderCell join;
        if (permissionChecker.hasRolloutUpdatePermission() && permissionChecker.hasRolloutCreatePermission()) {
            join = getDefaultHeaderRow().join(RUN_OPTION, PAUSE_OPTION, UPDATE_OPTION, COPY_OPTION, DELETE_OPTION);
        } else if (permissionChecker.hasRolloutUpdatePermission()) {
            join = getDefaultHeaderRow().join(RUN_OPTION, PAUSE_OPTION, UPDATE_OPTION, DELETE_OPTION);
        } else if (permissionChecker.hasRolloutCreatePermission()) {
            join = getDefaultHeaderRow().join(RUN_OPTION, PAUSE_OPTION, COPY_OPTION, DELETE_OPTION);
        } else {
            join = getDefaultHeaderRow().join(RUN_OPTION, PAUSE_OPTION);
        }
        return join;
    }

    @Override
    protected String getGridId() {
        return UIComponentIdProvider.ROLLOUT_LIST_GRID_ID;
    }

    @Override
    protected void setColumnProperties() {
        final List<Object> columnList = new ArrayList<>();
        columnList.add(ROLLOUT_RENDERER_DATA);
        columnList.add(VAR_DIST_NAME_VERSION);
        columnList.add(DS_TYPE);
        columnList.add(SW_MODULES);
        columnList.add(IS_REQUIRED_MIGRATION_STEP);
        columnList.add(VAR_STATUS);
        columnList.add(VAR_TOTAL_TARGETS_COUNT_STATUS);
        columnList.add(VAR_NUMBER_OF_GROUPS);
        columnList.add(VAR_TOTAL_TARGETS);

        columnList.add(RUN_OPTION);
        columnList.add(PAUSE_OPTION);

        if (permissionChecker.hasRolloutUpdatePermission()) {
            columnList.add(UPDATE_OPTION);
        }
        if (permissionChecker.hasRolloutCreatePermission()) {
            columnList.add(COPY_OPTION);
        }
        columnList.add(DELETE_OPTION);

        columnList.add(VAR_CREATED_DATE);
        columnList.add(VAR_CREATED_USER);
        columnList.add(VAR_MODIFIED_DATE);
        columnList.add(VAR_MODIFIED_BY);
        columnList.add(VAR_DESC);
        setColumnOrder(columnList.toArray());
        alignColumns();
    }

    @Override
    protected void setHiddenColumns() {
        final List<Object> columnsToBeHidden = new ArrayList<>();
        columnsToBeHidden.add(VAR_NAME);
        columnsToBeHidden.add(VAR_CREATED_DATE);
        columnsToBeHidden.add(VAR_CREATED_USER);
        columnsToBeHidden.add(VAR_MODIFIED_DATE);
        columnsToBeHidden.add(VAR_MODIFIED_BY);
        columnsToBeHidden.add(VAR_DESC);
        columnsToBeHidden.add(IS_REQUIRED_MIGRATION_STEP);
        columnsToBeHidden.add(DS_TYPE);
        columnsToBeHidden.add(SW_MODULES);
        for (final Object propertyId : columnsToBeHidden) {
            getColumn(propertyId).setHidden(true);
        }
    }

    @Override
    protected CellDescriptionGenerator getDescriptionGenerator() {
        return this::getDescription;
    }

    @Override
    protected void addColumnRenderes() {
        getColumn(VAR_NUMBER_OF_GROUPS).setRenderer(new HtmlRenderer(), new TotalTargetGroupsConverter());
        getColumn(VAR_TOTAL_TARGETS_COUNT_STATUS).setRenderer(new HtmlRenderer(),
                new TotalTargetCountStatusConverter());

        getColumn(VAR_STATUS).setRenderer(new HtmlLabelRenderer(), new RolloutStatusConverter());

        final RolloutRenderer customObjectRenderer = new RolloutRenderer(RolloutRendererData.class);
        customObjectRenderer.addClickListener(this::onClickOfRolloutName);
        getColumn(ROLLOUT_RENDERER_DATA).setRenderer(customObjectRenderer);

        getColumn(RUN_OPTION)
                .setRenderer(new HtmlButtonRenderer(clickEvent -> startOrResumeRollout((Long) clickEvent.getItemId())));

        getColumn(PAUSE_OPTION)
                .setRenderer(new HtmlButtonRenderer(clickEvent -> pauseRollout((Long) clickEvent.getItemId())));

        if (permissionChecker.hasRolloutUpdatePermission()) {
            getColumn(UPDATE_OPTION)
                    .setRenderer(new HtmlButtonRenderer(clickEvent -> updateRollout((Long) clickEvent.getItemId())));
            getColumn(DELETE_OPTION)
                    .setRenderer(new HtmlButtonRenderer(clickEvent -> deleteRollout((Long) clickEvent.getItemId())));
        }
        if (permissionChecker.hasRolloutCreatePermission()) {
            getColumn(COPY_OPTION)
                    .setRenderer(new HtmlButtonRenderer(clickEvent -> copyRollout((Long) clickEvent.getItemId())));
        }

    }

    private void alignColumns() {
        setCellStyleGenerator(new RollouStatusCellStyleGenerator(getContainerDataSource()));
    }

    private void onClickOfRolloutName(final RendererClickEvent event) {
        rolloutUIState.setRolloutId((long) event.getItemId());
        final String rolloutName = (String) getContainerDataSource().getItem(event.getItemId())
                .getItemProperty(VAR_NAME).getValue();
        rolloutUIState.setRolloutName(rolloutName);
        final String ds = (String) getContainerDataSource().getItem(event.getItemId())
                .getItemProperty(VAR_DIST_NAME_VERSION).getValue();
        rolloutUIState.setRolloutDistributionSet(ds);
        eventBus.publish(this, RolloutEvent.SHOW_ROLLOUT_GROUPS);
    }

    private void pauseRollout(final Long rolloutId) {
        final Item row = getContainerDataSource().getItem(rolloutId);

        final RolloutStatus rolloutStatus = (RolloutStatus) row.getItemProperty(VAR_STATUS).getValue();

        if (!RolloutStatus.RUNNING.equals(rolloutStatus)) {
            return;
        }

        final String rolloutName = (String) row.getItemProperty(VAR_NAME).getValue();

        rolloutManagement.pauseRollout(rolloutId);
        uiNotification.displaySuccess(i18n.get("message.rollout.paused", rolloutName));
    }

    private void startOrResumeRollout(final Long rolloutId) {
        final Item row = getContainerDataSource().getItem(rolloutId);

        final RolloutStatus rolloutStatus = (RolloutStatus) row.getItemProperty(VAR_STATUS).getValue();
        final String rolloutName = (String) row.getItemProperty(VAR_NAME).getValue();

        if (RolloutStatus.READY.equals(rolloutStatus)) {
            rolloutManagement.startRollout(rolloutId);
            uiNotification.displaySuccess(i18n.get("message.rollout.started", rolloutName));
            return;
        }

        if (RolloutStatus.PAUSED.equals(rolloutStatus)) {
            rolloutManagement.resumeRollout(rolloutId);
            uiNotification.displaySuccess(i18n.get("message.rollout.resumed", rolloutName));
            return;
        }
    }

    private void updateRollout(final Long rolloutId) {
        final CommonDialogWindow addTargetWindow = addUpdateRolloutWindow.getWindow(rolloutId, false);
        addTargetWindow.setCaption(i18n.get("caption.update.rollout"));
        UI.getCurrent().addWindow(addTargetWindow);
        addTargetWindow.setVisible(Boolean.TRUE);
    }

    private void copyRollout(final Long rolloutId) {
        final CommonDialogWindow addTargetWindow = addUpdateRolloutWindow.getWindow(rolloutId, true);
        addTargetWindow.setCaption(i18n.get("caption.create.rollout"));
        UI.getCurrent().addWindow(addTargetWindow);
        addTargetWindow.setVisible(Boolean.TRUE);
    }

    private void deleteRollout(final Long rolloutId) {
        final String formattedConfirmationQuestion = getConfirmationQuestion(rolloutId);
        final ConfirmationDialog confirmationDialog = new ConfirmationDialog(i18n.get("caption.confirm.delete.rollout"),
                formattedConfirmationQuestion, i18n.get("button.ok"), i18n.get("button.cancel"), ok -> {
                    if (ok) {
                        final Item row = getContainerDataSource().getItem(rolloutId);
                        final String rolloutName = (String) row.getItemProperty(VAR_NAME).getValue();
                        rolloutManagement.deleteRollout(rolloutId);
                        uiNotification.displaySuccess(i18n.get("message.rollout.deleted", rolloutName));
                    }
                });
        UI.getCurrent().addWindow(confirmationDialog.getWindow());
        confirmationDialog.getWindow().bringToFront();
    }

    private String getConfirmationQuestion(final Long rolloutId) {
        final Rollout rolloutData = rolloutManagement.findRolloutWithDetailedStatus(rolloutId, false);
        final Map<Status, Long> statusTotalCount = rolloutData.getTotalTargetCountStatus().getStatusTotalCountMap();
        Long scheduledActions = statusTotalCount.get(Status.SCHEDULED);
        if (scheduledActions == null) {
            scheduledActions = 0L;
        }
        final Long runningActions = statusTotalCount.get(Status.RUNNING);
        String rolloutDetailsMessage = StringUtils.EMPTY;
        if ((scheduledActions > 0) || (runningActions > 0)) {
            rolloutDetailsMessage = i18n.get("message.delete.rollout.details", runningActions, scheduledActions);
        }

        return i18n.get("message.delete.rollout", rolloutData.getName(), rolloutDetailsMessage);
    }

    private String getDescription(final CellReference cell) {

        String description = null;

        if (VAR_STATUS.equals(cell.getPropertyId())) {
            description = cell.getProperty().getValue().toString().toLowerCase();
        } else if (ACTION.equals(cell.getPropertyId())) {
            description = ACTION.toLowerCase();
        } else if (ROLLOUT_RENDERER_DATA.equals(cell.getPropertyId())) {
            description = ((RolloutRendererData) cell.getProperty().getValue()).getName();
        } else if (VAR_TOTAL_TARGETS_COUNT_STATUS.equals(cell.getPropertyId())) {
            description = getTooltip(((TotalTargetCountStatus) cell.getValue()).getStatusTotalCountMap());
        } else if (VAR_DIST_NAME_VERSION.equals(cell.getPropertyId())) {
            description = getDSDetails(cell.getItem());
        }

        return description;
    }

    private static String getDSDetails(final Item rolloutItem) {
        final StringBuilder swModuleNames = new StringBuilder();
        final StringBuilder swModuleVendors = new StringBuilder();
        final Set<SoftwareModule> swModules = (Set<SoftwareModule>) rolloutItem.getItemProperty(SW_MODULES).getValue();
        swModules.forEach(swModule -> {
            swModuleNames.append(swModule.getName());
            swModuleNames.append(" , ");
            swModuleVendors.append(swModule.getVendor());
            swModuleVendors.append(" , ");
        });
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(HTML_UL_OPEN_TAG);
        stringBuilder.append(HTML_LI_OPEN_TAG);
        stringBuilder.append(" DistributionSet Description : ")
                .append((String) rolloutItem.getItemProperty(VAR_DESC).getValue());
        stringBuilder.append(HTML_LI_CLOSE_TAG);
        stringBuilder.append(HTML_LI_OPEN_TAG);
        stringBuilder.append(" DistributionSet Type : ")
                .append((String) rolloutItem.getItemProperty(DS_TYPE).getValue());
        stringBuilder.append(HTML_LI_CLOSE_TAG);
        stringBuilder.append(HTML_LI_OPEN_TAG);
        stringBuilder.append("Required Migration step : ")
                .append((boolean) rolloutItem.getItemProperty(IS_REQUIRED_MIGRATION_STEP).getValue() ? "Yes" : "No");
        stringBuilder.append(HTML_LI_CLOSE_TAG);

        stringBuilder.append(HTML_LI_OPEN_TAG);
        stringBuilder.append("SoftWare Modules : ").append(swModuleNames.toString());
        stringBuilder.append(HTML_LI_CLOSE_TAG);

        stringBuilder.append(HTML_LI_OPEN_TAG);
        stringBuilder.append("Vendor(s) : ").append(swModuleVendors.toString());
        stringBuilder.append(HTML_LI_CLOSE_TAG);

        stringBuilder.append(HTML_UL_CLOSE_TAG);
        return stringBuilder.toString();
    }

    private static class RollouStatusCellStyleGenerator implements CellStyleGenerator {

        private static final long serialVersionUID = 1L;
        /**
         * Contains all expected rollout status per column to enable or disable
         * the button.
         */
        private static final Map<String, RolloutStatus> EXPECTED_ROLLOUT_STATUS_ENABLE_BUTTON = Maps
                .newHashMapWithExpectedSize(2);
        private final Container.Indexed containerDataSource;

        static {
            EXPECTED_ROLLOUT_STATUS_ENABLE_BUTTON.put(RUN_OPTION, RolloutStatus.READY);
            EXPECTED_ROLLOUT_STATUS_ENABLE_BUTTON.put(PAUSE_OPTION, RolloutStatus.RUNNING);
            EXPECTED_ROLLOUT_STATUS_ENABLE_BUTTON.put(UPDATE_OPTION, RolloutStatus.READY);
            EXPECTED_ROLLOUT_STATUS_ENABLE_BUTTON.put(UPDATE_OPTION, RolloutStatus.RUNNING);
            EXPECTED_ROLLOUT_STATUS_ENABLE_BUTTON.put(UPDATE_OPTION, RolloutStatus.PAUSED);
            EXPECTED_ROLLOUT_STATUS_ENABLE_BUTTON.put(UPDATE_OPTION, RolloutStatus.CREATING);
            EXPECTED_ROLLOUT_STATUS_ENABLE_BUTTON.put(UPDATE_OPTION, RolloutStatus.STARTING);
            EXPECTED_ROLLOUT_STATUS_ENABLE_BUTTON.put(UPDATE_OPTION, RolloutStatus.STOPPED);
            EXPECTED_ROLLOUT_STATUS_ENABLE_BUTTON.put(UPDATE_OPTION, RolloutStatus.ERROR_CREATING);
            EXPECTED_ROLLOUT_STATUS_ENABLE_BUTTON.put(UPDATE_OPTION, RolloutStatus.ERROR_STARTING);
            EXPECTED_ROLLOUT_STATUS_ENABLE_BUTTON.put(DELETE_OPTION, RolloutStatus.READY);
            EXPECTED_ROLLOUT_STATUS_ENABLE_BUTTON.put(DELETE_OPTION, RolloutStatus.RUNNING);
            EXPECTED_ROLLOUT_STATUS_ENABLE_BUTTON.put(DELETE_OPTION, RolloutStatus.PAUSED);
            EXPECTED_ROLLOUT_STATUS_ENABLE_BUTTON.put(DELETE_OPTION, RolloutStatus.CREATING);
            EXPECTED_ROLLOUT_STATUS_ENABLE_BUTTON.put(DELETE_OPTION, RolloutStatus.STARTING);
            EXPECTED_ROLLOUT_STATUS_ENABLE_BUTTON.put(DELETE_OPTION, RolloutStatus.STOPPED);
            EXPECTED_ROLLOUT_STATUS_ENABLE_BUTTON.put(DELETE_OPTION, RolloutStatus.ERROR_CREATING);
            EXPECTED_ROLLOUT_STATUS_ENABLE_BUTTON.put(DELETE_OPTION, RolloutStatus.ERROR_STARTING);
        }

        /**
         * Constructor
         *
         * @param containerDataSource
         *            the container
         */
        public RollouStatusCellStyleGenerator(final Container.Indexed containerDataSource) {
            this.containerDataSource = containerDataSource;
        }

        @Override
        public String getStyle(final CellReference cellReference) {
            if (VAR_STATUS.equals(cellReference.getPropertyId())) {
                return "centeralign";
            }
            return convertRolloutStatusToString(cellReference);
        }

        private String convertRolloutStatusToString(final CellReference cellReference) {
            final Object propertyId = cellReference.getPropertyId();
            final RolloutStatus expectedRolloutStatus = EXPECTED_ROLLOUT_STATUS_ENABLE_BUTTON.get(propertyId);
            if (expectedRolloutStatus == null) {
                return null;
            }

            if (RUN_OPTION.equals(cellReference.getPropertyId())) {
                return getStatus(cellReference, RolloutStatus.READY, RolloutStatus.PAUSED);
            }
            if (PAUSE_OPTION.equals(cellReference.getPropertyId())) {
                return getStatus(cellReference, RolloutStatus.RUNNING);
            }
            if (UPDATE_OPTION.equals(cellReference.getPropertyId())) {
                return getStatus(cellReference, RolloutStatus.CREATING, RolloutStatus.ERROR_CREATING,
                        RolloutStatus.ERROR_STARTING, RolloutStatus.PAUSED, RolloutStatus.READY, RolloutStatus.RUNNING,
                        RolloutStatus.STARTING, RolloutStatus.STOPPED);
            }
            if (DELETE_OPTION.equals(cellReference.getPropertyId())) {
                return getStatus(cellReference, RolloutStatus.CREATING, RolloutStatus.ERROR_CREATING,
                        RolloutStatus.ERROR_STARTING, RolloutStatus.PAUSED, RolloutStatus.READY, RolloutStatus.RUNNING,
                        RolloutStatus.STARTING, RolloutStatus.STOPPED);
            }

            return null;
        }

        private String getStatus(final CellReference cellReference, final RolloutStatus... expectedRolloutStatus) {
            final RolloutStatus currentRolloutStatus = getRolloutStatus(cellReference.getItemId());

            if (Arrays.asList(expectedRolloutStatus).contains(currentRolloutStatus)) {
                return null;
            }

            return org.eclipse.hawkbit.ui.customrenderers.client.renderers.HtmlButtonRenderer.DISABLE_VALUE;
        }

        private RolloutStatus getRolloutStatus(final Object itemId) {
            final Item row = containerDataSource.getItem(itemId);
            return (RolloutStatus) row.getItemProperty(VAR_STATUS).getValue();
        }
    }

    /**
     *
     * Converter to convert {@link RolloutStatus} to string.
     *
     */
    class RolloutStatusConverter implements Converter<String, RolloutStatus> {

        private static final long serialVersionUID = -1217685750825632678L;

        @Override
        public RolloutStatus convertToModel(final String value, final Class<? extends RolloutStatus> targetType,
                final Locale locale) {
            return null;
        }

        @Override
        public String convertToPresentation(final RolloutStatus value, final Class<? extends String> targetType,
                final Locale locale) {
            return convertRolloutStatusToString(value);
        }

        @Override
        public Class<RolloutStatus> getModelType() {
            return RolloutStatus.class;
        }

        @Override
        public Class<String> getPresentationType() {
            return String.class;
        }

        private String convertRolloutStatusToString(final RolloutStatus value) {
            StatusFontIcon statusFontIcon = statusIconMap.get(value);
            if (statusFontIcon == null) {
                statusFontIcon = new StatusFontIcon(FontAwesome.QUESTION_CIRCLE, SPUIStyleDefinitions.STATUS_ICON_BLUE);
            }
            final String codePoint = HawkbitCommonUtil.getCodePoint(statusFontIcon);
            return HawkbitCommonUtil.getStatusLabelDetailsInString(codePoint, statusFontIcon.getStyle(),
                    UIComponentIdProvider.ROLLOUT_STATUS_LABEL_ID);
        }
    }

    /**
     * Converter to convert {@link TotalTargetCountStatus} to formatted string
     * with status and count details.
     *
     */
    class TotalTargetCountStatusConverter implements Converter<String, TotalTargetCountStatus> {

        private static final long serialVersionUID = -5794528427855153924L;

        @Override
        public TotalTargetCountStatus convertToModel(final String value,
                final Class<? extends TotalTargetCountStatus> targetType, final Locale locale) {
            return null;
        }

        @Override
        public String convertToPresentation(final TotalTargetCountStatus value,
                final Class<? extends String> targetType, final Locale locale) {
            return DistributionBarHelper.getDistributionBarAsHTMLString(value.getStatusTotalCountMap());
        }

        @Override
        public Class<TotalTargetCountStatus> getModelType() {
            return TotalTargetCountStatus.class;
        }

        @Override
        public Class<String> getPresentationType() {
            return String.class;
        }
    }

    /**
     * Converter to convert 0 to empty, if total target groups is zero.
     *
     */
    class TotalTargetGroupsConverter implements Converter<String, Long> {

        private static final long serialVersionUID = 6589305227035220369L;

        @Override
        public Long convertToModel(final String value, final Class<? extends Long> targetType, final Locale locale) {
            return null;
        }

        @Override
        public String convertToPresentation(final Long value, final Class<? extends String> targetType,
                final Locale locale) {
            if (value == 0) {
                return "";
            }
            return value.toString();
        }

        @Override
        public Class<Long> getModelType() {
            return Long.class;
        }

        @Override
        public Class<String> getPresentationType() {
            return String.class;
        }
    }

}
