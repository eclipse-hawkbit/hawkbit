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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.QuotaManagement;
import org.eclipse.hawkbit.repository.RolloutGroupManagement;
import org.eclipse.hawkbit.repository.RolloutManagement;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.Rollout.RolloutStatus;
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
import org.eclipse.hawkbit.ui.push.RolloutDeletedEventContainer;
import org.eclipse.hawkbit.ui.push.event.RolloutChangedEvent;
import org.eclipse.hawkbit.ui.rollout.DistributionBarHelper;
import org.eclipse.hawkbit.ui.rollout.StatusFontIcon;
import org.eclipse.hawkbit.ui.rollout.event.RolloutEvent;
import org.eclipse.hawkbit.ui.rollout.state.RolloutUIState;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.addons.lazyquerycontainer.BeanQueryFactory;
import org.vaadin.addons.lazyquerycontainer.LazyQueryContainer;
import org.vaadin.addons.lazyquerycontainer.LazyQueryDefinition;
import org.vaadin.spring.events.EventBus.UIEventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.vaadin.data.Container;
import com.vaadin.data.Item;
import com.vaadin.data.util.converter.Converter;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.UI;
import com.vaadin.ui.renderers.ClickableRenderer.RendererClickEvent;
import com.vaadin.ui.renderers.HtmlRenderer;

/**
 * Rollout list grid component.
 */
public class RolloutListGrid extends AbstractGrid<LazyQueryContainer> {

    private static final long serialVersionUID = 1L;

    private static final String UPDATE_OPTION = "Update";

    private static final String COPY_OPTION = "Copy";

    private static final String PAUSE_OPTION = "Pause";

    private static final String RUN_OPTION = "Run";

    private static final String DELETE_OPTION = "Delete";

    private static final String ROLLOUT_RENDERER_DATA = "rolloutRendererData";

    private final transient RolloutManagement rolloutManagement;
    private final transient RolloutGroupManagement rolloutGroupManagement;

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

    private static final List<Object> HIDDEN_COLUMNS = Arrays.asList(SPUILabelDefinitions.VAR_NAME,
            SPUILabelDefinitions.VAR_CREATED_DATE, SPUILabelDefinitions.VAR_CREATED_USER,
            SPUILabelDefinitions.VAR_MODIFIED_DATE, SPUILabelDefinitions.VAR_MODIFIED_BY,
            SPUILabelDefinitions.VAR_DESC);

