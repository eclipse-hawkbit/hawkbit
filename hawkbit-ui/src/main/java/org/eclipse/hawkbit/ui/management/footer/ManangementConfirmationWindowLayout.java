/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.footer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.DistributionSetAssignmentResult;
import org.eclipse.hawkbit.repository.model.RepositoryModelConstants;
import org.eclipse.hawkbit.ui.common.confirmwindow.layout.AbstractConfirmationWindowLayout;
import org.eclipse.hawkbit.ui.common.confirmwindow.layout.ConfirmationTab;
import org.eclipse.hawkbit.ui.common.entity.DistributionSetIdName;
import org.eclipse.hawkbit.ui.common.entity.TargetIdName;
import org.eclipse.hawkbit.ui.common.table.BaseEntityEventType;
import org.eclipse.hawkbit.ui.management.event.DistributionTableEvent;
import org.eclipse.hawkbit.ui.management.event.PinUnpinEvent;
import org.eclipse.hawkbit.ui.management.event.SaveActionWindowEvent;
import org.eclipse.hawkbit.ui.management.event.TargetTableEvent;
import org.eclipse.hawkbit.ui.management.footer.ActionTypeOptionGroupLayout.ActionTypeOption;
import org.eclipse.hawkbit.ui.management.state.ManagementUIState;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus.UIEventBus;

import com.google.common.collect.Maps;
import com.vaadin.data.Item;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Table.Align;

/**
 * Confirmation window for target/distributionSet delete and assignment
 * operations on the Deployment View.
 */
public class ManangementConfirmationWindowLayout extends AbstractConfirmationWindowLayout {

    private static final long serialVersionUID = 1L;

    private static final String DISCARD_CHANGES = "DiscardChanges";

    private static final String TARGET_NAME = "TargetName";

    private static final String DISTRIBUTION_NAME = "DistributionName";

    private static final String DIST_ID = "DistributionId";

    private static final String TARGET_ID = "TargetId";

    private final ManagementUIState managementUIState;

    private final transient TargetManagement targetManagement;

    private final transient DeploymentManagement deploymentManagement;

    private final transient DistributionSetManagement distributionSetManagement;

    private final ActionTypeOptionGroupLayout actionTypeOptionGroupLayout;

    private ConfirmationTab assignmentTab;

    public ManangementConfirmationWindowLayout(final VaadinMessageSource i18n, final UIEventBus eventBus,
            final ManagementUIState managementUIState, final TargetManagement targetManagement,
            final DeploymentManagement deploymentManagement,
            final DistributionSetManagement distributionSetManagement) {
        super(i18n, eventBus);
        this.managementUIState = managementUIState;
        this.targetManagement = targetManagement;
        this.deploymentManagement = deploymentManagement;
        this.distributionSetManagement = distributionSetManagement;
        this.actionTypeOptionGroupLayout = new ActionTypeOptionGroupLayout(i18n);
    }

    @Override
    protected Map<String, ConfirmationTab> getConfirmationTabs() {
        final Map<String, ConfirmationTab> tabs = Maps.newHashMapWithExpectedSize(3);
        if (!managementUIState.getDeletedDistributionList().isEmpty()) {
            tabs.put(i18n.getMessage("caption.delete.dist.accordion.tab"), createDeletedDistributionTab());
        }
        if (!managementUIState.getDeletedTargetList().isEmpty()) {
            tabs.put(i18n.getMessage("caption.delete.target.accordion.tab"), createDeletedTargetTab());
        }
        if (!managementUIState.getAssignedList().isEmpty()) {
            tabs.put(i18n.getMessage("caption.assign.dist.accordion.tab"), createAssignmentTab());
        }
        return tabs;
    }

