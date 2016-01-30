/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.rollout;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.eclipse.hawkbit.eventbus.event.RolloutChangeEvent;
import org.eclipse.hawkbit.repository.RolloutManagement;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.Rollout.RolloutStatus;
import org.eclipse.hawkbit.repository.model.TotalTargetCountStatus;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleSmallNoBorder;
import org.eclipse.hawkbit.ui.rollout.event.RolloutEvent;
import org.eclipse.hawkbit.ui.rollout.state.RolloutUIState;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.SPUIComponetIdProvider;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.eclipse.hawkbit.ui.utils.TableColumn;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.addons.lazyquerycontainer.BeanQueryFactory;
import org.vaadin.addons.lazyquerycontainer.LazyQueryContainer;
import org.vaadin.addons.lazyquerycontainer.LazyQueryDefinition;
import org.vaadin.alump.distributionbar.DistributionBar;
import org.vaadin.peter.contextmenu.ContextMenu;
import org.vaadin.peter.contextmenu.ContextMenu.ContextMenuItem;
import org.vaadin.peter.contextmenu.ContextMenu.ContextMenuItemClickEvent;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.vaadin.data.Container;
import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.server.FontAwesome;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;

/**
 * 
 * Rollout table in list view.
 *
 */
@SpringComponent
@ViewScope
public class RolloutListTable extends AbstractSimpleTable {

    private static final long serialVersionUID = 8141874975649180139L;

    @Autowired
    private I18N i18n;

    @Autowired
    private transient EventBus.SessionEventBus eventBus;

    @Autowired
    private transient RolloutManagement rolloutManagement;

    @Autowired
    private AddUpdateRolloutWindowLayout addUpdateRolloutWindow;

    @Autowired
    private UINotification uiNotification;

    @Autowired
    private transient RolloutUIState rolloutUIState;

    @Override
    @PostConstruct
    protected void init() {
        super.init();
        eventBus.subscribe(this);
    }

    @PreDestroy
    void destroy() {
        eventBus.unsubscribe(this);
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final RolloutEvent event) {
        if (event == RolloutEvent.FILTER_BY_TEXT || event == RolloutEvent.CREATE_ROLLOUT
                || event == RolloutEvent.UPDATE_ROLLOUT || event == RolloutEvent.SHOW_ROLLOUTS) {
            refreshTable();
        }
    }

    /**
     * Handles the RolloutChangeEvent to refresh the item in the table.
     * 
     * @param rolloutChangeEvent
     *            the event which contains the rollout which has been changed
     */
    @EventBusListenerMethod(scope = EventScope.SESSION)
    public void onEvent(final RolloutChangeEvent rolloutChangeEvent) {
        final List<Object> visibleItemIds = (List<Object>) getVisibleItemIds();
        if (visibleItemIds.contains(rolloutChangeEvent.getRolloutId())) {
            final Rollout rollout = rolloutManagement.getRolloutDetailedStatus(rolloutChangeEvent.getRolloutId());
            final TotalTargetCountStatus totalTargetCountStatus = rollout.getTotalTargetCountStatus();
            final LazyQueryContainer rolloutContainer = (LazyQueryContainer) getContainerDataSource();
            final Item item = rolloutContainer.getItem(rolloutChangeEvent.getRolloutId());
            item.getItemProperty(SPUILabelDefinitions.VAR_STATUS).setValue(rollout.getStatus());
            item.getItemProperty(SPUILabelDefinitions.VAR_COUNT_TARGETS_RUNNING)
                    .setValue(totalTargetCountStatus.getTotalTargetCountByStatus(TotalTargetCountStatus.Status.RUNNING));
            item.getItemProperty(SPUILabelDefinitions.VAR_COUNT_TARGETS_ERROR)
                    .setValue(totalTargetCountStatus.getTotalTargetCountByStatus(TotalTargetCountStatus.Status.ERROR));
            item.getItemProperty(SPUILabelDefinitions.VAR_COUNT_TARGETS_FINISHED)
                    .setValue(totalTargetCountStatus.getTotalTargetCountByStatus(TotalTargetCountStatus.Status.FINISHED));
            item.getItemProperty(SPUILabelDefinitions.VAR_COUNT_TARGETS_NOT_STARTED)
                    .setValue(totalTargetCountStatus.getTotalTargetCountByStatus(TotalTargetCountStatus.Status.NOTSTARTED));
            item.getItemProperty(SPUILabelDefinitions.VAR_COUNT_TARGETS_CANCELLED)
                    .setValue(totalTargetCountStatus.getTotalTargetCountByStatus(TotalTargetCountStatus.Status.CANCELLED));
            item.getItemProperty(SPUILabelDefinitions.VAR_COUNT_TARGETS_SCHEDULED)
                    .setValue(totalTargetCountStatus.getTotalTargetCountByStatus(TotalTargetCountStatus.Status.SCHEDULED));
            item.getItemProperty("isActionRecieved")
                    .setValue(!(Boolean) item.getItemProperty("isActionRecieved").getValue());
        }
    }

