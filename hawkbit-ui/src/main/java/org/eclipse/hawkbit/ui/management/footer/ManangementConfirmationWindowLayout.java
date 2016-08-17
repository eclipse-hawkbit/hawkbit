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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.DistributionSetAssignmentResult;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.RepositoryModelConstants;
import org.eclipse.hawkbit.repository.model.TargetIdName;
import org.eclipse.hawkbit.ui.common.DistributionSetIdName;
import org.eclipse.hawkbit.ui.common.confirmwindow.layout.AbstractConfirmationWindowLayout;
import org.eclipse.hawkbit.ui.common.confirmwindow.layout.ConfirmationTab;
import org.eclipse.hawkbit.ui.management.event.PinUnpinEvent;
import org.eclipse.hawkbit.ui.management.event.SaveActionWindowEvent;
import org.eclipse.hawkbit.ui.management.footer.ActionTypeOptionGroupLayout.ActionTypeOption;
import org.eclipse.hawkbit.ui.management.state.ManagementUIState;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.SPUIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.data.Item;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.server.FontAwesome;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Table.Align;

/**
 * Confirmation window for target/ds delete and assignment.
 * 
 *
 */
@SpringComponent
@ViewScope
public class ManangementConfirmationWindowLayout extends AbstractConfirmationWindowLayout {

    private static final long serialVersionUID = 2114943830055679554L;

    private static final String DISCARD_CHANGES = "DiscardChanges";

    private static final String TARGET_NAME = "TargetName";

    private static final String DISTRIBUTION_NAME = "DistributionName";

    private static final String DIST_ID = "DistributionId";

    private static final String TARGET_ID = "TargetId";

    @Autowired
    private ManagementUIState managementUIState;

    @Autowired
    private transient TargetManagement targetManagement;

    @Autowired
    private transient DeploymentManagement deploymentManagement;

    @Autowired
    private transient DistributionSetManagement distributionSetManagement;

    @Autowired
    private ActionTypeOptionGroupLayout actionTypeOptionGroupLayout;

    private ConfirmationTab assignmnetTab;

    @Override
    protected Map<String, ConfirmationTab> getConfimrationTabs() {
        final Map<String, ConfirmationTab> tabs = new HashMap<>();
        if (!managementUIState.getDeletedDistributionList().isEmpty()) {
            tabs.put(i18n.get("caption.delete.dist.accordion.tab"), createDeletedDistributionTab());
        }
        if (!managementUIState.getDeletedTargetList().isEmpty()) {
            tabs.put(i18n.get("caption.delete.target.accordion.tab"), createDeletedTargetTab());
        }
        if (!managementUIState.getAssignedList().isEmpty()) {
            tabs.put(i18n.get("caption.assign.dist.accordion.tab"), createAssignmentTab());
        }

        return tabs;
    }

    private ConfirmationTab createAssignmentTab() {

        assignmnetTab = new ConfirmationTab();
        assignmnetTab.getConfirmAll().setId(SPUIComponentIdProvider.SAVE_ASSIGNMENT);
        assignmnetTab.getConfirmAll().setIcon(FontAwesome.SAVE);
        assignmnetTab.getConfirmAll().setCaption(i18n.get("button.assign.all"));
        assignmnetTab.getConfirmAll().addClickListener(event -> saveAllAssignments(assignmnetTab));

        assignmnetTab.getDiscardAll().setCaption(i18n.get(SPUILabelDefinitions.BUTTON_DISCARD_ALL));
        assignmnetTab.getDiscardAll().setId(SPUIComponentIdProvider.DISCARD_ASSIGNMENT);
        assignmnetTab.getDiscardAll().addClickListener(event -> discardAllAssignments(assignmnetTab));

        // Add items container to the table.
        assignmnetTab.getTable().setContainerDataSource(getAssignmentsTableContainer());

        // Add the discard action column
        assignmnetTab.getTable().addGeneratedColumn(DISCARD_CHANGES, (source, itemId, columnId) -> {
            final ClickListener clickListener = event -> discardAssignment(
                    (TargetIdName) ((Button) event.getComponent()).getData(), assignmnetTab);
            return createDiscardButton(itemId, clickListener);
        });

        assignmnetTab.getTable().setColumnExpandRatio(TARGET_NAME, 2);
        assignmnetTab.getTable().setColumnExpandRatio(DISTRIBUTION_NAME, 2);
        assignmnetTab.getTable().setColumnExpandRatio(DISCARD_CHANGES, 1);
        assignmnetTab.getTable().setVisibleColumns(TARGET_NAME, DISTRIBUTION_NAME, DISCARD_CHANGES);
        assignmnetTab.getTable().setColumnHeaders(i18n.get("header.first.assignment.table"),
                i18n.get("header.second.assignment.table"), i18n.get("header.third.assignment.table"));
        assignmnetTab.getTable().setColumnAlignment(DISCARD_CHANGES, Align.CENTER);

        actionTypeOptionGroupLayout.selectDefaultOption();
        assignmnetTab.addComponent(actionTypeOptionGroupLayout, 1);
        return assignmnetTab;

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

        final Map<Long, ArrayList<TargetIdName>> saveAssignedList = new HashMap<>();

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
            saveAssignedList.put(distId, (ArrayList<TargetIdName>) targetIdSetList);
        }