    private ConfirmationTab createAssignmentTab() {

        assignmentTab = new ConfirmationTab();
        assignmentTab.getConfirmAll().setId(UIComponentIdProvider.SAVE_ASSIGNMENT);
        assignmentTab.getConfirmAll().setIcon(FontAwesome.SAVE);
        assignmentTab.getConfirmAll().setCaption(i18n.getMessage("button.assign.all"));
        assignmentTab.getConfirmAll().addClickListener(event -> saveAllAssignments(assignmentTab));

        assignmentTab.getDiscardAll().setCaption(i18n.getMessage(SPUILabelDefinitions.BUTTON_DISCARD_ALL));
        assignmentTab.getDiscardAll().setId(UIComponentIdProvider.DISCARD_ASSIGNMENT);
        assignmentTab.getDiscardAll().addClickListener(event -> discardAllAssignments(assignmentTab));

        // Add items container to the table.
        assignmentTab.getTable().setContainerDataSource(getAssignmentsTableContainer());

        // Add the discard action column
        assignmentTab.getTable().addGeneratedColumn(DISCARD_CHANGES, (source, itemId, columnId) -> {
            final ClickListener clickListener = event -> discardAssignment((TargetIdName) itemId, assignmentTab);
            return createDiscardButton(itemId, clickListener);
        });

        assignmentTab.getTable().setColumnExpandRatio(TARGET_NAME, 2);
        assignmentTab.getTable().setColumnExpandRatio(DISTRIBUTION_NAME, 2);
        assignmentTab.getTable().setColumnExpandRatio(DISCARD_CHANGES, 1);
        assignmentTab.getTable().setVisibleColumns(TARGET_NAME, DISTRIBUTION_NAME, DISCARD_CHANGES);
        assignmentTab.getTable().setColumnHeaders(i18n.getMessage("header.first.assignment.table"),
                i18n.getMessage("header.second.assignment.table"), i18n.getMessage("header.third.assignment.table"));
        assignmentTab.getTable().setColumnAlignment(DISCARD_CHANGES, Align.CENTER);

        actionTypeOptionGroupLayout.selectDefaultOption();
        assignmentTab.addComponent(actionTypeOptionGroupLayout, 1);
        return assignmentTab;

    }

    private void saveAllAssignments(final ConfirmationTab tab) {
        final Set<TargetIdName> itemIds = managementUIState.getAssignedList().keySet();
        Long distId;
        List<TargetIdName> targetIdSetList;
        List<TargetIdName> tempIdList;
        final ActionType actionType = ((ActionTypeOptionGroupLayout.ActionTypeOption) actionTypeOptionGroupLayout
                .getActionTypeOptionGroup().getValue()).getActionType();
        final long forcedTimeStamp = (((ActionTypeOptionGroupLayout.ActionTypeOption) actionTypeOptionGroupLayout
                .getActionTypeOptionGroup().getValue()) == ActionTypeOption.AUTO_FORCED)
                        ? actionTypeOptionGroupLayout.getForcedTimeDateField().getValue().getTime()
                        : RepositoryModelConstants.NO_FORCE_TIME;

        final Map<Long, List<TargetIdName>> saveAssignedList = Maps.newHashMapWithExpectedSize(itemIds.size());

        int successAssignmentCount = 0;
        int duplicateAssignmentCount = 0;
        for (final TargetIdName itemId : itemIds) {
            final DistributionSetIdName distitem = managementUIState.getAssignedList().get(itemId);
            distId = distitem.getId();

            if (saveAssignedList.containsKey(distId)) {
                targetIdSetList = saveAssignedList.get(distId);
            } else {
                targetIdSetList = new ArrayList<>();
            }
            targetIdSetList.add(itemId);
            saveAssignedList.put(distId, targetIdSetList);
        }

        for (final Map.Entry<Long, List<TargetIdName>> mapEntry : saveAssignedList.entrySet()) {
            tempIdList = saveAssignedList.get(mapEntry.getKey());
            final DistributionSetAssignmentResult distributionSetAssignmentResult = deploymentManagement
                    .assignDistributionSet(mapEntry.getKey(), actionType, forcedTimeStamp,
                            tempIdList.stream().map(t -> t.getControllerId()).collect(Collectors.toList()));

            if (distributionSetAssignmentResult.getAssigned() > 0) {
                successAssignmentCount += distributionSetAssignmentResult.getAssigned();
            }
            if (distributionSetAssignmentResult.getAlreadyAssigned() > 0) {
                duplicateAssignmentCount += distributionSetAssignmentResult.getAlreadyAssigned();
            }
        }
        addMessage(successAssignmentCount, duplicateAssignmentCount);
        resfreshPinnedDetails(saveAssignedList);

        managementUIState.getAssignedList().clear();
        setActionMessage(i18n.getMessage("message.target.ds.assign.success"));
        removeCurrentTab(tab);
        eventBus.publish(this, SaveActionWindowEvent.SAVED_ASSIGNMENTS);
    }