    @Override
    protected List<TableColumn> getTableVisibleColumns() {
        final List<TableColumn> columnList = new ArrayList<TableColumn>();
        columnList.add(new TableColumn(SPUIDefinitions.ROLLOUT_NAME, i18n.get("header.name"), 0.25f));

        columnList.add(new TableColumn(SPUILabelDefinitions.VAR_DIST_NAME_VERSION, i18n.get("header.distributionset"),
                0.2f));
        columnList.add(new TableColumn(SPUILabelDefinitions.VAR_NUMBER_OF_GROUPS, i18n.get("header.numberofgroups"),
                0.2f));

        columnList.add(new TableColumn(SPUILabelDefinitions.VAR_DESC, i18n.get("header.description"), 0.15f));
        columnList
                .add(new TableColumn(SPUILabelDefinitions.VAR_TOTAL_TARGETS, i18n.get("header.total.targets"), 0.13f));
        columnList.add(new TableColumn(SPUIDefinitions.ROLLOUT_STATUS, i18n.get("header.status"), 0.07f));
        columnList.add(new TableColumn(SPUIDefinitions.DETAIL_STATUS, i18n.get("header.detail.status"), 0.3f));

        columnList.add(new TableColumn(SPUIDefinitions.ROLLOUT_ACTION, i18n.get("upload.action"), 0.1f));

        columnList.add(new TableColumn(SPUILabelDefinitions.VAR_CREATED_DATE, i18n.get("header.createdDate"), 0.2f));
        columnList.add(new TableColumn(SPUILabelDefinitions.VAR_CREATED_USER, i18n.get("header.createdBy"), 0.2f));
        columnList.add(new TableColumn(SPUILabelDefinitions.VAR_MODIFIED_DATE, i18n.get("header.modifiedDate"), 0.2f));
        columnList.add(new TableColumn(SPUILabelDefinitions.VAR_MODIFIED_BY, i18n.get("header.modifiedBy"), 0.2f));

        return columnList;
    }

    @Override
    protected Container createContainer() {
        final BeanQueryFactory<RolloutBeanQuery> rolloutQf = new BeanQueryFactory<RolloutBeanQuery>(
                RolloutBeanQuery.class);
        final LazyQueryContainer rolloutTableContainer = new LazyQueryContainer(new LazyQueryDefinition(true,
                SPUIDefinitions.PAGE_SIZE, SPUILabelDefinitions.VAR_ID), rolloutQf);
        return rolloutTableContainer;

    }

    @Override
    protected void addContainerProperties(final Container container) {
        final LazyQueryContainer rolloutTableContainer = (LazyQueryContainer) container;
        rolloutTableContainer.addContainerProperty(SPUILabelDefinitions.VAR_ID, String.class, null, false, false);
        rolloutTableContainer.addContainerProperty(SPUILabelDefinitions.VAR_NAME, String.class, "", false, false);
        rolloutTableContainer.addContainerProperty(SPUILabelDefinitions.VAR_DESC, String.class, null, false, false);
        rolloutTableContainer.addContainerProperty(SPUILabelDefinitions.VAR_STATUS, RolloutStatus.class, null, false,
                false);
        rolloutTableContainer.addContainerProperty(SPUILabelDefinitions.VAR_DIST_NAME_VERSION, String.class, null,
                false, false);

        // TODO display filter query name
        // TODO display started date
        rolloutTableContainer.addContainerProperty(SPUILabelDefinitions.VAR_TARGETFILTERQUERY, String.class, null,
                false, false);
        rolloutTableContainer.addContainerProperty(SPUILabelDefinitions.VAR_CREATED_DATE, String.class, null, false,
                false);

        rolloutTableContainer.addContainerProperty(SPUILabelDefinitions.VAR_MODIFIED_DATE, String.class, null, false,
                false);
        rolloutTableContainer.addContainerProperty(SPUILabelDefinitions.VAR_CREATED_USER, String.class, null, false,
                false);
        rolloutTableContainer.addContainerProperty(SPUILabelDefinitions.VAR_MODIFIED_BY, String.class, null, false,
                false);
        rolloutTableContainer.addContainerProperty(SPUILabelDefinitions.VAR_NUMBER_OF_GROUPS, Integer.class, null,
                false, false);

        rolloutTableContainer.addContainerProperty(SPUILabelDefinitions.VAR_COUNT_TARGETS_NOT_STARTED, Long.class, 0L,
                false, false);
        rolloutTableContainer.addContainerProperty(SPUILabelDefinitions.VAR_COUNT_TARGETS_RUNNING, Long.class, 0L,
                false, false);
        rolloutTableContainer.addContainerProperty(SPUILabelDefinitions.VAR_COUNT_TARGETS_SCHEDULED, Long.class, 0L,
                false, false);
        rolloutTableContainer.addContainerProperty(SPUILabelDefinitions.VAR_COUNT_TARGETS_ERROR, Long.class, 0L, false,
                false);
        rolloutTableContainer.addContainerProperty(SPUILabelDefinitions.VAR_COUNT_TARGETS_FINISHED, Long.class, 0L,
                false, false);
        rolloutTableContainer.addContainerProperty(SPUILabelDefinitions.VAR_COUNT_TARGETS_CANCELLED, Long.class, 0L,
                false, false);
        rolloutTableContainer.addContainerProperty(SPUILabelDefinitions.VAR_TOTAL_TARGETS, String.class, "0", false,
                false);

    }