        for (final Map.Entry<Long, ArrayList<TargetIdName>> mapEntry : saveAssignedList.entrySet()) {
            tempIdList = saveAssignedList.get(mapEntry.getKey());
            final String[] ids = tempIdList.stream().map(t -> t.getControllerId()).toArray(size -> new String[size]);
            final DistributionSetAssignmentResult distributionSetAssignmentResult = deploymentManagement
                    .assignDistributionSet(mapEntry.getKey(), actionType, forcedTimeStamp, ids);

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
        setActionMessage(i18n.get("message.target.ds.assign.success"));
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

    private void resfreshPinnedDetails(final Map<Long, ArrayList<TargetIdName>> saveAssignedList) {
        /**
         * If pinned Ds id is there in saved assignment list then refresh the
         * pinning
         */
        if (managementUIState.getTargetTableFilters().getPinnedDistId().isPresent()) {
            if (saveAssignedList.keySet().contains(managementUIState.getTargetTableFilters().getPinnedDistId().get())) {
                eventBus.publish(this, PinUnpinEvent.PIN_DISTRIBUTION);
            }
        } else if (managementUIState.getDistributionTableFilters().getPinnedTargetId().isPresent()) {
            final Set<TargetIdName> targetIdNameList = managementUIState.getAssignedList().keySet();
            final List<String> assignedTargetIds = targetIdNameList.stream()
                    .map(targetIdName -> targetIdName.getControllerId()).collect(Collectors.toList());
            if (assignedTargetIds.contains(managementUIState.getDistributionTableFilters().getPinnedTargetId().get())) {
                eventBus.publish(this, PinUnpinEvent.PIN_TARGET);
            }
        }

    }

    private String getAssigmentSuccessMessage(final int assignedCount) {
        return FontAwesome.TASKS.getHtml() + SPUILabelDefinitions.HTML_SPACE
                + i18n.get("message.target.assignment", new Object[] { assignedCount });
    }

    private String getDuplicateAssignmentMessage(final int alreadyAssignedCount) {
        return FontAwesome.TASKS.getHtml() + SPUILabelDefinitions.HTML_SPACE
                + i18n.get("message.target.alreadyAssigned", new Object[] { alreadyAssignedCount });
    }

    private void discardAllAssignments(final ConfirmationTab tab) {
        removeCurrentTab(tab);
        managementUIState.getAssignedList().clear();
        setActionMessage(i18n.get("message.assign.discard.success"));
        eventBus.publish(this, SaveActionWindowEvent.DISCARD_ALL_ASSIGNMENTS);
    }

    private void discardAssignment(final TargetIdName targetIdName, final ConfirmationTab tab) {
        tab.getTable().getContainerDataSource().removeItem(targetIdName);
        managementUIState.getAssignedList().remove(targetIdName);
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
        contactContainer.addContainerProperty(TARGET_ID, String.class, "");
        contactContainer.addContainerProperty(DIST_ID, Long.class, "");
        Item saveTblitem;
        final Map<TargetIdName, DistributionSetIdName> assignedList = managementUIState.getAssignedList();

        for (final Map.Entry<TargetIdName, DistributionSetIdName> entry : assignedList.entrySet()) {
            saveTblitem = contactContainer.addItem(entry.getKey());

            saveTblitem.getItemProperty(TARGET_NAME).setValue(entry.getKey().getName());

            saveTblitem.getItemProperty(DISTRIBUTION_NAME).setValue(HawkbitCommonUtil
                    .getDistributionNameAndVersion(entry.getValue().getName(), entry.getValue().getVersion()));

            saveTblitem.getItemProperty(TARGET_ID).setValue(entry.getKey().getControllerId());
            saveTblitem.getItemProperty(DIST_ID).setValue(entry.getValue().getId());
        }
        return contactContainer;
    }

    private ConfirmationTab createDeletedTargetTab() {
        final ConfirmationTab tab = new ConfirmationTab();

        // TobeDone ? y to set caption every time??
        tab.getConfirmAll().setId(SPUIComponentIdProvider.TARGET_DELETE_ALL);
        tab.getConfirmAll().setIcon(FontAwesome.TRASH_O);
        tab.getConfirmAll().setCaption(i18n.get(SPUILabelDefinitions.BUTTON_DELETE_ALL));
        tab.getConfirmAll().addClickListener(event -> deleteAllTargets(tab));

        tab.getDiscardAll().setCaption(i18n.get(SPUILabelDefinitions.BUTTON_DISCARD_ALL));
        tab.getDiscardAll().addClickListener(event -> discardAllTargets(tab));

        /* Add items container to the table. */
        tab.getTable().setContainerDataSource(getTargetModuleTableContainer());

        /* Add the discard action column */
        tab.getTable().addGeneratedColumn(DISCARD_CHANGES, (source, itemId, columnId) -> {
            final ClickListener clickListener = event -> discardTargetDelete(
                    (TargetIdName) ((Button) event.getComponent()).getData(), itemId, tab);
            return createDiscardButton(itemId, clickListener);

        });

        tab.getTable().setVisibleColumns(TARGET_NAME, DISCARD_CHANGES);
        tab.getTable().setColumnHeaders(i18n.get("header.first.deletetarget.table"),
                i18n.get("header.second.deletetarget.table"));

        tab.getTable().setColumnExpandRatio(TARGET_NAME, SPUIDefinitions.TARGET_DISTRIBUTION_COLUMN_WIDTH);
        tab.getTable().setColumnExpandRatio(DISCARD_CHANGES, SPUIDefinitions.DISCARD_COLUMN_WIDTH);
        tab.getTable().setColumnAlignment(DISCARD_CHANGES, Align.CENTER);
        return tab;
    }

    private ConfirmationTab createDeletedDistributionTab() {
        final ConfirmationTab tab = new ConfirmationTab();

        // TobeDone ? y to set caption every time??
        tab.getConfirmAll().setId(SPUIComponentIdProvider.DIST_DELETE_ALL);
        tab.getConfirmAll().setIcon(FontAwesome.TRASH_O);
        tab.getConfirmAll().setCaption(i18n.get(SPUILabelDefinitions.BUTTON_DELETE_ALL));
        tab.getConfirmAll().addClickListener(event -> deleteAllDistributions(tab));

        tab.getDiscardAll().setCaption(i18n.get(SPUILabelDefinitions.BUTTON_DISCARD_ALL));
        tab.getDiscardAll().addClickListener(event -> discardAllDistributions(tab));

        /* Add items container to the table. */
        tab.getTable().setContainerDataSource(getDSModuleTableContainer());

        /* Add the discard action column */
        tab.getTable().addGeneratedColumn(DISCARD_CHANGES, (source, itemId, columnId) -> {
            final ClickListener clickListener = event -> discardDSDelete(
                    (DistributionSetIdName) ((Button) event.getComponent()).getData(), itemId, tab);
            return createDiscardButton(itemId, clickListener);

        });

        tab.getTable().setColumnExpandRatio(DISTRIBUTION_NAME, SPUIDefinitions.TARGET_DISTRIBUTION_COLUMN_WIDTH);
        tab.getTable().setColumnExpandRatio(DISCARD_CHANGES, SPUIDefinitions.DISCARD_COLUMN_WIDTH);
        tab.getTable().setVisibleColumns(DISTRIBUTION_NAME, DISCARD_CHANGES);
        tab.getTable().setColumnHeaders(i18n.get("header.one.deletedist.table"),
                i18n.get("header.second.deletedist.table"));
        tab.getTable().setColumnAlignment(DISCARD_CHANGES, Align.CENTER);
        return tab;
    }

    private void discardDSDelete(final DistributionSetIdName discardDsIdName, final Object itemId,
            final ConfirmationTab tab) {
        managementUIState.getDeletedDistributionList().remove(discardDsIdName);
        tab.getTable().getContainerDataSource().removeItem(itemId);
        tab.getTable().getContainerDataSource().removeItem(itemId);
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
        setActionMessage(i18n.get("message.dist.discard.success"));
        eventBus.publish(this, SaveActionWindowEvent.DISCARD_ALL_DISTRIBUTIONS);
    }

    private void deleteAllDistributions(final ConfirmationTab tab) {
        final Set<Long> deletedIds = new HashSet<>();
        managementUIState.getDeletedDistributionList().forEach(distIdName -> deletedIds.add(distIdName.getId()));
        distributionSetManagement.deleteDistributionSet(deletedIds.toArray(new Long[deletedIds.size()]));
        addToConsolitatedMsg(FontAwesome.TRASH_O.getHtml() + SPUILabelDefinitions.HTML_SPACE
                + i18n.get("message.dist.deleted", managementUIState.getDeletedDistributionList().size()));

        removeDeletedDistributionsFromAssignmentTab();
        removeCurrentTab(tab);
        setActionMessage(i18n.get("message.dist.delete.success"));

        managementUIState.getTargetTableFilters().getPinnedDistId()
                .ifPresent(distId -> unPinDeletedDS(deletedIds, distId));

        managementUIState.getDeletedDistributionList().clear();

    }

    private void unPinDeletedTarget(final String pinnedTargetId) {
        final Set<TargetIdName> deletedTargets = managementUIState.getDeletedTargetList();
        final List<String> deletedTargetsControllerIds = deletedTargets.stream().map(t -> t.getControllerId())
                .collect(Collectors.toList());
        if (deletedTargetsControllerIds.contains(pinnedTargetId)) {
            managementUIState.getDistributionTableFilters().setPinnedTargetId(null);
            eventBus.publish(this, PinUnpinEvent.UNPIN_TARGET);
        }
    }

    private void unPinDeletedDS(final Set<Long> deletedDsIds, final Long pinnedDsId) {
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

    private void removeFromAssignmentTab(final Map.Entry entry, final DistributionSetIdName value) {
        if (Objects.equals(entry.getValue(), value)) {
            assignmnetTab.getTable().removeItem(entry.getKey());
        }
    }

    private IndexedContainer getTargetModuleTableContainer() {
        final IndexedContainer contactContainer = new IndexedContainer();
        contactContainer.addContainerProperty(TARGET_ID, String.class, "");
        contactContainer.addContainerProperty(TARGET_NAME, String.class, "");
        Item item;
        for (final TargetIdName targteId : managementUIState.getDeletedTargetList()) {
            item = contactContainer.addItem(targteId);
            item.getItemProperty(TARGET_ID).setValue(targteId.getControllerId());
            item.getItemProperty(TARGET_NAME).setValue(targteId.getName());
        }
        return contactContainer;
    }

    private void discardTargetDelete(final TargetIdName discardTargetIdName, final Object itemId,
            final ConfirmationTab tab) {
        managementUIState.getDeletedTargetList().remove(discardTargetIdName);
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
        setActionMessage(i18n.get("message.target.discard.success"));
        managementUIState.getDeletedTargetList().clear();
        eventBus.publish(this, SaveActionWindowEvent.DISCARD_ALL_TARGETS);
    }

    private void deleteAllTargets(final ConfirmationTab tab) {
        final Set<TargetIdName> itemIds = managementUIState.getDeletedTargetList();
        final List<Long> targetIds = itemIds.stream().map(t -> t.getTargetId()).collect(Collectors.toList());

        targetManagement.deleteTargets(targetIds.toArray(new Long[targetIds.size()]));
        addToConsolitatedMsg(FontAwesome.TRASH_O.getHtml() + SPUILabelDefinitions.HTML_SPACE
                + i18n.get("message.target.deleted", targetIds.size()));
        removeCurrentTab(tab);
        setActionMessage(i18n.get("message.target.delete.success"));
        // TobeDone change eventing convention

        removeDeletedTargetsFromAssignmentTab();

        /*
         * On delete of pinned target ,unpin refresh both target table and DS
         */
        managementUIState.getDistributionTableFilters().getPinnedTargetId().ifPresent(this::unPinDeletedTarget);
        eventBus.publish(this, SaveActionWindowEvent.DELETED_TARGETS);
        managementUIState.getDeletedTargetList().clear();

    }

    private void removeDeletedTargetsFromAssignmentTab() {
        for (final TargetIdName targetNameId : managementUIState.getDeletedTargetList()) {
            if (managementUIState.getAssignedList().containsKey(targetNameId)) {
                managementUIState.getAssignedList().remove(targetNameId);
                assignmnetTab.getTable().removeItem(targetNameId);
            }
        }
    }

}