    RolloutListGrid(final VaadinMessageSource i18n, final UIEventBus eventBus,
            final RolloutManagement rolloutManagement, final UINotification uiNotification,
            final RolloutUIState rolloutUIState, final SpPermissionChecker permissionChecker,
            final TargetManagement targetManagement, final EntityFactory entityFactory, final UiProperties uiProperties,
            final TargetFilterQueryManagement targetFilterQueryManagement,
            final RolloutGroupManagement rolloutGroupManagement, final QuotaManagement quotaManagement) {
        super(i18n, eventBus, permissionChecker);
        this.rolloutManagement = rolloutManagement;
        this.rolloutGroupManagement = rolloutGroupManagement;
        this.addUpdateRolloutWindow = new AddUpdateRolloutWindowLayout(rolloutManagement, targetManagement,
                uiNotification, uiProperties, entityFactory, i18n, eventBus, targetFilterQueryManagement,
                rolloutGroupManagement, quotaManagement);
        this.uiNotification = uiNotification;
        this.rolloutUIState = rolloutUIState;

        init();
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
     * Handles the RolloutDeletedEvent to refresh the grid.
     *
     * @param eventContainer
     *            container which holds the rollout delete event
     */
    @EventBusListenerMethod(scope = EventScope.UI)
    public void onRolloutDeletedEvent(final RolloutDeletedEventContainer eventContainer) {
        refreshContainer();
    }

    /**
     * Handles the RolloutChangeEvent to refresh the item in the grid.
     *
     * @param eventContainer
     *            container which holds the rollout change event
     */
    @EventBusListenerMethod(scope = EventScope.UI)
    public void onRolloutChangeEvent(final RolloutChangeEventContainer eventContainer) {
        eventContainer.getEvents().forEach(this::handleEvent);
    }

    private void handleEvent(final RolloutChangedEvent rolloutChangeEvent) {
        if (!rolloutUIState.isShowRollOuts() || rolloutChangeEvent.getRolloutId() == null) {
            return;
        }
        final Optional<Rollout> rollout = rolloutManagement
                .getWithDetailedStatus(rolloutChangeEvent.getRolloutId());

        if (!rollout.isPresent()) {
            return;
        }

        final LazyQueryContainer rolloutContainer = (LazyQueryContainer) getContainerDataSource();
        final Item item = rolloutContainer.getItem(rolloutChangeEvent.getRolloutId());
        if (item == null) {
            refreshContainer();
            return;
        }
        updateItem(rollout.get(), item);
    }

    private void updateItem(final Rollout rollout, final Item item) {
        final TotalTargetCountStatus totalTargetCountStatus = rollout.getTotalTargetCountStatus();
        item.getItemProperty(SPUILabelDefinitions.VAR_STATUS).setValue(rollout.getStatus());
        item.getItemProperty(SPUILabelDefinitions.VAR_TOTAL_TARGETS_COUNT_STATUS).setValue(totalTargetCountStatus);
        final Long groupCount = Long
                .valueOf((Integer) item.getItemProperty(SPUILabelDefinitions.VAR_NUMBER_OF_GROUPS).getValue());
        final int groupsCreated = rollout.getRolloutGroupsCreated();
        item.getItemProperty(ROLLOUT_RENDERER_DATA)
                .setValue(new RolloutRendererData(rollout.getName(), rollout.getStatus().toString()));

        if (groupsCreated != 0) {
            item.getItemProperty(SPUILabelDefinitions.VAR_NUMBER_OF_GROUPS).setValue(Integer.valueOf(groupsCreated));
            return;
        }

        final Long size = rolloutGroupManagement.countTargetsOfRolloutsGroup(rollout.getId());
        if (!size.equals(groupCount)) {
            item.getItemProperty(SPUILabelDefinitions.VAR_NUMBER_OF_GROUPS).setValue(size.intValue());
        }
    }

    @Override
    protected LazyQueryContainer createContainer() {
        final BeanQueryFactory<RolloutBeanQuery> rolloutQf = new BeanQueryFactory<>(RolloutBeanQuery.class);
        return new LazyQueryContainer(
                new LazyQueryDefinition(true, SPUIDefinitions.PAGE_SIZE, SPUILabelDefinitions.VAR_ID), rolloutQf);
    }

    @Override
    protected void addContainerProperties() {
        final LazyQueryContainer rolloutGridContainer = (LazyQueryContainer) getContainerDataSource();
        rolloutGridContainer.addContainerProperty(SPUILabelDefinitions.VAR_NAME, String.class, "", false, false);
        rolloutGridContainer.addContainerProperty(ROLLOUT_RENDERER_DATA, RolloutRendererData.class, null, false, false);
        rolloutGridContainer.addContainerProperty(SPUILabelDefinitions.VAR_DESC, String.class, null, false, false);
        rolloutGridContainer.addContainerProperty(SPUILabelDefinitions.VAR_STATUS, RolloutStatus.class, null, false,
                false);
        rolloutGridContainer.addContainerProperty(SPUILabelDefinitions.VAR_DIST_NAME_VERSION, String.class, null, false,
                false);
        rolloutGridContainer.addContainerProperty(SPUILabelDefinitions.VAR_CREATED_DATE, String.class, null, false,
                false);

        rolloutGridContainer.addContainerProperty(SPUILabelDefinitions.VAR_MODIFIED_DATE, String.class, null, false,
                false);
        rolloutGridContainer.addContainerProperty(SPUILabelDefinitions.VAR_CREATED_USER, String.class, null, false,
                false);
        rolloutGridContainer.addContainerProperty(SPUILabelDefinitions.VAR_MODIFIED_BY, String.class, null, false,
                false);
        rolloutGridContainer.addContainerProperty(SPUILabelDefinitions.VAR_NUMBER_OF_GROUPS, Integer.class, 0, false,
                false);
        rolloutGridContainer.addContainerProperty(SPUILabelDefinitions.VAR_TOTAL_TARGETS, String.class, "0", false,
                false);
        rolloutGridContainer.addContainerProperty(SPUILabelDefinitions.VAR_TOTAL_TARGETS_COUNT_STATUS,
                TotalTargetCountStatus.class, null, false, false);

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
        rolloutGridContainer.addContainerProperty(DELETE_OPTION, String.class, FontAwesome.TRASH_O.getHtml(), false,
                false);
    }

    @Override
    protected void setColumnExpandRatio() {

        getColumn(ROLLOUT_RENDERER_DATA).setMinimumWidth(40);
        getColumn(ROLLOUT_RENDERER_DATA).setMaximumWidth(300);

        getColumn(SPUILabelDefinitions.VAR_DIST_NAME_VERSION).setMinimumWidth(40);
        getColumn(SPUILabelDefinitions.VAR_DIST_NAME_VERSION).setMaximumWidth(300);

        getColumn(SPUILabelDefinitions.VAR_STATUS).setMinimumWidth(40);
        getColumn(SPUILabelDefinitions.VAR_STATUS).setMaximumWidth(60);

        getColumn(SPUILabelDefinitions.VAR_TOTAL_TARGETS).setMinimumWidth(40);
        getColumn(SPUILabelDefinitions.VAR_TOTAL_TARGETS).setMaximumWidth(60);

        getColumn(SPUILabelDefinitions.VAR_NUMBER_OF_GROUPS).setMinimumWidth(40);
        getColumn(SPUILabelDefinitions.VAR_NUMBER_OF_GROUPS).setMaximumWidth(60);

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

        getColumn(SPUILabelDefinitions.VAR_TOTAL_TARGETS_COUNT_STATUS).setMinimumWidth(280);
    }

    @Override
    protected void setColumnHeaderNames() {
        getColumn(ROLLOUT_RENDERER_DATA).setHeaderCaption(i18n.getMessage("header.name"));
        getColumn(SPUILabelDefinitions.VAR_DIST_NAME_VERSION)
                .setHeaderCaption(i18n.getMessage("header.distributionset"));
        getColumn(SPUILabelDefinitions.VAR_NUMBER_OF_GROUPS).setHeaderCaption(i18n.getMessage("header.numberofgroups"));
        getColumn(SPUILabelDefinitions.VAR_TOTAL_TARGETS).setHeaderCaption(i18n.getMessage("header.total.targets"));
        getColumn(SPUILabelDefinitions.VAR_CREATED_DATE).setHeaderCaption(i18n.getMessage("header.createdDate"));
        getColumn(SPUILabelDefinitions.VAR_CREATED_USER).setHeaderCaption(i18n.getMessage("header.createdBy"));
        getColumn(SPUILabelDefinitions.VAR_MODIFIED_DATE).setHeaderCaption(i18n.getMessage("header.modifiedDate"));
        getColumn(SPUILabelDefinitions.VAR_MODIFIED_BY).setHeaderCaption(i18n.getMessage("header.modifiedBy"));
        getColumn(SPUILabelDefinitions.VAR_DESC).setHeaderCaption(i18n.getMessage("header.description"));
        getColumn(SPUILabelDefinitions.VAR_TOTAL_TARGETS_COUNT_STATUS)
                .setHeaderCaption(i18n.getMessage("header.detail.status"));
        getColumn(SPUILabelDefinitions.VAR_STATUS).setHeaderCaption(i18n.getMessage("header.status"));

        getColumn(RUN_OPTION).setHeaderCaption(i18n.getMessage("header.action.run"));
        getColumn(PAUSE_OPTION).setHeaderCaption(i18n.getMessage("header.action.pause"));

        if (permissionChecker.hasRolloutUpdatePermission()) {
            getColumn(UPDATE_OPTION).setHeaderCaption(i18n.getMessage("header.action.update"));
        }

        if (permissionChecker.hasRolloutCreatePermission()) {
            getColumn(COPY_OPTION).setHeaderCaption(i18n.getMessage("header.action.copy"));
        }

        getColumn(DELETE_OPTION).setHeaderCaption(i18n.getMessage("header.action.delete"));

        joinColumns().setText(i18n.getMessage("header.action"));
    }

    private HeaderCell joinColumns() {
        if (permissionChecker.hasRolloutUpdatePermission() && permissionChecker.hasRolloutCreatePermission()) {
            return getDefaultHeaderRow().join(RUN_OPTION, PAUSE_OPTION, UPDATE_OPTION, COPY_OPTION, DELETE_OPTION);
        }
        if (permissionChecker.hasRolloutUpdatePermission()) {
            return getDefaultHeaderRow().join(RUN_OPTION, PAUSE_OPTION, UPDATE_OPTION, DELETE_OPTION);
        }
        if (permissionChecker.hasRolloutCreatePermission()) {
            return getDefaultHeaderRow().join(RUN_OPTION, PAUSE_OPTION, COPY_OPTION, DELETE_OPTION);
        }
        return getDefaultHeaderRow().join(RUN_OPTION, PAUSE_OPTION);
    }

    @Override
    protected String getGridId() {
        return UIComponentIdProvider.ROLLOUT_LIST_GRID_ID;
    }

    @Override
    protected void setColumnProperties() {
        final List<Object> columnList = new ArrayList<>();
        columnList.add(ROLLOUT_RENDERER_DATA);
        columnList.add(SPUILabelDefinitions.VAR_DIST_NAME_VERSION);
        columnList.add(SPUILabelDefinitions.VAR_STATUS);
        columnList.add(SPUILabelDefinitions.VAR_TOTAL_TARGETS_COUNT_STATUS);
        columnList.add(SPUILabelDefinitions.VAR_NUMBER_OF_GROUPS);
        columnList.add(SPUILabelDefinitions.VAR_TOTAL_TARGETS);

        columnList.add(RUN_OPTION);
        columnList.add(PAUSE_OPTION);

        if (permissionChecker.hasRolloutUpdatePermission()) {
            columnList.add(UPDATE_OPTION);
        }
        if (permissionChecker.hasRolloutCreatePermission()) {
            columnList.add(COPY_OPTION);
        }
        columnList.add(DELETE_OPTION);

        columnList.add(SPUILabelDefinitions.VAR_CREATED_DATE);
        columnList.add(SPUILabelDefinitions.VAR_CREATED_USER);
        columnList.add(SPUILabelDefinitions.VAR_MODIFIED_DATE);
        columnList.add(SPUILabelDefinitions.VAR_MODIFIED_BY);
        columnList.add(SPUILabelDefinitions.VAR_DESC);
        setColumnOrder(columnList.toArray());
        alignColumns();
    }

    @Override
    protected void setHiddenColumns() {
        for (final Object propertyId : HIDDEN_COLUMNS) {
            getColumn(propertyId).setHidden(true);
        }
    }

    @Override
    protected CellDescriptionGenerator getDescriptionGenerator() {
        return this::getDescription;
    }

    @Override
    protected void addColumnRenderes() {
        getColumn(SPUILabelDefinitions.VAR_NUMBER_OF_GROUPS).setRenderer(new HtmlRenderer(),
                new TotalTargetGroupsConverter());
        getColumn(SPUILabelDefinitions.VAR_TOTAL_TARGETS_COUNT_STATUS).setRenderer(new HtmlRenderer(),
                new TotalTargetCountStatusConverter());

        getColumn(SPUILabelDefinitions.VAR_STATUS).setRenderer(new HtmlLabelRenderer(), new RolloutStatusConverter());

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
        }
        if (permissionChecker.hasRolloutCreatePermission()) {
            getColumn(COPY_OPTION)
                    .setRenderer(new HtmlButtonRenderer(clickEvent -> copyRollout((Long) clickEvent.getItemId())));
        }

        getColumn(DELETE_OPTION)
                .setRenderer(new HtmlButtonRenderer(clickEvent -> deleteRollout((Long) clickEvent.getItemId())));

    }