    @Override
    protected String getTableId() {
        return SPUIComponetIdProvider.ROLLOUT_LIST_TABLE_ID;
    }

    @Override
    protected void onValueChange() {
        /**
         * No implementation required.
         */
    }

    @Override
    protected void addCustomGeneratedColumns() {
        addGeneratedColumn(SPUIDefinitions.ROLLOUT_NAME, (source, itemId, columnId) -> getRolloutNameLink(itemId));
        addGeneratedColumn(SPUIDefinitions.ROLLOUT_STATUS, (source, itemId, columnId) -> getStatusLabel(itemId));
        addGeneratedColumn(SPUIDefinitions.DETAIL_STATUS, (source, itemId, columnId) -> getProgressBar(itemId));
        addGeneratedColumn(SPUIDefinitions.ROLLOUT_ACTION, (source, itemId, columnId) -> getActionButton(itemId));

        setColumnAlignment(SPUIDefinitions.ROLLOUT_STATUS, Align.CENTER);
        setColumnAlignment(SPUIDefinitions.DETAIL_STATUS, Align.CENTER);
        setColumnAlignment(SPUIDefinitions.ROLLOUT_ACTION, Align.CENTER);

    }

    @Override
    protected void setCollapsiblecolumns() {
        setColumnCollapsed(SPUILabelDefinitions.VAR_CREATED_DATE, true);
        setColumnCollapsed(SPUILabelDefinitions.VAR_MODIFIED_DATE, true);
        setColumnCollapsed(SPUILabelDefinitions.VAR_CREATED_USER, true);
        setColumnCollapsed(SPUILabelDefinitions.VAR_MODIFIED_BY, true);
    }

    private Button getActionButton(final Object itemId) {
        final Item row = getItem(itemId);
        final String rolloutName = (String) row.getItemProperty(SPUILabelDefinitions.VAR_NAME).getValue();
        final RolloutStatus rolloutStatus = (RolloutStatus) row.getItemProperty(SPUILabelDefinitions.VAR_STATUS)
                .getValue();
        final Button actionButton = SPUIComponentProvider.getButton(getActionButtonId(rolloutName), "",
                SPUILabelDefinitions.ACTION, ValoTheme.BUTTON_TINY + " ", true, FontAwesome.CIRCLE_O,
                SPUIButtonStyleSmallNoBorder.class);
        actionButton.setData(itemId);
        actionButton.setHtmlContentAllowed(true);
        if (rolloutStatus == RolloutStatus.FINISHED) {
            actionButton.setEnabled(false);
        }
        actionButton.addClickListener(event -> onAction(event));
        return actionButton;
    }

    private ContextMenu createContextMenu(final Long rolloutId) {
        final Item row = getItem(rolloutId);
        final RolloutStatus rolloutStatus = (RolloutStatus) row.getItemProperty(SPUILabelDefinitions.VAR_STATUS)
                .getValue();
        final ContextMenu context = new ContextMenu();
        if (rolloutStatus == RolloutStatus.READY) {
            final ContextMenuItem startItem = context.addItem("Start");
            startItem.setData(new ContextMenuData(rolloutId, ACTION.START));
        } else if (rolloutStatus == RolloutStatus.RUNNING) {
            final ContextMenuItem pauseItem = context.addItem("Pause");
            pauseItem.setData(new ContextMenuData(rolloutId, ACTION.PAUSE));
        } else if (rolloutStatus == RolloutStatus.PAUSED) {
            final ContextMenuItem resumeItem = context.addItem("Resume");
            resumeItem.setData(new ContextMenuData(rolloutId, ACTION.RESUME));
        }

        final ContextMenuItem cancelItem = context.addItem("Update");
        cancelItem.setData(new ContextMenuData(rolloutId, ACTION.UPDATE));
        context.addItemClickListener(event -> menuItemClicked(event));

        return context;
    }

