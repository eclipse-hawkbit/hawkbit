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

import org.eclipse.hawkbit.eventbus.event.RolloutGroupChangeEvent;
import org.eclipse.hawkbit.repository.RolloutGroupManagement;
import org.eclipse.hawkbit.repository.SpPermissionChecker;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupStatus;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.addons.lazyquerycontainer.BeanQueryFactory;
import org.vaadin.addons.lazyquerycontainer.LazyQueryContainer;
import org.vaadin.addons.lazyquerycontainer.LazyQueryDefinition;
import org.vaadin.alump.distributionbar.DistributionBar;
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
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Rollout Group Table in List view.
 *
 */
@SpringComponent
@ViewScope
public class RolloutGroupListTable extends AbstractSimpleTable {

    private static final long serialVersionUID = 1182656768844867443L;

    @Autowired
    private I18N i18n;

    @Autowired
    private transient EventBus.SessionEventBus eventBus;

    @Autowired
    private transient RolloutGroupManagement rolloutGroupManagement;

    @Autowired
    private transient RolloutUIState rolloutUIState;

    @Autowired
    private transient SpPermissionChecker permissionChecker;

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
        if (event == RolloutEvent.SHOW_ROLLOUT_GROUPS) {
            ((LazyQueryContainer) getContainerDataSource()).refresh();
        }
    }

    /**
     * 
     * Handles the RolloutGroupChangeEvent to refresh the item in the table.
     * 
     * 
     * @param rolloutGroupChangeEvent
     *            the event which contains the rollout group which has been
     *            change
     */
    @EventBusListenerMethod(scope = EventScope.SESSION)
    public void onEvent(final RolloutGroupChangeEvent rolloutGroupChangeEvent) {
        final List<Object> visibleItemIds = (List<Object>) getVisibleItemIds();
        if (visibleItemIds.contains(rolloutGroupChangeEvent.getRolloutGroupId())) {
            final RolloutGroup rolloutGroup = rolloutGroupManagement
                    .findRolloutGroupWithDetailedStatus(rolloutGroupChangeEvent.getRolloutGroupId());
            final TotalTargetCountStatus totalTargetCountStatus = rolloutGroup.getTotalTargetCountStatus();
            final LazyQueryContainer rolloutContainer = (LazyQueryContainer) getContainerDataSource();
            final Item item = rolloutContainer.getItem(rolloutGroup.getId());
            item.getItemProperty(SPUILabelDefinitions.VAR_STATUS).setValue(rolloutGroup.getStatus());
            item.getItemProperty(SPUILabelDefinitions.VAR_COUNT_TARGETS_RUNNING).setValue(
                    totalTargetCountStatus.getTotalTargetCountByStatus(TotalTargetCountStatus.Status.RUNNING));
            item.getItemProperty(SPUILabelDefinitions.VAR_COUNT_TARGETS_ERROR).setValue(
                    totalTargetCountStatus.getTotalTargetCountByStatus(TotalTargetCountStatus.Status.ERROR));
            item.getItemProperty(SPUILabelDefinitions.VAR_COUNT_TARGETS_FINISHED).setValue(
                    totalTargetCountStatus.getTotalTargetCountByStatus(TotalTargetCountStatus.Status.FINISHED));
            item.getItemProperty(SPUILabelDefinitions.VAR_COUNT_TARGETS_NOT_STARTED).setValue(
                    totalTargetCountStatus.getTotalTargetCountByStatus(TotalTargetCountStatus.Status.NOTSTARTED));
            item.getItemProperty(SPUILabelDefinitions.VAR_COUNT_TARGETS_CANCELLED).setValue(
                    totalTargetCountStatus.getTotalTargetCountByStatus(TotalTargetCountStatus.Status.CANCELLED));
            item.getItemProperty(SPUILabelDefinitions.VAR_COUNT_TARGETS_SCHEDULED).setValue(
                    totalTargetCountStatus.getTotalTargetCountByStatus(TotalTargetCountStatus.Status.SCHEDULED));
            item.getItemProperty("isActionRecieved").setValue(
                    !(Boolean) item.getItemProperty("isActionRecieved").getValue());
        }
    }

    @Override
    protected List<TableColumn> getTableVisibleColumns() {
        final List<TableColumn> columnList = new ArrayList<TableColumn>();
        columnList.add(new TableColumn(SPUIDefinitions.ROLLOUT_GROUP_NAME, i18n.get("header.name"), 0.1f));
        columnList.add(new TableColumn(SPUIDefinitions.ROLLOUT_GROUP_STATUS, i18n.get("header.status"), 0.1f));
        columnList.add(new TableColumn(SPUIDefinitions.DETAIL_STATUS, i18n.get("header.detail.status"), 0.42f));
        columnList
                .add(new TableColumn(SPUILabelDefinitions.VAR_TOTAL_TARGETS, i18n.get("header.total.targets"), 0.08f));
        columnList.add(new TableColumn(SPUIDefinitions.ROLLOUT_GROUP_INSTALLED_PERCENTAGE, i18n
                .get("header.rolloutgroup.installed.percentage"), 0.1f));
        columnList.add(new TableColumn(SPUIDefinitions.ROLLOUT_GROUP_ERROR_THRESHOLD, i18n
                .get("header.rolloutgroup.threshold.error"), 0.1f));
        columnList.add(new TableColumn(SPUIDefinitions.ROLLOUT_GROUP_THRESHOLD, i18n
                .get("header.rolloutgroup.threshold"), 0.1f));
        columnList.add(new TableColumn(SPUILabelDefinitions.VAR_CREATED_DATE, i18n.get("header.createdDate"), 0.15f));
        columnList.add(new TableColumn(SPUILabelDefinitions.VAR_CREATED_USER, i18n.get("header.createdBy"), 0.15f));
        columnList.add(new TableColumn(SPUILabelDefinitions.VAR_MODIFIED_DATE, i18n.get("header.modifiedDate"), 0.15f));
        columnList.add(new TableColumn(SPUILabelDefinitions.VAR_MODIFIED_BY, i18n.get("header.modifiedBy"), 0.15f));
        return columnList;
    }

    @Override
    protected Container createContainer() {
        final BeanQueryFactory<RolloutGroupBeanQuery> rolloutQf = new BeanQueryFactory<RolloutGroupBeanQuery>(
                RolloutGroupBeanQuery.class);
        final LazyQueryContainer rolloutGroupTableContainer = new LazyQueryContainer(new LazyQueryDefinition(true,
                SPUIDefinitions.PAGE_SIZE, SPUILabelDefinitions.VAR_ID), rolloutQf);
        return rolloutGroupTableContainer;
    }

    @Override
    protected void addContainerProperties(final Container container) {
        final LazyQueryContainer rolloutTableContainer = (LazyQueryContainer) container;
        rolloutTableContainer.addContainerProperty(SPUILabelDefinitions.VAR_ID, String.class, null, false, false);
        rolloutTableContainer.addContainerProperty(SPUILabelDefinitions.VAR_NAME, String.class, "", false, false);
        rolloutTableContainer.addContainerProperty(SPUILabelDefinitions.VAR_DESC, String.class, null, false, false);
        rolloutTableContainer.addContainerProperty(SPUILabelDefinitions.VAR_STATUS, RolloutGroupStatus.class, null,
                false, false);
        rolloutTableContainer.addContainerProperty(SPUIDefinitions.ROLLOUT_GROUP_INSTALLED_PERCENTAGE, String.class,
                null, false, false);
        rolloutTableContainer.addContainerProperty(SPUIDefinitions.ROLLOUT_GROUP_ERROR_THRESHOLD, String.class, null,
                false, false);

        rolloutTableContainer.addContainerProperty(SPUIDefinitions.ROLLOUT_GROUP_THRESHOLD, String.class, null, false,
                false);

        rolloutTableContainer.addContainerProperty(SPUILabelDefinitions.VAR_CREATED_DATE, String.class, null, false,
                false);

        rolloutTableContainer.addContainerProperty(SPUILabelDefinitions.VAR_MODIFIED_DATE, String.class, null, false,
                false);
        rolloutTableContainer.addContainerProperty(SPUILabelDefinitions.VAR_CREATED_USER, String.class, null, false,
                false);
        rolloutTableContainer.addContainerProperty(SPUILabelDefinitions.VAR_MODIFIED_BY, String.class, null, false,
                false);
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
        rolloutTableContainer.addContainerProperty("isActionRecieved", Boolean.class, false, false, false);

        rolloutTableContainer.addContainerProperty(SPUILabelDefinitions.VAR_TOTAL_TARGETS, String.class, "0", false,
                false);

    }

    @Override
    protected String getTableId() {
        return SPUIComponetIdProvider.ROLLOUT_GROUP_LIST_TABLE_ID;
    }

    @Override
    protected void onValueChange() {
        /**
         * No implementation required.
         */
    }

    @Override
    protected void addCustomGeneratedColumns() {
        addGeneratedColumn(SPUIDefinitions.ROLLOUT_GROUP_NAME, (source, itemId, columnId) -> getRolloutNameLink(itemId));
        addGeneratedColumn(SPUIDefinitions.ROLLOUT_GROUP_STATUS, (source, itemId, columnId) -> getStatusLabel(itemId));
        addGeneratedColumn(SPUIDefinitions.DETAIL_STATUS, (source, itemId, columnId) -> getProgressBar(itemId));
        setColumnAlignment(SPUIDefinitions.ROLLOUT_GROUP_STATUS, Align.CENTER);

    }

    private Label getStatusLabel(final Object itemId) {
        final Label statusLabel = new Label();
        statusLabel.setHeightUndefined();
        statusLabel.setContentMode(ContentMode.HTML);
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

    private String getDescription(final Object itemId) {
        final Item item = getItem(itemId);
        if (item != null) {
            final RolloutGroupStatus rolloutGroupStatus = (RolloutGroupStatus) item.getItemProperty(
                    SPUILabelDefinitions.VAR_STATUS).getValue();
            return rolloutGroupStatus.toString().toLowerCase();
        }
        return null;
    }

    private void setStatusIcon(final Object itemId, final Label statusLabel) {
        final Item item = getItem(itemId);
        if (item != null) {
            final RolloutGroupStatus rolloutGroupStatus = (RolloutGroupStatus) item.getItemProperty(
                    SPUILabelDefinitions.VAR_STATUS).getValue();
            setRolloutStatusIcon(rolloutGroupStatus, statusLabel);
        }
    }

    private void setRolloutStatusIcon(final RolloutGroupStatus rolloutGroupStatus, final Label statusLabel) {
        switch (rolloutGroupStatus) {
        case FINISHED:
            statusLabel.setValue(FontAwesome.CHECK_CIRCLE.getHtml());
            statusLabel.setStyleName("statusIconGreen");
            break;
        case SCHEDULED:
            statusLabel.setValue(FontAwesome.BULLSEYE.getHtml());
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
        case ERROR:
            statusLabel.setValue(FontAwesome.EXCLAMATION_CIRCLE.getHtml());
            statusLabel.setStyleName("statusIconRed");
            break;
        default:
            break;
        }
        statusLabel.addStyleName(ValoTheme.LABEL_SMALL);
    }

    private Component getRolloutNameLink(final Object itemId) {
        final Item row = getItem(itemId);
        final String rolloutGroupName = (String) row.getItemProperty(SPUILabelDefinitions.VAR_NAME).getValue();
        if (permissionChecker.hasRolloutTargetsReadPermission()) {
            final Button rolloutGroupNameLink = SPUIComponentProvider.getButton(getDetailLinkId(rolloutGroupName),
                    rolloutGroupName, SPUILabelDefinitions.SHOW_ROLLOUT_GROUP_DETAILS, null, false, null,
                    SPUIButtonStyleSmallNoBorder.class);
            rolloutGroupNameLink.setData(rolloutGroupName);
            rolloutGroupNameLink.addStyleName(ValoTheme.LINK_SMALL + " " + "on-focus-no-border link");
            rolloutGroupNameLink.addClickListener(event -> showRolloutGroups(itemId));
            return rolloutGroupNameLink;
        } else {
            final Label rolloutGroupNameLabel = new Label();
            rolloutGroupNameLabel.setHeightUndefined();
            rolloutGroupNameLabel.addStyleName(ValoTheme.LABEL_SMALL);
            rolloutGroupNameLabel.setValue(rolloutGroupName);
            return rolloutGroupNameLabel;
        }
    }

    private void showRolloutGroups(final Object itemId) {
        rolloutUIState.setRolloutGroup(rolloutGroupManagement.findRolloutGroupWithDetailedStatus((Long) itemId));
        eventBus.publish(this, RolloutEvent.SHOW_ROLLOUT_GROUP_TARGETS);
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

    private static String getDetailLinkId(final String rolloutGroupName) {
        return new StringBuilder(SPUIComponetIdProvider.ROLLOUT_GROUP_NAME_LINK_ID).append('.')
                .append(rolloutGroupName).toString();
    }

    @Override
    protected void setCollapsiblecolumns() {
        setColumnCollapsed(SPUILabelDefinitions.VAR_CREATED_DATE, true);
        setColumnCollapsed(SPUILabelDefinitions.VAR_MODIFIED_DATE, true);
        setColumnCollapsed(SPUILabelDefinitions.VAR_CREATED_USER, true);
        setColumnCollapsed(SPUILabelDefinitions.VAR_MODIFIED_BY, true);
    }

}