    private void alignColumns() {
        setCellStyleGenerator(new RollouStatusCellStyleGenerator(getContainerDataSource()));
    }

    private void onClickOfRolloutName(final RendererClickEvent event) {
        rolloutUIState.setRolloutId((long) event.getItemId());
        final String rolloutName = (String) getContainerDataSource().getItem(event.getItemId())
                .getItemProperty(SPUILabelDefinitions.VAR_NAME).getValue();
        rolloutUIState.setRolloutName(rolloutName);
        final String ds = (String) getContainerDataSource().getItem(event.getItemId())
                .getItemProperty(SPUILabelDefinitions.VAR_DIST_NAME_VERSION).getValue();
        rolloutUIState.setRolloutDistributionSet(ds);
        eventBus.publish(this, RolloutEvent.SHOW_ROLLOUT_GROUPS);
    }

    private void pauseRollout(final Long rolloutId) {
        final Item row = getContainerDataSource().getItem(rolloutId);

        final RolloutStatus rolloutStatus = (RolloutStatus) row.getItemProperty(SPUILabelDefinitions.VAR_STATUS)
                .getValue();

        if (!RolloutStatus.RUNNING.equals(rolloutStatus)) {
            return;
        }

        final String rolloutName = (String) row.getItemProperty(SPUILabelDefinitions.VAR_NAME).getValue();

        rolloutManagement.pauseRollout(rolloutId);
        uiNotification.displaySuccess(i18n.getMessage("message.rollout.paused", rolloutName));
    }