    private void addMessage(final int successAssignmentCount, final int duplicateAssignmentCount) {
        if (successAssignmentCount > 0) {
            addToConsolitatedMsg(getAssigmentSuccessMessage(successAssignmentCount));
        }
        if (duplicateAssignmentCount > 0) {
            addToConsolitatedMsg(getDuplicateAssignmentMessage(duplicateAssignmentCount));
        }

    }

    private void resfreshPinnedDetails(final Map<Long, List<TargetIdName>> saveAssignedList) {
        final Optional<Long> pinnedDist = managementUIState.getTargetTableFilters().getPinnedDistId();
        final Optional<TargetIdName> pinnedTarget = managementUIState.getDistributionTableFilters().getPinnedTarget();

        if (pinnedDist.isPresent()) {
            if (saveAssignedList.keySet().contains(pinnedDist.get())) {
                eventBus.publish(this, PinUnpinEvent.PIN_DISTRIBUTION);
            }
        } else if (pinnedTarget.isPresent()) {
            final Set<TargetIdName> assignedTargetIds = managementUIState.getAssignedList().keySet();
            if (assignedTargetIds.contains(pinnedTarget.get())) {
                eventBus.publish(this, PinUnpinEvent.PIN_TARGET);
            }
        }

    }

    private String getAssigmentSuccessMessage(final int assignedCount) {
        return FontAwesome.TASKS.getHtml() + SPUILabelDefinitions.HTML_SPACE
                + i18n.getMessage("message.target.assignment", new Object[] { assignedCount });
    }

    private String getDuplicateAssignmentMessage(final int alreadyAssignedCount) {
        return FontAwesome.TASKS.getHtml() + SPUILabelDefinitions.HTML_SPACE
                + i18n.getMessage("message.target.alreadyAssigned", new Object[] { alreadyAssignedCount });
    }

    private void discardAllAssignments(final ConfirmationTab tab) {
        removeCurrentTab(tab);
        managementUIState.getAssignedList().clear();
        setActionMessage(i18n.getMessage("message.assign.discard.success"));
        eventBus.publish(this, SaveActionWindowEvent.DISCARD_ALL_ASSIGNMENTS);
    }

    private void discardAssignment(final TargetIdName targetId, final ConfirmationTab tab) {
        tab.getTable().getContainerDataSource().removeItem(targetId);
        managementUIState.getAssignedList().remove(targetId);
        final int assigCount = tab.getTable().getContainerDataSource().size();
        if (0 == assigCount) {
            removeCurrentTab(tab);
            eventBus.publish(this, SaveActionWindowEvent.DISCARD_ALL_ASSIGNMENTS);
        } else {
            eventBus.publish(this, SaveActionWindowEvent.DISCARD_ASSIGNMENT);
        }
    }