    private void menuItemClicked(final ContextMenuItemClickEvent event) {
        final ContextMenuItem item = (ContextMenuItem) event.getSource();
        final ContextMenuData contextMenuData = (ContextMenuData) item.getData();
        final Item row = getItem(contextMenuData.getRolloutId());
        final String rolloutName = (String) row.getItemProperty(SPUILabelDefinitions.VAR_NAME).getValue();

        if (contextMenuData.getAction() == ACTION.PAUSE) {
            rolloutManagement.pauseRollout(rolloutManagement.findRolloutById(contextMenuData.getRolloutId()));
            uiNotification.displaySuccess(i18n.get("message.rollout.paused", rolloutName));
        } else if (contextMenuData.getAction() == ACTION.RESUME) {
            rolloutManagement.resumeRollout(rolloutManagement.findRolloutById(contextMenuData.getRolloutId()));
            uiNotification.displaySuccess(i18n.get("message.rollout.resumed", rolloutName));
        } else if (contextMenuData.getAction() == ACTION.START) {
            rolloutManagement.startRolloutAsync(rolloutManagement.findRolloutByName(rolloutName));
            uiNotification.displaySuccess(i18n.get("message.rollout.started", rolloutName));
        } else if (contextMenuData.getAction() == ACTION.UPDATE) {
            addUpdateRolloutWindow.populateData(contextMenuData.getRolloutId());
            final Window addTargetWindow = addUpdateRolloutWindow.getWindow();
            addTargetWindow.setCaption(i18n.get("caption.update.rollout"));
            UI.getCurrent().addWindow(addTargetWindow);
            addTargetWindow.setVisible(Boolean.TRUE);
        }
    }

    private void onAction(final ClickEvent event) {
        final ContextMenu contextMenu = createContextMenu((Long) event.getButton().getData());
        contextMenu.setAsContextMenuOf(event.getButton());
        contextMenu.open(event.getClientX(), event.getClientY());
    }

    private Button getRolloutNameLink(final Object itemId) {
        final Item row = getItem(itemId);
        final String rolloutName = (String) row.getItemProperty(SPUILabelDefinitions.VAR_NAME).getValue();
        final Button updateIcon = SPUIComponentProvider.getButton(getDetailLinkId(rolloutName), rolloutName,
                SPUILabelDefinitions.SHOW_ROLLOUT_GROUP_DETAILS, null, false, null, SPUIButtonStyleSmallNoBorder.class);
        updateIcon.setData(rolloutName);
        updateIcon.addStyleName(ValoTheme.LINK_SMALL + " " + "on-focus-no-border link");
        updateIcon.addClickListener(event -> showRolloutGroups(itemId));
        return updateIcon;
    }

    private void showRolloutGroups(final Object itemId) {
        rolloutUIState.setRolloutId((long) itemId);
        final String rolloutName = (String) getItem(itemId).getItemProperty(SPUILabelDefinitions.VAR_NAME).getValue();
        rolloutUIState.setRolloutName(rolloutName);
        eventBus.publish(this, RolloutEvent.SHOW_ROLLOUT_GROUPS);
    }

    private static String getActionButtonId(final String rollOutName) {
        return new StringBuilder(SPUIComponetIdProvider.ROLLOUT_ACTION_BUTTON_ID).append('.').append(rollOutName)
                .toString();
    }

    private static String getDetailLinkId(final String rollOutName) {
        return new StringBuilder(SPUIComponetIdProvider.ROLLOUT_NAME_LINK_ID).append('.').append(rollOutName)
                .toString();
    }

    private DistributionBar getProgressBar(final Object itemId) {
        final DistributionBar bar = new DistributionBar(2);
        bar.setSizeFull();
        bar.setZeroSizedVisible(false);
        HawkbitCommonUtil.initialiseProgressBar(bar, getItem(itemId));
        addPropertyChangeListenerOnActionRecieved(itemId, bar);
        return bar;
    }