    private void startOrResumeRollout(final Long rolloutId) {
        final Item row = getContainerDataSource().getItem(rolloutId);

        final RolloutStatus rolloutStatus = (RolloutStatus) row.getItemProperty(SPUILabelDefinitions.VAR_STATUS)
                .getValue();
        final String rolloutName = (String) row.getItemProperty(SPUILabelDefinitions.VAR_NAME).getValue();

        if (RolloutStatus.READY.equals(rolloutStatus)) {
            rolloutManagement.start(rolloutId);
            uiNotification.displaySuccess(i18n.getMessage("message.rollout.started", rolloutName));
            return;
        }

        if (RolloutStatus.PAUSED.equals(rolloutStatus)) {
            rolloutManagement.resumeRollout(rolloutId);
            uiNotification.displaySuccess(i18n.getMessage("message.rollout.resumed", rolloutName));
            return;
        }
    }

    private void updateRollout(final Long rolloutId) {
        final CommonDialogWindow addTargetWindow = addUpdateRolloutWindow.getWindow(rolloutId, false);
        addTargetWindow.setCaption(i18n.getMessage("caption.update.rollout"));
        UI.getCurrent().addWindow(addTargetWindow);
        addTargetWindow.setVisible(Boolean.TRUE);
    }

    private void copyRollout(final Long rolloutId) {
        final CommonDialogWindow addTargetWindow = addUpdateRolloutWindow.getWindow(rolloutId, true);
        addTargetWindow.setCaption(i18n.getMessage("caption.create.rollout"));
        UI.getCurrent().addWindow(addTargetWindow);
        addTargetWindow.setVisible(Boolean.TRUE);
    }

