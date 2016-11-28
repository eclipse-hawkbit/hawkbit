/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.actionhistory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringJoiner;

import org.eclipse.hawkbit.repository.ActionStatusFields;
import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.exception.CancelActionNotAllowedException;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.eclipse.hawkbit.repository.model.ActionWithStatusCount;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.ui.common.ConfirmationDialog;
import org.eclipse.hawkbit.ui.common.builder.LabelBuilder;
import org.eclipse.hawkbit.ui.common.table.BaseEntityEventType;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleSmallNoBorder;
import org.eclipse.hawkbit.ui.management.event.ManagementUIEvent;
import org.eclipse.hawkbit.ui.management.event.PinUnpinEvent;
import org.eclipse.hawkbit.ui.management.event.TargetTableEvent;
import org.eclipse.hawkbit.ui.management.state.ManagementUIState;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.SPDateTimeUtil;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.EventBus.UIEventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.vaadin.data.Container;
import com.vaadin.data.Item;
import com.vaadin.data.util.HierarchicalContainer;
import com.vaadin.server.FontAwesome;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Table;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TreeTable;
import com.vaadin.ui.UI;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Table for {@link Target#getActions()} history.
 *
 */
public class ActionHistoryTable extends TreeTable {

    private static final long serialVersionUID = -1631514704696786653L;
    private static final Logger LOG = LoggerFactory.getLogger(ActionHistoryTable.class);
    private static final String BUTTON_CANCEL = "button.cancel";
    private static final String BUTTON_OK = "button.ok";
    private static final String STATUS_ICON_GREEN = "statusIconGreen";

    private final I18N i18n;
    private final DeploymentManagement deploymentManagement;
    private final EventBus.UIEventBus eventBus;
    private final UINotification notification;
    private final ManagementUIState managementUIState;

    private Container hierarchicalContainer;
    private boolean alreadyHasMessages;

    private Target target;

    public ActionHistoryTable(final I18N i18n, final DeploymentManagement deploymentManagement,
            final UIEventBus eventBus, final UINotification notification, final ManagementUIState managementUIState) {
        this.i18n = i18n;
        this.deploymentManagement = deploymentManagement;
        this.eventBus = eventBus;
        this.notification = notification;
        this.managementUIState = managementUIState;

        initializeTableSettings();
        buildComponent();
        restorePreviousState();
        setVisibleColumns(getVisbleColumns().toArray());
        eventBus.subscribe(this);
        setPageLength(SPUIDefinitions.PAGE_SIZE);
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onEvent(final ManagementUIEvent mgmtUIEvent) {
        if (mgmtUIEvent == ManagementUIEvent.MAX_ACTION_HISTORY) {
            UI.getCurrent().access(() -> createTableContentForMax());
        }
        if (mgmtUIEvent == ManagementUIEvent.MIN_ACTION_HISTORY) {
            UI.getCurrent().access(() -> normalActionHistoryTable());
        }
    }

    /*
     * this method is called when the target history component is initialized
     * without any data
     */
    private void buildComponent() {
        // create an empty container
        createContainer();
        setContainerDataSource(hierarchicalContainer);
        addGeneratedColumns();
        setColumnExpandRatioForMinimisedTable();
    }

    private void setColumnExpandRatioForMinimisedTable() {
        setColumnExpandRatio(SPUIDefinitions.ACTION_HIS_TBL_ACTION_ID, 0.1F);
        setColumnExpandRatio(SPUIDefinitions.ACTION_HIS_TBL_DIST, 0.3F);
        setColumnExpandRatio(SPUIDefinitions.ACTION_HIS_TBL_STATUS, 0.15F);
        setColumnExpandRatio(SPUIDefinitions.ACTION_HIS_TBL_DATETIME, 0.3F);
        setColumnExpandRatio(SPUIDefinitions.ACTION_HIS_TBL_FORCED, 0.15F);
        setColumnExpandRatio(SPUIDefinitions.ACTIONS_COLUMN, 0.2F);
    }

    private void initializeTableSettings() {

        setId(UIComponentIdProvider.ACTION_HISTORY_TABLE_ID);
        setSelectable(false);
        setMultiSelect(false);
        setSortEnabled(true);
        setColumnReorderingAllowed(true);
        setHeight(100.0F, Unit.PERCENTAGE);
        setWidth(100.0F, Unit.PERCENTAGE);
        setImmediate(true);
        setStyleName("sp-table");
        addStyleName(ValoTheme.TABLE_NO_VERTICAL_LINES);
        addStyleName(ValoTheme.TABLE_SMALL);
        setColumnAlignment(SPUIDefinitions.ACTION_HIS_TBL_FORCED, Align.CENTER);
        setColumnAlignment(SPUIDefinitions.ACTION_HIS_TBL_STATUS, Align.CENTER);
        setColumnAlignment(SPUIDefinitions.ACTIONS_COLUMN, Align.CENTER);
        // listeners for child
        addExpandListener(event -> {
            expandParentActionRow(event.getItemId());
            managementUIState.getExpandParentActionRowId().add(event.getItemId());
        });
        addCollapseListener(event -> {
            collapseParentActionRow(event.getItemId());
            managementUIState.getExpandParentActionRowId().remove(event.getItemId());
        });
    }

    /**
     * Create a empty HierarchicalContainer.
     */
    public void createContainer() {
        /* Create HierarchicalContainer container */
        hierarchicalContainer = new HierarchicalContainer();
        hierarchicalContainer.addContainerProperty(SPUIDefinitions.ACTION_HIS_TBL_ACTIVE_HIDDEN, String.class, null);
        hierarchicalContainer.addContainerProperty(SPUIDefinitions.ACTION_HIS_TBL_FORCED, Action.class, null);
        hierarchicalContainer.addContainerProperty(SPUIDefinitions.ACTION_HIS_TBL_ACTION_ID_HIDDEN, Long.class, null);
        hierarchicalContainer.addContainerProperty(SPUIDefinitions.ACTION_HIS_TBL_ACTION_ID, String.class, null);
        hierarchicalContainer.addContainerProperty(SPUIDefinitions.ACTION_HIS_TBL_DIST, String.class, null);
        hierarchicalContainer.addContainerProperty(SPUIDefinitions.ACTION_HIS_TBL_DATETIME, String.class, null);
        hierarchicalContainer.addContainerProperty(SPUIDefinitions.ACTION_HIS_TBL_STATUS_HIDDEN, Action.Status.class,
                null);
        hierarchicalContainer.addContainerProperty(SPUIDefinitions.ACTION_HIS_TBL_MSGS_HIDDEN, List.class, null);
        hierarchicalContainer.addContainerProperty(SPUIDefinitions.ACTION_HIS_TBL_ROLLOUT_NAME, String.class, null);

    }

    private List<Object> getVisbleColumns() {
        final List<Object> visibleColumnIds = new ArrayList<>();
        visibleColumnIds.add(SPUIDefinitions.ACTION_HIS_TBL_ACTIVE);
        visibleColumnIds.add(SPUIDefinitions.ACTION_HIS_TBL_DIST);
        visibleColumnIds.add(SPUIDefinitions.ACTION_HIS_TBL_DATETIME);
        visibleColumnIds.add(SPUIDefinitions.ACTION_HIS_TBL_STATUS);
        visibleColumnIds.add(SPUIDefinitions.ACTION_HIS_TBL_FORCED);
        visibleColumnIds.add(SPUIDefinitions.ACTIONS_COLUMN);

        if (managementUIState.isActionHistoryMaximized()) {
            visibleColumnIds.add(SPUIDefinitions.ACTION_HIS_TBL_ROLLOUT_NAME);
            visibleColumnIds.add(SPUIDefinitions.ACTION_HIS_TBL_MSGS);
            visibleColumnIds.add(1, SPUIDefinitions.ACTION_HIS_TBL_ACTION_ID);
        }
        return visibleColumnIds;
    }

    /**
     * fetch the target details using controller id, and set it globally.
     *
     * @param selectedTarget
     *            reference of target
     */
    public void populateTableData(final Target selectedTarget) {
        target = selectedTarget;
        refreshContainer();
    }

    /* re -create the container and get the data and set it to the table */
    private void refreshContainer() {
        getcontainerData();
        // to expand parent row , if already expanded.
        expandParentRow();

    }

    private void getcontainerData() {
        hierarchicalContainer.removeAllItems();

        if (target != null) {
            /* service method to create action history for target */
            final List<ActionWithStatusCount> actionHistory = deploymentManagement
                    .findActionsWithStatusCountByTargetOrderByIdDesc(target);

            addDetailsToContainer(actionHistory);
        }
    }

    /**
     * Populate Container for Action.
     *
     * @param isActiveActions
     *            as flag
     * @param reversedActions
     *            as action
     * @param startIdx
     *            as sort
     * @param target2
     * @param actionHistoryMode
     *            as either {@link ActionHistoryMode.NORMAL} or
     *            {@link ActionHistoryMode.MAXIMIZED}
     */
    @SuppressWarnings("unchecked")
    private void addDetailsToContainer(final List<ActionWithStatusCount> actions) {
        for (final ActionWithStatusCount actionWithStatusCount : actions) {

            final Action action = actionWithStatusCount.getAction();

            final Item item = hierarchicalContainer.addItem(actionWithStatusCount.getAction().getId());

            item.getItemProperty(SPUIDefinitions.ACTION_HIS_TBL_STATUS_HIDDEN)
                    .setValue(actionWithStatusCount.getAction().getStatus());

            /*
             * add action id.
             */
            item.getItemProperty(SPUIDefinitions.ACTION_HIS_TBL_ACTION_ID)
                    .setValue(actionWithStatusCount.getAction().getId().toString());
            /*
             * add active/inactive status to the item which will be used in
             * Column generator to generate respective icon
             */
            item.getItemProperty(SPUIDefinitions.ACTION_HIS_TBL_ACTIVE_HIDDEN).setValue(
                    actionWithStatusCount.getAction().isActive() ? SPUIDefinitions.ACTIVE : SPUIDefinitions.IN_ACTIVE);

            /*
             * add action Id to the item which will be used for fetching child
             * items ( previous action status ) during expand
             */
            item.getItemProperty(SPUIDefinitions.ACTION_HIS_TBL_ACTION_ID_HIDDEN)
                    .setValue(actionWithStatusCount.getAction().getId());

            /*
             * add distribution name to the item which will be displayed in the
             * table. The name should not exceed certain limit.
             */
            item.getItemProperty(SPUIDefinitions.ACTION_HIS_TBL_DIST)
                    .setValue(actionWithStatusCount.getDsName() + ":" + actionWithStatusCount.getDsVersion());
            item.getItemProperty(SPUIDefinitions.ACTION_HIS_TBL_FORCED).setValue(action);

            /* Default no child */
            ((Hierarchical) hierarchicalContainer).setChildrenAllowed(actionWithStatusCount.getAction().getId(), false);

            item.getItemProperty(SPUIDefinitions.ACTION_HIS_TBL_DATETIME)
                    .setValue(SPDateTimeUtil.getFormattedDate(actionWithStatusCount.getAction().getLastModifiedAt()));

            item.getItemProperty(SPUIDefinitions.ACTION_HIS_TBL_ROLLOUT_NAME)
                    .setValue(actionWithStatusCount.getRolloutName());

            if (actionWithStatusCount.getActionStatusCount() > 0) {
                ((Hierarchical) hierarchicalContainer).setChildrenAllowed(actionWithStatusCount.getAction().getId(),
                        true);
            }
        }
    }

    private void addGeneratedColumns() {
        addGeneratedColumn(SPUIDefinitions.ACTION_HIS_TBL_ACTIVE, new Table.ColumnGenerator() {
            /* Serial Verion Id */
            private static final long serialVersionUID = -8673604389011758339L;

            @Override
            public Component generateCell(final Table source, final Object itemId, final Object columnId) {
                return getActiveColumn(itemId);
            }
        });
        addGeneratedColumn(SPUIDefinitions.ACTION_HIS_TBL_STATUS, new Table.ColumnGenerator() {
            /* Serial Verion Id */
            private static final long serialVersionUID = 1L;

            @Override
            public Component generateCell(final Table source, final Object itemId, final Object columnId) {
                return getStatusColumn(itemId);
            }
        });
        // forced
        addGeneratedColumn(SPUIDefinitions.ACTION_HIS_TBL_FORCED, new Table.ColumnGenerator() {
            /* Serial Verion Id */
            private static final long serialVersionUID = 1L;

            @Override
            public Component generateCell(final Table source, final Object itemId, final Object columnId) {
                return getForcedColumn(itemId);
            }
        });

        addGeneratedColumn(SPUIDefinitions.ACTIONS_COLUMN, new Table.ColumnGenerator() {

            private static final long serialVersionUID = 1L;

            @Override
            public Component generateCell(final Table source, final Object itemId, final Object columnId) {
                return createActionBarColumn(itemId);
            }
        });
    }

    private Component getForcedColumn(final Object itemId) {
        final Action actionWithActiveStatus = (Action) hierarchicalContainer.getItem(itemId)
                .getItemProperty(SPUIDefinitions.ACTION_HIS_TBL_FORCED).getValue();
        final Label actionLabel = new LabelBuilder().name("").buildCaptionLabel();
        actionLabel.setContentMode(ContentMode.HTML);
        actionLabel.setStyleName("action-history-table-col-forced-label");
        if (actionWithActiveStatus != null && actionWithActiveStatus.getActionType() == ActionType.FORCED) {
            actionLabel.setValue(FontAwesome.BOLT.getHtml());
            // setted Id for Forced.
            actionLabel.setId("action.history.table.forcedId");
        } else if (actionWithActiveStatus != null && actionWithActiveStatus.getActionType() == ActionType.TIMEFORCED) {
            return actionLabelWithTimeForceIcon(actionWithActiveStatus, actionLabel);
        }
        return actionLabel;
    }

    private Component getActiveColumn(final Object itemId) {
        final Action.Status status = (Action.Status) hierarchicalContainer.getItem(itemId)
                .getItemProperty(SPUIDefinitions.ACTION_HIS_TBL_STATUS_HIDDEN).getValue();
        String activeValue;
        if (status == Action.Status.SCHEDULED) {
            activeValue = Action.Status.SCHEDULED.toString().toLowerCase();
        } else {
            activeValue = (String) hierarchicalContainer.getItem(itemId)
                    .getItemProperty(SPUIDefinitions.ACTION_HIS_TBL_ACTIVE_HIDDEN).getValue();
        }
        final String distName = (String) hierarchicalContainer.getItem(itemId)
                .getItemProperty(SPUIDefinitions.ACTION_HIS_TBL_DIST).getValue();
        final Label activeStatusIcon = createActiveStatusLabel(activeValue,
                (Action.Status) hierarchicalContainer.getItem(itemId)
                        .getItemProperty(SPUIDefinitions.ACTION_HIS_TBL_STATUS_HIDDEN)
                        .getValue() == Action.Status.ERROR);
        activeStatusIcon.setId(new StringJoiner(".").add(distName).add(itemId.toString())
                .add(SPUIDefinitions.ACTION_HIS_TBL_ACTIVE).add(activeValue).toString());
        return activeStatusIcon;
    }

    private HorizontalLayout createActionBarColumn(final Object itemId) {
        final HorizontalLayout actionBar = new HorizontalLayout();
        final Item item = hierarchicalContainer.getItem(itemId);
        final Long actionId = (Long) item.getItemProperty(SPUIDefinitions.ACTION_HIS_TBL_ACTION_ID_HIDDEN).getValue();
        final String activeValue = (String) item.getItemProperty(SPUIDefinitions.ACTION_HIS_TBL_ACTIVE_HIDDEN)
                .getValue();
        final Action actionWithActiveStatus = (Action) item.getItemProperty(SPUIDefinitions.ACTION_HIS_TBL_FORCED)
                .getValue();

        if (actionWithActiveStatus == null) {
            return null;
        }
        final boolean isActionActive = target != null && SPUIDefinitions.ACTIVE.equals(activeValue);

        final Button actionCancel = SPUIComponentProvider.getButton(
                UIComponentIdProvider.ACTION_HISTORY_TABLE_CANCEL_ID, "", i18n.get("message.cancel.action"),
                ValoTheme.BUTTON_TINY, true, FontAwesome.TIMES, SPUIButtonStyleSmallNoBorder.class);
        actionCancel.setEnabled(isActionActive && !actionWithActiveStatus.isCancelingOrCanceled());
        actionCancel.addClickListener(event -> confirmAndCancelAction(actionId));

        final Button actionForce = SPUIComponentProvider.getButton(UIComponentIdProvider.ACTION_HISTORY_TABLE_FORCE_ID,
                "", i18n.get("message.force.action"), ValoTheme.BUTTON_TINY, true, FontAwesome.BOLT,
                SPUIButtonStyleSmallNoBorder.class);
        actionForce.setEnabled(
                isActionActive && !actionWithActiveStatus.isForce() && !actionWithActiveStatus.isCancelingOrCanceled());
        actionForce.addClickListener(event -> confirmAndForceAction(actionId));

        final Button actionForceQuit = SPUIComponentProvider.getButton(
                UIComponentIdProvider.ACTION_HISTORY_TABLE_FORCE_QUIT_ID, "", i18n.get("message.forcequit.action"),
                ValoTheme.BUTTON_TINY + " redicon", true, FontAwesome.TIMES, SPUIButtonStyleSmallNoBorder.class);
        actionForceQuit.setEnabled(isActionActive && actionWithActiveStatus.isCancelingOrCanceled());
        actionForceQuit.addClickListener(event -> confirmAndForceQuitAction(actionId));

        actionBar.addComponents(actionCancel, actionForce, actionForceQuit);

        return actionBar;
    }

    private Component getStatusColumn(final Object itemId) {
        final Action.Status status = (Action.Status) hierarchicalContainer.getItem(itemId)
                .getItemProperty(SPUIDefinitions.ACTION_HIS_TBL_STATUS_HIDDEN).getValue();
        return getStatusIcon(status);
    }

    /**
     * Load the rows of previous status history of the selected action row and
     * add it next to the selected action row.
     *
     * @param parentRowIdx
     *            index of the selected action row.
     */
    @SuppressWarnings("unchecked")
    private void expandParentActionRow(final Object parentRowIdx) {
        /* Get the item for which the expand is made */
        final Item item = hierarchicalContainer.getItem(parentRowIdx);
        if (null != item) {
            final Long actionId = (Long) item.getItemProperty(SPUIDefinitions.ACTION_HIS_TBL_ACTION_ID_HIDDEN)
                    .getValue();

            final org.eclipse.hawkbit.repository.model.Action action = deploymentManagement
                    .findActionWithDetails(actionId);
            final Pageable pageReq = new PageRequest(0, 1000,
                    new Sort(Direction.DESC, ActionStatusFields.ID.getFieldName()));
            final Page<ActionStatus> actionStatusList;
            if (managementUIState.isActionHistoryMaximized()) {
                actionStatusList = deploymentManagement.findActionStatusByActionWithMessages(pageReq, action);
            } else {
                actionStatusList = deploymentManagement.findActionStatusByAction(pageReq, action);
            }
            final List<ActionStatus> content = actionStatusList.getContent();
            /*
             * Since the recent action status and messages are already
             * displaying with parent item, check if more than one action status
             * available.
             */
            int childIdx = 1;
            for (final ActionStatus actionStatus : content) {
                final String childId = parentRowIdx + " -> " + childIdx;
                final Item childItem = hierarchicalContainer.addItem(childId);
                if (null != childItem) {
                    /*
                     * For better UI, no need to display active/inactive icon
                     * for child items.
                     */
                    childItem.getItemProperty(SPUIDefinitions.ACTION_HIS_TBL_ACTIVE_HIDDEN).setValue("");

                    childItem.getItemProperty(SPUIDefinitions.ACTION_HIS_TBL_DIST).setValue(
                            action.getDistributionSet().getName() + ":" + action.getDistributionSet().getVersion());

                    childItem.getItemProperty(SPUIDefinitions.ACTION_HIS_TBL_DATETIME)
                            .setValue(SPDateTimeUtil.getFormattedDate(actionStatus.getCreatedAt()));
                    childItem.getItemProperty(SPUIDefinitions.ACTION_HIS_TBL_STATUS_HIDDEN)
                            .setValue(actionStatus.getStatus());
                    showOrHideMessage(childItem, actionStatus);
                    /* No further child items allowed for the child items */
                    ((Hierarchical) hierarchicalContainer).setChildrenAllowed(childId, false);
                    /* Assign this childItem to the parent */
                    ((Hierarchical) hierarchicalContainer).setParent(childId, parentRowIdx);
                    childIdx++;

                }
            }
        }
    }

    /**
     * Hide the rows of previous status history of the selected action row.
     *
     * @param parentRowIdx
     *            index of the selected action row.
     */
    public void collapseParentActionRow(final Object parentRowIdx) {
        /* Remove all child items for the clear the memory. */
        final Collection<?> children = ((Hierarchical) hierarchicalContainer).getChildren(parentRowIdx);
        if (children != null && !children.isEmpty()) {
            String ids = children.toString().substring(1);
            ids = ids.substring(0, ids.length() - 1);
            for (final String childId : ids.split(", ")) {
                ((Hierarchical) hierarchicalContainer).removeItem(childId);
            }
        }
    }

    /**
     * Get status icon.
     *
     * @param status
     *            as Status
     * @return Label as UI
     */
    private Label getStatusIcon(final Action.Status status) {
        final Label label = new LabelBuilder().name("").buildLabel();
        final String statusIconPending = "statusIconPending";
        label.setContentMode(ContentMode.HTML);
        if (Action.Status.FINISHED == status) {
            label.setDescription(i18n.get("label.finished"));
            label.setStyleName(STATUS_ICON_GREEN);
            label.setValue(FontAwesome.CHECK_CIRCLE.getHtml());
        } else if (Action.Status.ERROR == status) {
            label.setDescription(i18n.get("label.error"));
            label.setStyleName("statusIconRed");
            label.setValue(FontAwesome.EXCLAMATION_CIRCLE.getHtml());
        } else if (Action.Status.WARNING == status) {
            label.setStyleName("statusIconOrange");
            label.setDescription(i18n.get("label.warning"));
            label.setValue(FontAwesome.EXCLAMATION_CIRCLE.getHtml());
        } else if (Action.Status.RUNNING == status) {
            // dynamic spinner
            label.setStyleName(statusIconPending);
            label.setDescription(i18n.get("label.running"));
            label.setValue(FontAwesome.ADJUST.getHtml());
        } else if (Action.Status.CANCELING == status) {
            label.setStyleName(statusIconPending);
            label.setDescription(i18n.get("label.cancelling"));
            label.setValue(FontAwesome.TIMES_CIRCLE.getHtml());
        } else if (Action.Status.CANCELED == status) {
            label.setStyleName(statusIconPending);
            label.setDescription(i18n.get("label.cancelled"));
            label.setStyleName(STATUS_ICON_GREEN);
            label.setValue(FontAwesome.TIMES_CIRCLE.getHtml());
        } else if (Action.Status.RETRIEVED == status) {
            label.setStyleName(statusIconPending);
            label.setDescription(i18n.get("label.retrieved"));
            label.setValue(FontAwesome.CIRCLE_O.getHtml());
        } else if (Action.Status.DOWNLOAD == status) {
            label.setStyleName(statusIconPending);
            label.setDescription(i18n.get("label.download"));
            label.setValue(FontAwesome.CLOUD_DOWNLOAD.getHtml());
        } else if (Action.Status.SCHEDULED == status) {
            label.setStyleName(statusIconPending);
            label.setDescription(i18n.get("label.scheduled"));
            label.setValue(FontAwesome.HOURGLASS_1.getHtml());
        } else {
            label.setDescription("");
            label.setValue("");
        }
        return label;

    }

    // to show forced Icon
    private Component actionLabelWithTimeForceIcon(final Action actionWithActiveStatus, final Label actionLabel) {
        final long currentTimeMillis = System.currentTimeMillis();

        final HorizontalLayout hLayout = new HorizontalLayout();
        final Label autoForceLabel = new LabelBuilder().name("").id("action.history.table.timedforceId").buildLabel();

        actionLabel.setValue(FontAwesome.BOLT.getHtml());
        autoForceLabel.setContentMode(ContentMode.HTML);
        autoForceLabel.setValue(FontAwesome.HISTORY.getHtml());

        hLayout.addComponent(actionLabel);
        hLayout.addComponent(autoForceLabel);

        if (actionWithActiveStatus.isHitAutoForceTime(currentTimeMillis)) {
            autoForceLabel.setDescription("autoforced");
            autoForceLabel.setStyleName(STATUS_ICON_GREEN);
            autoForceLabel.setDescription("auto forced since " + SPDateTimeUtil
                    .getDurationFormattedString(actionWithActiveStatus.getForcedTime(), currentTimeMillis, i18n));
        } else {
            autoForceLabel.setDescription("auto forcing in " + SPDateTimeUtil
                    .getDurationFormattedString(currentTimeMillis, actionWithActiveStatus.getForcedTime(), i18n));
            autoForceLabel.setStyleName("statusIconPending");
            autoForceLabel.setValue(FontAwesome.HISTORY.getHtml());
        }
        return hLayout;
    }

    /**
     * Create Status Label.
     *
     * @param activeValue
     *            as String
     * @return Labeal as UI
     */
    private static Label createActiveStatusLabel(final String activeValue, final boolean endedWithError) {
        final Label label = new LabelBuilder().name("").buildLabel();
        label.setContentMode(ContentMode.HTML);
        if (SPUIDefinitions.SCHEDULED.equals(activeValue)) {
            label.setDescription("Scheduled");
            label.setValue(FontAwesome.HOURGLASS_1.getHtml());
        } else if (SPUIDefinitions.ACTIVE.equals(activeValue)) {
            label.setDescription("Active");
            label.setStyleName("statusIconActive");
        } else if (SPUIDefinitions.IN_ACTIVE.equals(activeValue)) {
            if (endedWithError) {
                label.setStyleName("statusIconRed");
            } else {
                label.setStyleName("statusIconNeutral");
            }
            label.setDescription("In-active");
            label.setValue(FontAwesome.CHECK_CIRCLE.getHtml());
        } else {
            label.setValue("");
        }
        return label;
    }

    /**
     * Creates full sized window for action history with message.
     */
    private void createTableContentForMax() {
        setColumnCollapsingAllowed(true);
        if (!alreadyHasMessages) {
            /*
             * check to avoid DB call for fetching the messages again and again
             * if already available in container.
             */
            getcontainerData();
            // to expand parent row , if already expanded.
            expandParentRow();
            alreadyHasMessages = true;
        }

        addGeneratedColumn(SPUIDefinitions.ACTION_HIS_TBL_MSGS, new Table.ColumnGenerator() {
            private static final long serialVersionUID = 1L;

            @SuppressWarnings("unchecked")
            @Override
            public Component generateCell(final Table source, final Object itemId, final Object columnId) {
                final List<String> messages = (List<String>) hierarchicalContainer.getItem(itemId)
                        .getItemProperty(SPUIDefinitions.ACTION_HIS_TBL_MSGS_HIDDEN).getValue();
                return createMessagesBlock(messages);
            }
        });
        setVisibleColumns(getVisbleColumns().toArray());
        setColumnExpantRatioOnTableMaximize();
    }

    private void setColumnExpantRatioOnTableMaximize() {
        /* set messages column can expand the rest of the available space */
        setColumnExpandRatio(SPUIDefinitions.ACTION_HIS_TBL_ACTIVE, 0.1F);
        setColumnExpandRatio(SPUIDefinitions.ACTION_HIS_TBL_ACTION_ID, 0.1F);
        setColumnExpandRatio(SPUIDefinitions.ACTION_HIS_TBL_STATUS, 0.1F);
        setColumnExpandRatio(SPUIDefinitions.ACTION_HIS_TBL_DIST, 0.2F);
        setColumnExpandRatio(SPUIDefinitions.ACTION_HIS_TBL_FORCED, 0.1F);
        setColumnExpandRatio(SPUIDefinitions.ACTION_HIS_TBL_ROLLOUT_NAME, 0.1F);
        setColumnExpandRatio(SPUIDefinitions.ACTION_HIS_TBL_MSGS, 0.35F);
        setColumnExpandRatio(SPUIDefinitions.ACTION_HIS_TBL_DATETIME, 0.15F);
        setColumnExpandRatio(SPUIDefinitions.ACTIONS_COLUMN, 0.2F);
    }

    /**
     * create Message block for Actions.
     *
     * @param messages
     *            as List of msg
     * @return Component as UI
     */
    protected Component createMessagesBlock(final List<String> messages) {

        final TextArea textArea = new TextArea();
        textArea.addStyleName(ValoTheme.TEXTAREA_BORDERLESS);
        textArea.addStyleName(ValoTheme.TEXTAREA_TINY);
        textArea.addStyleName("inline-icon");
        textArea.setSizeFull();
        int index = 1;
        final StringBuilder updateStatusMessages = new StringBuilder();
        if (messages != null && !messages.isEmpty()) {
            /* Messages are available */
            for (final String msg : messages) {
                updateStatusMessages.append('[').append(index).append("]: ").append(msg).append('\n');
                index++;
            }
        } else {
            /* Messages are not available */
            updateStatusMessages.append(i18n.get("message.no.available"));
        }
        textArea.setValue(updateStatusMessages.toString());
        textArea.setReadOnly(Boolean.TRUE);
        return textArea;
    }

    /**
     * Show or hide message.
     *
     * @param actionHistoryMode
     *            {@link ActionHisTableType}
     * @param childItem
     *            {@link Item}
     * @param actionStatus
     *            {@link ActionHisTableType}
     */
    @SuppressWarnings("unchecked")
    private void showOrHideMessage(final Item childItem, final ActionStatus actionStatus) {
        if (managementUIState.isActionHistoryMaximized()) {
            childItem.getItemProperty(SPUIDefinitions.ACTION_HIS_TBL_MSGS_HIDDEN).setValue(actionStatus.getMessages());
        }
    }

    /**
     * View History Table in normal window mode.
     */
    private void normalActionHistoryTable() {
        setColumnCollapsingAllowed(false);
        managementUIState.setActionHistoryMaximized(false);
        removeGeneratedColumn(SPUIDefinitions.ACTION_HIS_TBL_MSGS);
        setVisibleColumns(getVisbleColumns().toArray());
        setColumnExpandRatioForMinimisedTable();
    }

    /**
     * Show confirmation window and if ok then only, force the action.
     *
     * @param actionId
     *            as Id if the action needs to be forced.
     */
    private void confirmAndForceAction(final Long actionId) {
        /* Display the confirmation */
        final ConfirmationDialog confirmDialog = new ConfirmationDialog(i18n.get("caption.force.action.confirmbox"),
                i18n.get("message.force.action.confirm"), i18n.get(BUTTON_OK), i18n.get(BUTTON_CANCEL), ok -> {
                    if (!ok) {
                        return;
                    }
                    deploymentManagement.forceTargetAction(actionId);
                    populateAndupdateTargetDetails(target);
                    notification.displaySuccess(i18n.get("message.force.action.success"));
                });
        UI.getCurrent().addWindow(confirmDialog.getWindow());

        confirmDialog.getWindow().bringToFront();
    }

    private void confirmAndForceQuitAction(final Long actionId) {
        /* Display the confirmation */
        final ConfirmationDialog confirmDialog = new ConfirmationDialog(i18n.get("caption.forcequit.action.confirmbox"),
                i18n.get("message.forcequit.action.confirm"), i18n.get(BUTTON_OK), i18n.get(BUTTON_CANCEL), ok -> {
                    if (!ok) {
                        return;
                    }
                    final boolean cancelResult = forceQuitActiveAction(actionId);
                    if (cancelResult) {
                        populateAndupdateTargetDetails(target);
                        notification.displaySuccess(i18n.get("message.forcequit.action.success"));
                    } else {
                        notification.displayValidationError(i18n.get("message.forcequit.action.failed"));
                    }
                }, FontAwesome.WARNING);
        UI.getCurrent().addWindow(confirmDialog.getWindow());

        confirmDialog.getWindow().bringToFront();
    }

    /**
     * Show confirmation window and if ok then only, cancel the action.
     *
     * @param actionId
     *            as Id if the action needs to be cancelled.
     */
    private void confirmAndCancelAction(final Long actionId) {
        if (actionId == null) {
            return;
        }

        final ConfirmationDialog confirmDialog = new ConfirmationDialog(i18n.get("caption.cancel.action.confirmbox"),
                i18n.get("message.cancel.action.confirm"), i18n.get(BUTTON_OK), i18n.get(BUTTON_CANCEL), ok -> {
                    if (!ok) {
                        return;
                    }
                    final boolean cancelResult = cancelActiveAction(actionId);
                    if (cancelResult) {
                        populateAndupdateTargetDetails(target);
                        notification.displaySuccess(i18n.get("message.cancel.action.success"));
                    } else {
                        notification.displayValidationError(i18n.get("message.cancel.action.failed"));
                    }
                });
        UI.getCurrent().addWindow(confirmDialog.getWindow());
        confirmDialog.getWindow().bringToFront();
    }

    private void populateAndupdateTargetDetails(final Target target) {
        // show the updated target action history details
        populateTableData(target);
        // update the target table and its pinning details
        updateTargetAndDsTable();
    }

    // service call to cancel the active action
    private boolean cancelActiveAction(final Long actionId) {
        if (actionId != null) {
            final Action activeAction = deploymentManagement.findAction(actionId);
            try {
                deploymentManagement.cancelAction(activeAction, target);
                return true;
            } catch (final CancelActionNotAllowedException e) {
                LOG.info("Cancel action not allowed exception :{}", e);
                return false;
            }
        }
        return false;
    }

    // service call to cancel the active action
    private boolean forceQuitActiveAction(final Long actionId) {
        if (actionId != null) {
            final Action activeAction = deploymentManagement.findAction(actionId);
            try {
                deploymentManagement.forceQuitAction(activeAction);
                return true;
            } catch (final CancelActionNotAllowedException e) {
                LOG.info("Force Cancel action not allowed exception :{}", e);
                return false;
            }
        }
        return false;
    }

    private void updateTargetAndDsTable() {
        eventBus.publish(this, new TargetTableEvent(BaseEntityEventType.UPDATED_ENTITY, target));
        updateDistributionTableStyle();
    }

    /**
     * Update the colors of Assigned and installed distribution set in Target
     * Pinning.
     */
    private void updateDistributionTableStyle() {

        if (managementUIState.getDistributionTableFilters().getPinnedTargetId().isPresent()
                && null != managementUIState.getDistributionTableFilters().getPinnedTargetId().get()) {
            final String alreadyPinnedControllerId = managementUIState.getDistributionTableFilters().getPinnedTargetId()
                    .get();
            // if the current target is pinned publish a pin event again
            if (null != alreadyPinnedControllerId && alreadyPinnedControllerId.equals(target.getControllerId())) {
                eventBus.publish(this, PinUnpinEvent.PIN_TARGET);
            }
        }
    }

    /**
     * empty the container when there is no data selected in Target Table.
     */
    public void clearContainerData() {
        getContainerDataSource().removeAllItems();
    }

    /**
     * Set messages false.
     *
     * @param alreadyHasMessages
     *            the alreadyHasMessages to set
     */
    public void setAlreadyHasMessages(final boolean alreadyHasMessages) {
        this.alreadyHasMessages = alreadyHasMessages;
    }

    private void restorePreviousState() {
        if (managementUIState.isActionHistoryMaximized()) {
            createTableContentForMax();
        }
    }

    private void expandParentRow() {
        if (null != managementUIState.getExpandParentActionRowId()
                && !managementUIState.getExpandParentActionRowId().isEmpty()) {
            for (final Object obj : managementUIState.getExpandParentActionRowId()) {
                expandParentActionRow(obj);
            }
        }
    }
}