    private IndexedContainer getAssignmentsTableContainer() {
        final IndexedContainer contactContainer = new IndexedContainer();
        contactContainer.addContainerProperty(TARGET_NAME, String.class, "");
        contactContainer.addContainerProperty(DISTRIBUTION_NAME, String.class, "");
        contactContainer.addContainerProperty(TARGET_ID, Long.class, "");
        contactContainer.addContainerProperty(DIST_ID, Long.class, "");
        final Map<TargetIdName, DistributionSetIdName> assignedList = managementUIState.getAssignedList();

        for (final Map.Entry<TargetIdName, DistributionSetIdName> entry : assignedList.entrySet()) {
            final Item saveTblitem = contactContainer.addItem(entry.getKey());

            saveTblitem.getItemProperty(TARGET_NAME).setValue(entry.getKey().getTargetName());

            saveTblitem.getItemProperty(DISTRIBUTION_NAME).setValue(HawkbitCommonUtil
                    .getDistributionNameAndVersion(entry.getValue().getName(), entry.getValue().getVersion()));

            saveTblitem.getItemProperty(TARGET_ID).setValue(entry.getKey().getTargetId());
            saveTblitem.getItemProperty(DIST_ID).setValue(entry.getValue().getId());
        }
        return contactContainer;
    }

    private ConfirmationTab createDeletedTargetTab() {
        final ConfirmationTab tab = new ConfirmationTab();

        // TobeDone ? y to set caption every time??
        tab.getConfirmAll().setId(UIComponentIdProvider.TARGET_DELETE_ALL);
        tab.getConfirmAll().setIcon(FontAwesome.TRASH_O);
        tab.getConfirmAll().setCaption(i18n.getMessage(SPUILabelDefinitions.BUTTON_DELETE_ALL));
        tab.getConfirmAll().addClickListener(event -> deleteAllTargets(tab));

        tab.getDiscardAll().setCaption(i18n.getMessage(SPUILabelDefinitions.BUTTON_DISCARD_ALL));
        tab.getDiscardAll().addClickListener(event -> discardAllTargets(tab));

        /* Add items container to the table. */
        tab.getTable().setContainerDataSource(getTargetModuleTableContainer());

        /* Add the discard action column */
        tab.getTable().addGeneratedColumn(DISCARD_CHANGES, (source, itemId, columnId) -> {
            final ClickListener clickListener = event -> discardTargetDelete((TargetIdName) itemId, tab);
            return createDiscardButton(itemId, clickListener);

        });

        tab.getTable().setVisibleColumns(TARGET_NAME, DISCARD_CHANGES);
        tab.getTable().setColumnHeaders(i18n.getMessage("header.first.deletetarget.table"),
                i18n.getMessage("header.second.deletetarget.table"));

        tab.getTable().setColumnExpandRatio(TARGET_NAME, SPUIDefinitions.TARGET_DISTRIBUTION_COLUMN_WIDTH);
        tab.getTable().setColumnExpandRatio(DISCARD_CHANGES, SPUIDefinitions.DISCARD_COLUMN_WIDTH);
        tab.getTable().setColumnAlignment(DISCARD_CHANGES, Align.CENTER);
        return tab;
    }

    private ConfirmationTab createDeletedDistributionTab() {
        final ConfirmationTab tab = new ConfirmationTab();

        // TobeDone ? y to set caption every time??
        tab.getConfirmAll().setId(UIComponentIdProvider.DIST_DELETE_ALL);
        tab.getConfirmAll().setIcon(FontAwesome.TRASH_O);
        tab.getConfirmAll().setCaption(i18n.getMessage(SPUILabelDefinitions.BUTTON_DELETE_ALL));
        tab.getConfirmAll().addClickListener(event -> deleteAllDistributions(tab));

        tab.getDiscardAll().setCaption(i18n.getMessage(SPUILabelDefinitions.BUTTON_DISCARD_ALL));
        tab.getDiscardAll().addClickListener(event -> discardAllDistributions(tab));

        /* Add items container to the table. */
        tab.getTable().setContainerDataSource(getDSModuleTableContainer());

        /* Add the discard action column */
        tab.getTable().addGeneratedColumn(DISCARD_CHANGES, (source, itemId, columnId) -> {
            final ClickListener clickListener = event -> discardDSDelete((DistributionSetIdName) itemId, tab);
            return createDiscardButton(itemId, clickListener);

        });

        tab.getTable().setColumnExpandRatio(DISTRIBUTION_NAME, SPUIDefinitions.TARGET_DISTRIBUTION_COLUMN_WIDTH);
        tab.getTable().setColumnExpandRatio(DISCARD_CHANGES, SPUIDefinitions.DISCARD_COLUMN_WIDTH);
        tab.getTable().setVisibleColumns(DISTRIBUTION_NAME, DISCARD_CHANGES);
        tab.getTable().setColumnHeaders(i18n.getMessage("header.one.deletedist.table"),
                i18n.getMessage("header.second.deletedist.table"));
        tab.getTable().setColumnAlignment(DISCARD_CHANGES, Align.CENTER);
        return tab;
    }

