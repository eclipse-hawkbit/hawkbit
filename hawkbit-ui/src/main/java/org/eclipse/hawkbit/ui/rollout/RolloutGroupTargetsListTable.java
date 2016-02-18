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

import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupStatus;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.ui.rollout.event.RolloutEvent;
import org.eclipse.hawkbit.ui.rollout.state.RolloutUIState;
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

import com.vaadin.data.Container;
import com.vaadin.data.Item;
import com.vaadin.server.FontAwesome;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.Label;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Rollout Group Targets Table in List view.
 *
 */
@SpringComponent
@ViewScope
public class RolloutGroupTargetsListTable extends AbstractSimpleTable {

    private static final long serialVersionUID = 7984314603271801746L;

    @Autowired
    private I18N i18n;

    @Autowired
    private transient EventBus.SessionEventBus eventBus;

    @Autowired
    private transient RolloutUIState rolloutUIState;

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
        if (event == RolloutEvent.SHOW_ROLLOUT_GROUP_TARGETS) {
            ((LazyQueryContainer) getContainerDataSource()).refresh();
            eventBus.publish(this, RolloutEvent.SHOW_ROLLOUT_GROUP_TARGETS_COUNT);
        }
    }

    @Override
    protected List<TableColumn> getTableVisibleColumns() {
        final List<TableColumn> columnList = new ArrayList<>();
        columnList.add(new TableColumn(SPUILabelDefinitions.VAR_NAME, i18n.get("header.name"), 0.15f));
        columnList.add(new TableColumn(SPUILabelDefinitions.VAR_CREATED_BY, i18n.get("header.createdBy"), 0.15f));
        columnList.add(new TableColumn(SPUILabelDefinitions.VAR_CREATED_DATE, i18n.get("header.createdDate"), 0.15f));
        columnList
                .add(new TableColumn(SPUILabelDefinitions.VAR_LAST_MODIFIED_BY, i18n.get("header.modifiedBy"), 0.15f));
        columnList.add(new TableColumn(SPUILabelDefinitions.VAR_LAST_MODIFIED_DATE, i18n.get("header.modifiedDate"),
                0.15f));
        columnList.add(new TableColumn(SPUILabelDefinitions.VAR_DESC, i18n.get("header.description"), 0.15f));

        columnList.add(new TableColumn(SPUILabelDefinitions.VAR_TARGET_STATUS, i18n.get("header.status"), 0.1f));

        return columnList;
    }

    @Override
    protected Container createContainer() {
        final BeanQueryFactory<RolloutGroupTargetsBeanQuery> rolloutgrouBeanQueryFactory = new BeanQueryFactory<>(
                RolloutGroupTargetsBeanQuery.class);
        return new LazyQueryContainer(new LazyQueryDefinition(true, SPUIDefinitions.PAGE_SIZE,
                SPUILabelDefinitions.VAR_ID), rolloutgrouBeanQueryFactory);
    }

    @Override
    protected void addContainerProperties(final Container container) {
        final LazyQueryContainer rolloutGroupTargetTableContainer = (LazyQueryContainer) container;
        rolloutGroupTargetTableContainer.addContainerProperty(SPUILabelDefinitions.VAR_CONT_ID, String.class, "",
                false, false);
        rolloutGroupTargetTableContainer.addContainerProperty(SPUILabelDefinitions.VAR_NAME, String.class, "", false,
                true);
        rolloutGroupTargetTableContainer.addContainerProperty(SPUILabelDefinitions.VAR_STATUS, Status.class,
                Status.RETRIEVED, false, false);

        rolloutGroupTargetTableContainer.addContainerProperty(SPUILabelDefinitions.VAR_TARGET_STATUS,
                TargetUpdateStatus.class, TargetUpdateStatus.UNKNOWN, false, false);

        rolloutGroupTargetTableContainer.addContainerProperty(SPUILabelDefinitions.ASSIGNED_DISTRIBUTION_NAME_VER,
                String.class, "", false, true);
        rolloutGroupTargetTableContainer.addContainerProperty(SPUILabelDefinitions.VAR_CREATED_BY, String.class, null,
                false, true);
        rolloutGroupTargetTableContainer.addContainerProperty(SPUILabelDefinitions.VAR_LAST_MODIFIED_BY, String.class,
                null, false, true);
        rolloutGroupTargetTableContainer.addContainerProperty(SPUILabelDefinitions.VAR_CREATED_DATE, String.class,
                null, false, true);
        rolloutGroupTargetTableContainer.addContainerProperty(SPUILabelDefinitions.VAR_LAST_MODIFIED_DATE,
                String.class, null, false, true);
        rolloutGroupTargetTableContainer.addContainerProperty(SPUILabelDefinitions.VAR_DESC, String.class, "", false,
                true);
    }

    @Override
    protected String getTableId() {

        return SPUIComponetIdProvider.ROLLOUT_GROUP_TARGETS_LIST_TABLE_ID;
    }

    @Override
    protected void onValueChange() {
        /**
         * No implementation required.
         */

    }

    @Override
    protected void addCustomGeneratedColumns() {
        addGeneratedColumn(SPUILabelDefinitions.VAR_TARGET_STATUS, (source, itemId, columnId) -> getStatusLabel(itemId));
        setColumnAlignment(SPUILabelDefinitions.VAR_TARGET_STATUS, Align.CENTER);
    }

    @Override
    protected void setCollapsiblecolumns() {
        /**
         * No implementation required.
         */
    }

    private Label getStatusLabel(final Object itemId) {
        final Label statusLabel = new Label();
        statusLabel.addStyleName(ValoTheme.LABEL_SMALL);
        statusLabel.setHeightUndefined();
        statusLabel.setContentMode(ContentMode.HTML);
        setStatusIcon(itemId, statusLabel);
        statusLabel.setSizeUndefined();
        return statusLabel;
    }

    private void setStatusIcon(final Object itemId, final Label statusLabel) {
        final Item item = getItem(itemId);
        final RolloutGroup rolloutGroup = rolloutUIState.getRolloutGroup().isPresent() ? rolloutUIState
                .getRolloutGroup().get() : null;
        if (item != null) {
            final Status status = (Status) item.getItemProperty(SPUILabelDefinitions.VAR_STATUS).getValue();
            if (status == null) {
                if (rolloutGroup != null && rolloutGroup.getStatus() == RolloutGroupStatus.READY) {
                    statusLabel.setValue(FontAwesome.DOT_CIRCLE_O.getHtml());
                    statusLabel.addStyleName("statusIconLightBlue");
                    statusLabel.setDescription(RolloutGroupStatus.READY.toString().toLowerCase());
                } else if (rolloutGroup != null && rolloutGroup.getStatus() == RolloutGroupStatus.FINISHED) {
                    statusLabel.setValue(FontAwesome.MINUS_CIRCLE.getHtml());
                    statusLabel.addStyleName("statusIconBlue");

                    final String dsNameVersion = (String) item.getItemProperty(
                            SPUILabelDefinitions.ASSIGNED_DISTRIBUTION_NAME_VER).getValue();
                    statusLabel.setDescription(i18n
                            .get("message.dist.already.assigned", new Object[] { dsNameVersion }));
                }
            } else {
                setRolloutStatusIcon(status, statusLabel);
                statusLabel.setDescription(status.toString().toLowerCase());
            }
        }
    }

     private void setRolloutStatusIcon(final Status targetUpdateStatus, final Label statusLabel) {
		switch (targetUpdateStatus) {
		case ERROR:
			statusLabel.setValue(FontAwesome.EXCLAMATION_CIRCLE.getHtml());
			statusLabel.addStyleName("statusIconRed");
			break;
		case SCHEDULED:
			statusLabel.setValue(FontAwesome.BULLSEYE.getHtml());
			statusLabel.addStyleName("statusIconBlue");
			break;
		case FINISHED:
			statusLabel.setValue(FontAwesome.CHECK_CIRCLE.getHtml());
			statusLabel.addStyleName("statusIconGreen");
			break;
		case RUNNING:
		case RETRIEVED:
		case WARNING:
		case DOWNLOAD:
			statusLabel.setValue(FontAwesome.ADJUST.getHtml());
			statusLabel.addStyleName("statusIconYellow");
			break;
		case CANCELED:
			statusLabel.setValue(FontAwesome.TIMES_CIRCLE.getHtml());
			statusLabel.addStyleName("statusIconGreen");
			break;
		case CANCELING:
			statusLabel.setValue(FontAwesome.TIMES_CIRCLE.getHtml());
			statusLabel.addStyleName("statusIconPending");
			break;
		default:
			break;
		}
    }
}