    private void addPropertyChangeListenerOnActionRecieved(final Object itemId, final DistributionBar bar) {
        final Property status = getContainerProperty(itemId, "isActionRecieved");
        final Property.ValueChangeNotifier notifier = (Property.ValueChangeNotifier) status;
        notifier.addValueChangeListener(new ValueChangeListener() {
            @Override
            public void valueChange(final com.vaadin.data.Property.ValueChangeEvent event) {
                HawkbitCommonUtil.setProgressBarDetails(bar, getItem(itemId));
            }
        });
    }

    private Label getStatusLabel(final Object itemId) {
        final Label statusLabel = new Label();
        statusLabel.setHeightUndefined();
        statusLabel.setContentMode(ContentMode.HTML);
        statusLabel.setId(getRolloutStatusId(itemId));
        setStatusIcon(itemId, statusLabel);
        statusLabel.setDescription(getDescription(itemId));
        statusLabel.setSizeUndefined();
        addPropertyChangeListener(itemId, statusLabel);
        return statusLabel;
    }

    private void addPropertyChangeListener(final Object itemId, final Label statusLabel) {
        final Property status = getContainerProperty(itemId, SPUILabelDefinitions.VAR_STATUS);
        final Property.ValueChangeNotifier notifier = (Property.ValueChangeNotifier) status;
        notifier.addValueChangeListener(new ValueChangeListener() {
            @Override
            public void valueChange(final com.vaadin.data.Property.ValueChangeEvent event) {
                setStatusIcon(itemId, statusLabel);
            }
        });
    }

    private String getRolloutStatusId(final Object itemId) {
        final String rolloutName = (String) getItem(itemId).getItemProperty(SPUILabelDefinitions.VAR_NAME).getValue();
        return new StringBuilder(SPUIComponetIdProvider.ROLLOUT_STATUS_LABEL_ID).append(".").append(rolloutName)
                .toString();
    }

    private String getDescription(final Object itemId) {
        final Item item = getItem(itemId);
        if (item != null) {
            final RolloutStatus rolloutStatus = (RolloutStatus) item.getItemProperty(SPUILabelDefinitions.VAR_STATUS)
                    .getValue();
            return rolloutStatus.toString().toLowerCase();
        }
        return null;
    }

    private void setStatusIcon(final Object itemId, final Label statusLabel) {
        final Item item = getItem(itemId);
        if (item != null) {
            final RolloutStatus rolloutStatus = (RolloutStatus) item.getItemProperty(SPUILabelDefinitions.VAR_STATUS)
                    .getValue();
            setRolloutStatusIcon(rolloutStatus, statusLabel);
        }
    }

    private void setRolloutStatusIcon(final RolloutStatus rolloutStatus, final Label statusLabel) {
        switch (rolloutStatus) {
        case FINISHED:
            statusLabel.setValue(FontAwesome.CHECK_CIRCLE.getHtml());
            statusLabel.setStyleName("statusIconGreen");
            break;
        case PAUSED:
            statusLabel.setValue(FontAwesome.PAUSE.getHtml());
            statusLabel.setStyleName("statusIconBlue");
            break;
        case RUNNING:
            statusLabel.setValue(FontAwesome.ADJUST.getHtml());
            statusLabel.setStyleName("statusIconYellow");
            break;
        case READY:
            statusLabel.setValue(FontAwesome.DOT_CIRCLE_O.getHtml());
            statusLabel.setStyleName("statusIconLightBlue");
            break;
        case STOPPED:
            statusLabel.setValue(FontAwesome.STOP.getHtml());
            statusLabel.setStyleName("statusIconRed");
            break;
        default:
            break;
        }
        statusLabel.addStyleName(ValoTheme.LABEL_SMALL);
    }

    private void refreshTable() {
        final LazyQueryContainer container = (LazyQueryContainer) getContainerDataSource();
        container.refresh();
    }

    enum ACTION {
        PAUSE, RESUME, START, UPDATE
    }

    /**
     * Represents data of context menu item.
     *
     */
    public static class ContextMenuData {

        private Long rolloutId;

        private ACTION action;

        /**
         * Set rollout if and action.
         * 
         * @param rolloutId
         *            id of rollout
         * @param action
         *            user action {@link ACTION}
         */
        public ContextMenuData(final Long rolloutId, final ACTION action) {
            this.action = action;
            this.rolloutId = rolloutId;
        }

        /**
         * @return the rolloutId
         */
        public Long getRolloutId() {
            return rolloutId;
        }

        /**
         * @param rolloutId
         *            the rolloutId to set
         */
        public void setRolloutId(final Long rolloutId) {
            this.rolloutId = rolloutId;
        }

        /**
         * @return the action
         */
        public ACTION getAction() {
            return action;
        }

        /**
         * @param action
         *            the action to set
         */
        public void setAction(final ACTION action) {
            this.action = action;
        }

    }

}