    private void discardDSDelete(final DistributionSetIdName discardDsIdName, final ConfirmationTab tab) {
        managementUIState.getDeletedDistributionList().remove(discardDsIdName);
        tab.getTable().getContainerDataSource().removeItem(discardDsIdName);
        final int deleteCount = tab.getTable().size();
        if (0 == deleteCount) {
            removeCurrentTab(tab);
            eventBus.publish(this, SaveActionWindowEvent.DISCARD_ALL_DISTRIBUTIONS);
        } else {
            eventBus.publish(this, SaveActionWindowEvent.DISCARD_DELETE_DS);
        }
    }

    private IndexedContainer getDSModuleTableContainer() {
        final IndexedContainer contactContainer = new IndexedContainer();
        contactContainer.addContainerProperty(DIST_ID, Long.class, "");
        contactContainer.addContainerProperty(DISTRIBUTION_NAME, String.class, "");
        Item item;
        for (final DistributionSetIdName distIdName : managementUIState.getDeletedDistributionList()) {
            item = contactContainer.addItem(distIdName);
            item.getItemProperty(DIST_ID).setValue(distIdName.getId());

            final String distName = HawkbitCommonUtil.getDistributionNameAndVersion(distIdName.getName(),
                    distIdName.getVersion());
            item.getItemProperty(DISTRIBUTION_NAME).setValue(distName);
        }
        return contactContainer;
    }

    private void discardAllDistributions(final ConfirmationTab tab) {
        removeCurrentTab(tab);
        managementUIState.getDeletedDistributionList().clear();
        setActionMessage(i18n.getMessage("message.dist.discard.success"));
        eventBus.publish(this, SaveActionWindowEvent.DISCARD_ALL_DISTRIBUTIONS);
    }

    private void deleteAllDistributions(final ConfirmationTab tab) {
        final Collection<Long> deletedIds = managementUIState.getDeletedDistributionList().stream()
                .map(DistributionSetIdName::getId).collect(Collectors.toList());

        distributionSetManagement.delete(deletedIds);
        eventBus.publish(this, new DistributionTableEvent(BaseEntityEventType.REMOVE_ENTITY, deletedIds));

        addToConsolitatedMsg(FontAwesome.TRASH_O.getHtml() + SPUILabelDefinitions.HTML_SPACE
                + i18n.getMessage("message.dist.deleted", managementUIState.getDeletedDistributionList().size()));

        removeDeletedDistributionsFromAssignmentTab();
        removeCurrentTab(tab);
        setActionMessage(i18n.getMessage("message.dist.delete.success"));

        managementUIState.getTargetTableFilters().getPinnedDistId()
                .ifPresent(distId -> unPinDeletedDS(deletedIds, distId));
        managementUIState.getDeletedDistributionList().clear();
    }

    private void unPinDeletedTarget(final TargetIdName pinnedTarget) {
        final Set<TargetIdName> deletedTargets = managementUIState.getDeletedTargetList();
        if (deletedTargets.contains(pinnedTarget)) {
            managementUIState.getDistributionTableFilters().setPinnedTarget(null);
            eventBus.publish(this, PinUnpinEvent.UNPIN_TARGET);
        }
    }

    private void unPinDeletedDS(final Collection<Long> deletedDsIds, final Long pinnedDsId) {
        if (deletedDsIds.contains(pinnedDsId)) {
            managementUIState.getTargetTableFilters().setPinnedDistId(null);
            eventBus.publish(this, PinUnpinEvent.UNPIN_DISTRIBUTION);
        }
    }