    private void deleteRollout(final Long rolloutId) {
        final Optional<Rollout> rollout = rolloutManagement.getWithDetailedStatus(rolloutId);

        if (!rollout.isPresent()) {
            return;
        }

        final String formattedConfirmationQuestion = getConfirmationQuestion(rollout.get());
        final ConfirmationDialog confirmationDialog = new ConfirmationDialog(
                i18n.getMessage("caption.confirm.delete.rollout"), formattedConfirmationQuestion,
                i18n.getMessage("button.ok"), i18n.getMessage("button.cancel"), ok -> {
                    if (!ok) {
                        return;
                    }
                    final Item row = getContainerDataSource().getItem(rolloutId);
                    final String rolloutName = (String) row.getItemProperty(SPUILabelDefinitions.VAR_NAME).getValue();
                    rolloutManagement.delete(rolloutId);
                    uiNotification.displaySuccess(i18n.getMessage("message.rollout.deleted", rolloutName));
                }, UIComponentIdProvider.ROLLOUT_DELETE_CONFIRMATION_DIALOG);
        UI.getCurrent().addWindow(confirmationDialog.getWindow());
        confirmationDialog.getWindow().bringToFront();
    }

    private String getConfirmationQuestion(final Rollout rollout) {

        final Map<Status, Long> statusTotalCount = rollout.getTotalTargetCountStatus().getStatusTotalCountMap();
        Long scheduledActions = statusTotalCount.get(Status.SCHEDULED);
        if (scheduledActions == null) {
            scheduledActions = 0L;
        }
        final Long runningActions = statusTotalCount.get(Status.RUNNING);
        String rolloutDetailsMessage = "";
        if ((scheduledActions > 0) || (runningActions > 0)) {
            rolloutDetailsMessage = i18n.getMessage("message.delete.rollout.details", runningActions, scheduledActions);
        }

        return i18n.getMessage("message.delete.rollout", rollout.getName(), rolloutDetailsMessage);
    }