    private void removeDeletedDistributionsFromAssignmentTab() {
        for (final DistributionSetIdName distributionSetIdName : managementUIState.getDeletedDistributionList()) {
            managementUIState.getAssignedList().entrySet().stream()
                    .forEach(entry -> removeFromAssignmentTab(entry, distributionSetIdName));
            final Collection<DistributionSetIdName> list = managementUIState.getAssignedList().values();
            final Iterator<DistributionSetIdName> itr = list.iterator();
            while (itr.hasNext()) {
                if (itr.next().equals(distributionSetIdName)) {
                    itr.remove();
                }
            }
        }
        eventBus.publish(this, SaveActionWindowEvent.SHOW_HIDE_TAB);
    }

    private void removeFromAssignmentTab(final Entry<TargetIdName, DistributionSetIdName> entry,
            final DistributionSetIdName value) {
        if (Objects.equals(entry.getValue(), value)) {
            assignmentTab.getTable().removeItem(entry.getKey().getTargetId());
        }
    }

    private IndexedContainer getTargetModuleTableContainer() {
        final IndexedContainer contactContainer = new IndexedContainer();
        contactContainer.addContainerProperty(TARGET_ID, Long.class, "");
        contactContainer.addContainerProperty(TARGET_NAME, String.class, "");
        Item item;
        for (final TargetIdName targteId : managementUIState.getDeletedTargetList()) {
            item = contactContainer.addItem(targteId);
            item.getItemProperty(TARGET_ID).setValue(targteId.getTargetId());
            item.getItemProperty(TARGET_NAME).setValue(targteId.getTargetName());
        }
        return contactContainer;
    }

    private void discardTargetDelete(final TargetIdName itemId, final ConfirmationTab tab) {
        managementUIState.getDeletedTargetList().remove(itemId);
        tab.getTable().getContainerDataSource().removeItem(itemId);

        final int assigCount = tab.getTable().getContainerDataSource().size();
        if (0 == assigCount) {
            removeCurrentTab(tab);
            eventBus.publish(this, SaveActionWindowEvent.DISCARD_ALL_TARGETS);
        } else {
            eventBus.publish(this, SaveActionWindowEvent.DISCARD_DELETE_TARGET);
        }
    }

    private void discardAllTargets(final ConfirmationTab tab) {
        removeCurrentTab(tab);
        setActionMessage(i18n.getMessage("message.target.discard.success"));
        managementUIState.getDeletedTargetList().clear();
        eventBus.publish(this, SaveActionWindowEvent.DISCARD_ALL_TARGETS);
    }

    private void deleteAllTargets(final ConfirmationTab tab) {
        final Set<TargetIdName> targetIdNames = managementUIState.getDeletedTargetList();

        final Set<Long> targetIds = targetIdNames.stream().map(TargetIdName::getTargetId).collect(Collectors.toSet());

        targetManagement.delete(targetIds);

        eventBus.publish(this, new TargetTableEvent(BaseEntityEventType.REMOVE_ENTITY, targetIds));

        addToConsolitatedMsg(FontAwesome.TRASH_O.getHtml() + SPUILabelDefinitions.HTML_SPACE
                + i18n.getMessage("message.target.deleted", targetIds.size()));
        removeCurrentTab(tab);
        setActionMessage(i18n.getMessage("message.target.delete.success"));
        removeDeletedTargetsFromAssignmentTab();

        managementUIState.getDistributionTableFilters().getPinnedTarget().ifPresent(this::unPinDeletedTarget);
        eventBus.publish(this, SaveActionWindowEvent.SHOW_HIDE_TAB);
        managementUIState.getDeletedTargetList().clear();
    }

    private void removeDeletedTargetsFromAssignmentTab() {
        for (final TargetIdName targetNameId : managementUIState.getDeletedTargetList()) {
            if (managementUIState.getAssignedList().containsKey(targetNameId)) {
                managementUIState.getAssignedList().remove(targetNameId);
                assignmentTab.getTable().removeItem(targetNameId.getTargetId());
            }
        }
    }

}