    private String getDescription(final CellReference cell) {

        String description = null;

        if (SPUILabelDefinitions.VAR_STATUS.equals(cell.getPropertyId())) {
            description = cell.getProperty().getValue().toString().toLowerCase();
        } else if (SPUILabelDefinitions.ACTION.equals(cell.getPropertyId())) {
            description = SPUILabelDefinitions.ACTION.toLowerCase();
        } else if (ROLLOUT_RENDERER_DATA.equals(cell.getPropertyId())) {
            description = ((RolloutRendererData) cell.getProperty().getValue()).getName();
        } else if (SPUILabelDefinitions.VAR_TOTAL_TARGETS_COUNT_STATUS.equals(cell.getPropertyId())) {
            description = getTooltip(((TotalTargetCountStatus) cell.getValue()).getStatusTotalCountMap());
        }

        return description;
    }

    private static class RollouStatusCellStyleGenerator implements CellStyleGenerator {

        private static final List<RolloutStatus> DELETE_COPY_BUTTON_ENABLED = Arrays.asList(RolloutStatus.CREATING,
                RolloutStatus.ERROR_CREATING, RolloutStatus.ERROR_STARTING, RolloutStatus.PAUSED, RolloutStatus.READY,
                RolloutStatus.RUNNING, RolloutStatus.STARTING, RolloutStatus.STOPPED, RolloutStatus.FINISHED);

        private static final List<RolloutStatus> UPDATE_BUTTON_ENABLED = Arrays.asList(RolloutStatus.CREATING,
                RolloutStatus.ERROR_CREATING, RolloutStatus.ERROR_STARTING, RolloutStatus.PAUSED, RolloutStatus.READY,
                RolloutStatus.RUNNING, RolloutStatus.STARTING, RolloutStatus.STOPPED);

        private static final List<RolloutStatus> PAUSE_BUTTON_ENABLED = Arrays.asList(RolloutStatus.RUNNING);

        private static final List<RolloutStatus> RUN_BUTTON_ENABLED = Arrays.asList(RolloutStatus.READY,
                RolloutStatus.PAUSED);

        private static final long serialVersionUID = 1L;

        private final Container.Indexed containerDataSource;

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
            if (SPUILabelDefinitions.VAR_STATUS.equals(cellReference.getPropertyId())) {
                return "centeralign";
            }
            return convertRolloutStatusToString(cellReference);
        }

        private String convertRolloutStatusToString(final CellReference cellReference) {
            final String propertyId = (String) cellReference.getPropertyId();

            if (RUN_OPTION.equals(propertyId)) {
                return getStatus(cellReference, RUN_BUTTON_ENABLED);
            }

            if (PAUSE_OPTION.equals(propertyId)) {
                return getStatus(cellReference, PAUSE_BUTTON_ENABLED);
            }

            if (UPDATE_OPTION.equals(propertyId)) {
                return getStatus(cellReference, UPDATE_BUTTON_ENABLED);
            }

            if (DELETE_OPTION.equals(propertyId) || COPY_OPTION.equals(propertyId)) {
                return getStatus(cellReference, DELETE_COPY_BUTTON_ENABLED);
            }

            return null;
        }

        private String getStatus(final CellReference cellReference, final List<RolloutStatus> expectedRolloutStatus) {
            final RolloutStatus currentRolloutStatus = getRolloutStatus(cellReference.getItemId());

            if (expectedRolloutStatus.contains(currentRolloutStatus)) {
                return null;
            }

            return org.eclipse.hawkbit.ui.customrenderers.client.renderers.HtmlButtonRenderer.DISABLE_VALUE;
        }

        private RolloutStatus getRolloutStatus(final Object itemId) {
            final Item row = containerDataSource.getItem(itemId);
            return (RolloutStatus) row.getItemProperty(SPUILabelDefinitions.VAR_STATUS).getValue();
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

        private static final long serialVersionUID = 1L;

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
    class TotalTargetGroupsConverter implements Converter<String, Integer> {

        private static final long serialVersionUID = 1L;

        @Override
        public Integer convertToModel(final String value, final Class<? extends Integer> targetType,
                final Locale locale) {
            return null;
        }

        @Override
        public String convertToPresentation(final Integer value, final Class<? extends String> targetType,
                final Locale locale) {
            if (value == 0) {
                return "";
            }
            return value.toString();
        }

        @Override
        public Class<Integer> getModelType() {
            return Integer.class;
        }

        @Override
        public Class<String> getPresentationType() {
            return String.class;
        }
    }

}
