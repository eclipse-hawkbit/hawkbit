/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.distributions.footer;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.DistributionSetTypeManagement;
import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.repository.SoftwareModuleTypeManagement;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.ui.artifacts.event.SoftwareModuleEvent;
import org.eclipse.hawkbit.ui.common.confirmwindow.layout.AbstractConfirmationWindowLayout;
import org.eclipse.hawkbit.ui.common.confirmwindow.layout.ConfirmationTab;
import org.eclipse.hawkbit.ui.common.entity.DistributionSetIdName;
import org.eclipse.hawkbit.ui.common.entity.SoftwareModuleIdName;
import org.eclipse.hawkbit.ui.common.table.BaseEntityEventType;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleSmallNoBorder;
import org.eclipse.hawkbit.ui.distributions.event.SaveActionWindowEvent;
import org.eclipse.hawkbit.ui.distributions.state.ManageDistUIState;
import org.eclipse.hawkbit.ui.management.event.DistributionTableEvent;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.springframework.util.CollectionUtils;
import org.vaadin.spring.events.EventBus.UIEventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.google.common.collect.Maps;
import com.vaadin.data.Container;
import com.vaadin.data.Item;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Table.Align;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Abstract layout of confirm actions window.
 *
 */
public class DistributionsConfirmationWindowLayout extends AbstractConfirmationWindowLayout {

    private static final long serialVersionUID = 3641621131320823058L;

    private static final String SW_MODULE_NAME_MSG = "SW MOdule Name";

    private static final String SW_DISCARD_CHGS = "DiscardChanges";

    private static final String SW_MODULE_TYPE_NAME = "SoftwareModuleTypeName";

    private static final String DISCARD = "Discard";

    private static final String DIST_NAME = "DistributionName";

    private static final String SOFTWARE_MODULE_NAME = "SoftwareModuleName";

    private static final String DIST_ID_NAME = "DistributionIdName";

    private static final String DIST_SET_NAME = "DistributionSetTypeName";

    private static final String SOFTWARE_MODULE_ID_NAME = "SoftwareModuleIdName";

    private ConfirmationTab assignmnetTab;

    private final transient DistributionSetManagement distributionSetManagement;

    private final transient DistributionSetTypeManagement distributionSetTypeManagement;

    private final transient SoftwareModuleManagement softwareModuleManagement;

    private final transient SoftwareModuleTypeManagement softwareModuleTypeManagement;

    private final ManageDistUIState manageDistUIState;

    DistributionsConfirmationWindowLayout(final VaadinMessageSource i18n, final UIEventBus eventBus,
            final DistributionSetManagement dsManagement,
            final DistributionSetTypeManagement distributionSetTypeManagement,
            final SoftwareModuleManagement softwareModuleManagement,
            final SoftwareModuleTypeManagement softwareModuleTypeManagement,
            final ManageDistUIState manageDistUIState) {
        super(i18n, eventBus);
        this.distributionSetManagement = dsManagement;
        this.distributionSetTypeManagement = distributionSetTypeManagement;
        this.softwareModuleTypeManagement = softwareModuleTypeManagement;
        this.softwareModuleManagement = softwareModuleManagement;
        this.manageDistUIState = manageDistUIState;
    }

    @Override
    protected Map<String, ConfirmationTab> getConfirmationTabs() {
        final Map<String, ConfirmationTab> tabs = Maps.newHashMapWithExpectedSize(5);
        /* Create tab for SW Modules delete */
        if (!manageDistUIState.getDeleteSofwareModulesList().isEmpty()) {
            tabs.put(i18n.getMessage("caption.delete.swmodule.accordion.tab"), createSMDeleteConfirmationTab());
        }

        /* Create tab for SW Module Type delete */
        if (!manageDistUIState.getSelectedDeleteSWModuleTypes().isEmpty()) {
            tabs.put(i18n.getMessage("caption.delete.sw.module.type.accordion.tab"),
                    createSMtypeDeleteConfirmationTab());
        }

        /* Create tab for Distributions delete */
        if (!manageDistUIState.getDeletedDistributionList().isEmpty()) {
            tabs.put(i18n.getMessage("caption.delete.dist.accordion.tab"), createDistDeleteConfirmationTab());
        }

        /* Create tab for Distribution Set Types delete */
        if (!manageDistUIState.getSelectedDeleteDistSetTypes().isEmpty()) {
            tabs.put(i18n.getMessage("caption.delete.dist.set.type.accordion.tab"),
                    createDistSetTypeDeleteConfirmationTab());
        }

        /* Create tab for Assign Software Module */
        if (!manageDistUIState.getAssignedList().isEmpty()) {
            tabs.put(i18n.getMessage("caption.assign.dist.accordion.tab"), createAssignSWModuleConfirmationTab());
        }

        return tabs;

    }

    private ConfirmationTab createSMDeleteConfirmationTab() {
        final ConfirmationTab tab = new ConfirmationTab();

        tab.getConfirmAll().setId(UIComponentIdProvider.SW_DELETE_ALL);
        tab.getConfirmAll().setIcon(FontAwesome.TRASH_O);
        tab.getConfirmAll().setCaption(i18n.getMessage(SPUILabelDefinitions.BUTTON_DELETE_ALL));
        tab.getConfirmAll().addClickListener(event -> deleteSMAll(tab));

        tab.getDiscardAll().setCaption(i18n.getMessage(SPUILabelDefinitions.BUTTON_DISCARD_ALL));
        tab.getDiscardAll().addClickListener(event -> discardSMAll(tab));

        /* Add items container to the table. */
        tab.getTable().setContainerDataSource(getSWModuleTableContainer());

        /* Add the discard action column */
        tab.getTable().addGeneratedColumn(SW_DISCARD_CHGS, (source, itemId, columnId) -> {
            final ClickListener clickListener = event -> discardSoftwareDelete(event, itemId, tab);
            return createDiscardButton(itemId, clickListener);
        });

        tab.getTable().setVisibleColumns(SW_MODULE_NAME_MSG, SW_DISCARD_CHGS);
        tab.getTable().setColumnHeaders(i18n.getMessage("upload.swModuleTable.header"),
                i18n.getMessage("header.second.deletetarget.table"));

        tab.getTable().setColumnExpandRatio(SW_MODULE_NAME_MSG, SPUIDefinitions.TARGET_DISTRIBUTION_COLUMN_WIDTH);
        tab.getTable().setColumnExpandRatio(SW_DISCARD_CHGS, SPUIDefinitions.DISCARD_COLUMN_WIDTH);
        tab.getTable().setColumnAlignment(SW_DISCARD_CHGS, Align.CENTER);
        return tab;
    }

    private void deleteSMAll(final ConfirmationTab tab) {
        final Set<Long> swmoduleIds = manageDistUIState.getDeleteSofwareModulesList().keySet();

        if (manageDistUIState.getAssignedList() == null || manageDistUIState.getAssignedList().isEmpty()) {
            removeAssignedSoftwareModules();
        }

        softwareModuleManagement.delete(swmoduleIds);
        eventBus.publish(this, new SoftwareModuleEvent(BaseEntityEventType.REMOVE_ENTITY, swmoduleIds));

        addToConsolitatedMsg(FontAwesome.TRASH_O.getHtml() + SPUILabelDefinitions.HTML_SPACE
                + i18n.getMessage("message.swModule.deleted", swmoduleIds.size()));
        manageDistUIState.getDeleteSofwareModulesList().clear();
        removeCurrentTab(tab);
        setActionMessage(i18n.getMessage("message.software.delete.success"));
        eventBus.publish(this, SaveActionWindowEvent.DELETE_ALL_SOFWARE);
    }

    private void removeAssignedSoftwareModules() {
        for (final Entry<DistributionSetIdName, HashSet<SoftwareModuleIdName>> entryAssignSM : manageDistUIState
                .getAssignedList().entrySet()) {
            for (final Entry<Long, String> entryDeleteSM : manageDistUIState.getDeleteSofwareModulesList().entrySet()) {
                final SoftwareModuleIdName smIdName = new SoftwareModuleIdName(entryDeleteSM.getKey(),
                        entryDeleteSM.getValue());
                if (entryAssignSM.getValue().contains(smIdName)) {
                    entryAssignSM.getValue().remove(smIdName);
                    assignmnetTab.getTable().removeItem(HawkbitCommonUtil.concatStrings("|||",
                            entryAssignSM.getKey().getId().toString(), smIdName.getId().toString()));
                }

                if (entryAssignSM.getValue().isEmpty()) {
                    manageDistUIState.getAssignedList().remove(entryAssignSM.getKey());
                }
            }
        }
    }

    private void discardSMAll(final ConfirmationTab tab) {
        removeCurrentTab(tab);
        manageDistUIState.getDeleteSofwareModulesList().clear();
        setActionMessage(i18n.getMessage("message.software.discard.success"));
        eventBus.publish(this, SaveActionWindowEvent.DISCARD_ALL_SOFTWARE);
    }

    /**
     * Get SWModule table container.
     *
     * @return IndexedContainer
     */
    @SuppressWarnings("unchecked")
    private IndexedContainer getSWModuleTableContainer() {
        final IndexedContainer swcontactContainer = new IndexedContainer();
        swcontactContainer.addContainerProperty("SWModuleId", String.class, "");
        swcontactContainer.addContainerProperty(SW_MODULE_NAME_MSG, String.class, "");
        for (final Long swModuleID : manageDistUIState.getDeleteSofwareModulesList().keySet()) {
            final Item item = swcontactContainer.addItem(swModuleID);
            item.getItemProperty("SWModuleId").setValue(swModuleID.toString());
            item.getItemProperty(SW_MODULE_NAME_MSG)
                    .setValue(manageDistUIState.getDeleteSofwareModulesList().get(swModuleID));
        }
        return swcontactContainer;
    }

    private void discardSoftwareDelete(final Button.ClickEvent event, final Object itemId, final ConfirmationTab tab) {
        final Long swmoduleId = (Long) ((Button) event.getComponent()).getData();
        if (null != manageDistUIState.getDeleteSofwareModulesList()
                && !manageDistUIState.getDeleteSofwareModulesList().isEmpty()
                && manageDistUIState.getDeleteSofwareModulesList().containsKey(swmoduleId)) {
            manageDistUIState.getDeleteSofwareModulesList().remove(swmoduleId);
        }
        tab.getTable().getContainerDataSource().removeItem(itemId);
        final int deleteCount = tab.getTable().size();
        if (0 == deleteCount) {
            removeCurrentTab(tab);
            eventBus.publish(this, SaveActionWindowEvent.DISCARD_ALL_SOFTWARE);
        } else {
            eventBus.publish(this, SaveActionWindowEvent.DISCARD_DELETE_SOFTWARE);
        }
    }

    private ConfirmationTab createSMtypeDeleteConfirmationTab() {
        final ConfirmationTab tab = new ConfirmationTab();

        tab.getConfirmAll().setId(UIComponentIdProvider.SAVE_DELETE_SW_MODULE_TYPE);
        tab.getConfirmAll().setIcon(FontAwesome.TRASH_O);
        tab.getConfirmAll().setCaption(i18n.getMessage(SPUILabelDefinitions.BUTTON_DELETE_ALL));
        tab.getConfirmAll().addClickListener(event -> deleteSMtypeAll(tab));

        tab.getDiscardAll().setCaption(i18n.getMessage(SPUILabelDefinitions.BUTTON_DISCARD_ALL));
        tab.getDiscardAll().setId(UIComponentIdProvider.DISCARD_SW_MODULE_TYPE);
        tab.getDiscardAll().addClickListener(event -> discardSMtypeAll(tab));

        // Add items container to the table.
        tab.getTable().setContainerDataSource(getSWModuleTypeTableContainer());

        // Add the discard action column
        tab.getTable().addGeneratedColumn(DISCARD, (source, itemId, columnId) -> {
            final StringBuilder style = new StringBuilder(ValoTheme.BUTTON_TINY);
            style.append(' ');
            style.append(SPUIStyleDefinitions.REDICON);
            final Button deleteIcon = SPUIComponentProvider.getButton("", "", SPUILabelDefinitions.DISCARD,
                    style.toString(), true, FontAwesome.REPLY, SPUIButtonStyleSmallNoBorder.class);
            deleteIcon.setData(itemId);
            deleteIcon.setImmediate(true);
            deleteIcon.addClickListener(event -> discardSoftwareTypeDelete(
                    (String) ((Button) event.getComponent()).getData(), itemId, tab));
            return deleteIcon;
        });

        tab.getTable().setVisibleColumns(SW_MODULE_TYPE_NAME, DISCARD);
        tab.getTable().setColumnHeaders(i18n.getMessage("header.first.delete.swmodule.type.table"),
                i18n.getMessage("header.second.delete.swmodule.type.table"));

        tab.getTable().setColumnExpandRatio(SW_MODULE_TYPE_NAME, 2);
        tab.getTable().setColumnExpandRatio(SW_DISCARD_CHGS, SPUIDefinitions.DISCARD_COLUMN_WIDTH);
        tab.getTable().setColumnAlignment(SW_DISCARD_CHGS, Align.CENTER);
        return tab;
    }

    private void deleteSMtypeAll(final ConfirmationTab tab) {
        final int deleteSWModuleTypeCount = manageDistUIState.getSelectedDeleteSWModuleTypes().size();
        for (final String swModuleTypeName : manageDistUIState.getSelectedDeleteSWModuleTypes()) {

            softwareModuleTypeManagement.getByName(swModuleTypeName).map(SoftwareModuleType::getId)
                    .ifPresent(softwareModuleTypeManagement::delete);
        }
        addToConsolitatedMsg(FontAwesome.TASKS.getHtml() + SPUILabelDefinitions.HTML_SPACE
                + i18n.getMessage("message.sw.module.type.delete", new Object[] { deleteSWModuleTypeCount }));
        manageDistUIState.getSelectedDeleteSWModuleTypes().clear();
        removeCurrentTab(tab);
        setActionMessage(i18n.getMessage("message.software.type.delete.success"));
        eventBus.publish(this, SaveActionWindowEvent.SAVED_DELETE_SW_MODULE_TYPES);
    }

    private void discardSMtypeAll(final ConfirmationTab tab) {
        removeCurrentTab(tab);
        manageDistUIState.getSelectedDeleteSWModuleTypes().clear();
        setActionMessage(i18n.getMessage("message.software.type.discard.success"));
        eventBus.publish(this, SaveActionWindowEvent.DISCARD_ALL_DELETE_SW_MODULE_TYPES);
    }

    private Container getSWModuleTypeTableContainer() {
        final IndexedContainer contactContainer = new IndexedContainer();
        contactContainer.addContainerProperty(SW_MODULE_TYPE_NAME, String.class, "");

        for (final String swModuleTypeName : manageDistUIState.getSelectedDeleteSWModuleTypes()) {
            final Item saveTblitem = contactContainer.addItem(swModuleTypeName);

            saveTblitem.getItemProperty(SW_MODULE_TYPE_NAME).setValue(swModuleTypeName);
        }

        return contactContainer;
    }

    private void discardSoftwareTypeDelete(final String discardSWModuleType, final Object itemId,
            final ConfirmationTab tab) {
        if (!CollectionUtils.isEmpty(manageDistUIState.getSelectedDeleteSWModuleTypes())
                && manageDistUIState.getSelectedDeleteSWModuleTypes().contains(discardSWModuleType)) {
            manageDistUIState.getSelectedDeleteSWModuleTypes().remove(discardSWModuleType);
        }
        tab.getTable().getContainerDataSource().removeItem(itemId);
        final int deleteCount = tab.getTable().size();
        if (0 == deleteCount) {
            removeCurrentTab(tab);
            eventBus.publish(this, SaveActionWindowEvent.DISCARD_ALL_DELETE_SW_MODULE_TYPES);
        } else {
            eventBus.publish(this, SaveActionWindowEvent.DISCARD_DELETE_SW_MODULE_TYPE);
        }
    }

    /* for Distributions */
    private ConfirmationTab createDistDeleteConfirmationTab() {
        final ConfirmationTab tab = new ConfirmationTab();

        tab.getConfirmAll().setId(UIComponentIdProvider.DIST_DELETE_ALL);
        tab.getConfirmAll().setIcon(FontAwesome.TRASH_O);
        tab.getConfirmAll().setCaption(i18n.getMessage(SPUILabelDefinitions.BUTTON_DELETE_ALL));
        tab.getConfirmAll().addClickListener(event -> deleteDistAll(tab));

        tab.getDiscardAll().setCaption(i18n.getMessage(SPUILabelDefinitions.BUTTON_DISCARD_ALL));
        tab.getDiscardAll().addClickListener(event -> discardDistAll(tab));

        /* Add items container to the table. */
        tab.getTable().setContainerDataSource(getDistTableContainer());

        /* Add the discard action column */
        tab.getTable().addGeneratedColumn(DISCARD, (source, itemId, columnId) -> {
            final ClickListener clickListener = event -> discardDistDelete(event, itemId, tab);
            return createDiscardButton(itemId, clickListener);

        });

        tab.getTable().setVisibleColumns(DIST_NAME, DISCARD);
        tab.getTable().setColumnHeaders(i18n.getMessage("header.one.deletedist.table"),
                i18n.getMessage("header.second.deletedist.table"));

        tab.getTable().setColumnExpandRatio(DIST_NAME, SPUIDefinitions.TARGET_DISTRIBUTION_COLUMN_WIDTH);
        tab.getTable().setColumnExpandRatio(DISCARD, SPUIDefinitions.DISCARD_COLUMN_WIDTH);
        tab.getTable().setColumnAlignment(DISCARD, Align.CENTER);
        return tab;
    }

    /* Delete Distributions. */
    private void deleteDistAll(final ConfirmationTab tab) {
        final Long[] deletedIds = manageDistUIState.getDeletedDistributionList().stream().map(idName -> idName.getId())
                .toArray(Long[]::new);
        if (manageDistUIState.getAssignedList() != null && !manageDistUIState.getAssignedList().isEmpty()) {
            manageDistUIState.getDeletedDistributionList().forEach(distSetName -> {
                if (manageDistUIState.getAssignedList().containsKey(distSetName)) {
                    manageDistUIState.getAssignedList().remove(distSetName);
                }
            });
        }

        distributionSetManagement.delete(Arrays.asList(deletedIds));
        eventBus.publish(this,
                new DistributionTableEvent(BaseEntityEventType.REMOVE_ENTITY, Arrays.asList(deletedIds)));

        addToConsolitatedMsg(FontAwesome.TRASH_O.getHtml() + SPUILabelDefinitions.HTML_SPACE
                + i18n.getMessage("message.dist.deleted", deletedIds.length));

        manageDistUIState.getDeletedDistributionList()
                .forEach(deletedIdname -> manageDistUIState.getAssignedList().remove(deletedIdname));
        removeCurrentTab(tab);
        manageDistUIState.getDeletedDistributionList().clear();
        eventBus.publish(this, SaveActionWindowEvent.DELETED_DISTRIBUTIONS);
    }

    private void discardDistAll(final ConfirmationTab tab) {
        removeCurrentTab(tab);
        manageDistUIState.getDeletedDistributionList().clear();
        setActionMessage(i18n.getMessage("message.dist.discard.success"));
        eventBus.publish(this, SaveActionWindowEvent.DISCARD_ALL_DISTRIBUTIONS);

    }

    private IndexedContainer getDistTableContainer() {
        final IndexedContainer contactContainer = new IndexedContainer();
        contactContainer.addContainerProperty(DIST_ID_NAME, DistributionSetIdName.class, "");
        contactContainer.addContainerProperty(DIST_NAME, String.class, "");
        for (final DistributionSetIdName distIdName : manageDistUIState.getDeletedDistributionList()) {
            final Item item = contactContainer.addItem(distIdName);
            item.getItemProperty(DIST_NAME).setValue(distIdName.getName().concat(":" + distIdName.getVersion()));
        }
        return contactContainer;

    }

    private void discardDistDelete(final Button.ClickEvent event, final Object itemId, final ConfirmationTab tab) {

        final DistributionSetIdName distId = (DistributionSetIdName) ((Button) event.getComponent()).getData();
        if (!CollectionUtils.isEmpty(manageDistUIState.getDeletedDistributionList())
                && manageDistUIState.getDeletedDistributionList().contains(distId)) {
            manageDistUIState.getDeletedDistributionList().remove(distId);
        }
        tab.getTable().getContainerDataSource().removeItem(itemId);
        final int deleteCount = tab.getTable().size();
        if (0 == deleteCount) {
            removeCurrentTab(tab);
            eventBus.publish(this, SaveActionWindowEvent.DISCARD_ALL_DISTRIBUTIONS);
        } else {
            eventBus.publish(this, SaveActionWindowEvent.DISCARD_DEL_DISTRIBUTION);
        }
    }

    /* For Distribution set Type */

    private ConfirmationTab createDistSetTypeDeleteConfirmationTab() {
        final ConfirmationTab tab = new ConfirmationTab();

        tab.getConfirmAll().setId(UIComponentIdProvider.SAVE_DELETE_DIST_SET_TYPE);
        tab.getConfirmAll().setIcon(FontAwesome.TRASH_O);
        tab.getConfirmAll().setCaption(i18n.getMessage(SPUILabelDefinitions.BUTTON_DELETE_ALL));
        tab.getConfirmAll().addClickListener(event -> deleteDistSetTypeAll(tab));

        tab.getDiscardAll().setCaption(i18n.getMessage(SPUILabelDefinitions.BUTTON_DISCARD_ALL));
        tab.getDiscardAll().setId(UIComponentIdProvider.DISCARD_DIST_SET_TYPE);
        tab.getDiscardAll().addClickListener(event -> discardDistSetTypeAll(tab));

        // Add items container to the table.
        tab.getTable().setContainerDataSource(getDistSetTypeTableContainer());

        // Add the discard action column
        tab.getTable().addGeneratedColumn(DISCARD, (source, itemId, columnId) -> {
            final ClickListener clickListener = event -> discardDistTypeDelete(
                    (String) ((Button) event.getComponent()).getData(), itemId, tab);
            return createDiscardButton(itemId, clickListener);

        });

        tab.getTable().setVisibleColumns(DIST_SET_NAME, DISCARD);
        tab.getTable().setColumnHeaders(i18n.getMessage("header.first.delete.dist.type.table"),
                i18n.getMessage("header.second.delete.dist.type.table"));

        tab.getTable().setColumnExpandRatio(DIST_SET_NAME, 2);
        tab.getTable().setColumnExpandRatio(DISCARD, SPUIDefinitions.DISCARD_COLUMN_WIDTH);
        tab.getTable().setColumnAlignment(DISCARD, Align.CENTER);
        return tab;
    }

    private void deleteDistSetTypeAll(final ConfirmationTab tab) {

        final int deleteDistTypeCount = manageDistUIState.getSelectedDeleteDistSetTypes().size();
        manageDistUIState.getSelectedDeleteDistSetTypes().stream()
                .map(deleteDistTypeName -> distributionSetTypeManagement
                        .getByName(deleteDistTypeName).get().getId())
                .forEach(distributionSetTypeManagement::delete);

        addToConsolitatedMsg(FontAwesome.TASKS.getHtml() + SPUILabelDefinitions.HTML_SPACE
                + i18n.getMessage("message.dist.type.delete", new Object[] { deleteDistTypeCount }));
        manageDistUIState.getSelectedDeleteDistSetTypes().clear();
        removeCurrentTab(tab);
        setActionMessage(i18n.getMessage("message.dist.set.type.deleted.success"));
        eventBus.publish(this, SaveActionWindowEvent.SAVED_DELETE_DIST_SET_TYPES);
    }

    private void discardDistSetTypeAll(final ConfirmationTab tab) {
        removeCurrentTab(tab);
        manageDistUIState.getSelectedDeleteDistSetTypes().clear();
        setActionMessage(i18n.getMessage("message.dist.type.discard.success"));
        eventBus.publish(this, SaveActionWindowEvent.DISCARD_ALL_DELETE_DIST_SET_TYPES);
    }

    private IndexedContainer getDistSetTypeTableContainer() {
        final IndexedContainer contactContainer = new IndexedContainer();
        contactContainer.addContainerProperty(DIST_SET_NAME, String.class, "");

        for (final String distTypeMName : manageDistUIState.getSelectedDeleteDistSetTypes()) {
            final Item saveTblitem = contactContainer.addItem(distTypeMName);

            saveTblitem.getItemProperty(DIST_SET_NAME).setValue(distTypeMName);
        }

        return contactContainer;

    }

    private void discardDistTypeDelete(final String discardDSType, final Object itemId, final ConfirmationTab tab) {

        if (!CollectionUtils.isEmpty(manageDistUIState.getSelectedDeleteDistSetTypes())
                && manageDistUIState.getSelectedDeleteDistSetTypes().contains(discardDSType)) {
            manageDistUIState.getSelectedDeleteDistSetTypes().remove(discardDSType);
        }
        tab.getTable().getContainerDataSource().removeItem(itemId);
        final int deleteCount = tab.getTable().size();
        if (0 == deleteCount) {
            removeCurrentTab(tab);
            eventBus.publish(this, SaveActionWindowEvent.DISCARD_ALL_DELETE_DIST_SET_TYPES);
        } else {
            eventBus.publish(this, SaveActionWindowEvent.DISCARD_DELETE_DIST_SET_TYPE);
        }
    }

    /* For Assign software modules */
    private ConfirmationTab createAssignSWModuleConfirmationTab() {

        assignmnetTab = new ConfirmationTab();

        assignmnetTab.getConfirmAll().setId(UIComponentIdProvider.SAVE_ASSIGNMENT);
        assignmnetTab.getConfirmAll().setIcon(FontAwesome.SAVE);
        assignmnetTab.getConfirmAll().setCaption(i18n.getMessage("button.assign.all"));
        assignmnetTab.getConfirmAll().addClickListener(event -> saveAllAssignments(assignmnetTab));

        assignmnetTab.getDiscardAll().setCaption(i18n.getMessage(SPUILabelDefinitions.BUTTON_DISCARD_ALL));
        assignmnetTab.getDiscardAll().setId(UIComponentIdProvider.DISCARD_ASSIGNMENT);
        assignmnetTab.getDiscardAll().addClickListener(event -> discardAllSWAssignments(assignmnetTab));

        // Add items container to the table.
        assignmnetTab.getTable().setContainerDataSource(getSWAssignmentsTableContainer());

        // Add the discard action column
        assignmnetTab.getTable().addGeneratedColumn(DISCARD, (source, itemId, columnId) -> {
            final StringBuilder style = new StringBuilder(ValoTheme.BUTTON_TINY);
            style.append(' ');
            style.append(SPUIStyleDefinitions.REDICON);
            final Button deleteIcon = SPUIComponentProvider.getButton("", "", SPUILabelDefinitions.DISCARD,
                    style.toString(), true, FontAwesome.REPLY, SPUIButtonStyleSmallNoBorder.class);
            deleteIcon.setData(itemId);
            deleteIcon.setImmediate(true);
            deleteIcon.addClickListener(event -> discardSWAssignment((String) ((Button) event.getComponent()).getData(),
                    itemId, assignmnetTab));
            return deleteIcon;
        });

        assignmnetTab.getTable().setVisibleColumns(DIST_NAME, SOFTWARE_MODULE_NAME, DISCARD);
        assignmnetTab.getTable().setColumnHeaders(i18n.getMessage("header.dist.first.assignment.table"),
                i18n.getMessage("header.dist.second.assignment.table"),
                i18n.getMessage("header.third.assignment.table"));

        assignmnetTab.getTable().setColumnExpandRatio(DIST_NAME, 2);
        assignmnetTab.getTable().setColumnExpandRatio(SOFTWARE_MODULE_NAME, 2);
        assignmnetTab.getTable().setColumnExpandRatio(DISCARD, SPUIDefinitions.DISCARD_COLUMN_WIDTH);
        assignmnetTab.getTable().setColumnAlignment(DISCARD, Align.CENTER);
        return assignmnetTab;

    }

    @SuppressWarnings("unchecked")
    private IndexedContainer getSWAssignmentsTableContainer() {
        final IndexedContainer contactContainer = new IndexedContainer();
        contactContainer.addContainerProperty(DIST_NAME, String.class, "");
        contactContainer.addContainerProperty(SOFTWARE_MODULE_NAME, String.class, "");
        contactContainer.addContainerProperty(DIST_ID_NAME, DistributionSetIdName.class, "");
        contactContainer.addContainerProperty(SOFTWARE_MODULE_ID_NAME, SoftwareModuleIdName.class, "");

        final Map<DistributionSetIdName, HashSet<SoftwareModuleIdName>> assignedList = manageDistUIState
                .getAssignedList();

        assignedList.forEach((distIdname, softIdNameSet) -> softIdNameSet.forEach(softIdName -> {
            final String itemId = HawkbitCommonUtil.concatStrings("|||", distIdname.getId().toString(),
                    softIdName.getId().toString());
            final Item saveTblitem = contactContainer.addItem(itemId);

            saveTblitem.getItemProperty(DIST_NAME).setValue(distIdname.getName().concat(":" + distIdname.getVersion()));

            saveTblitem.getItemProperty(SOFTWARE_MODULE_NAME).setValue(softIdName.getName());

            saveTblitem.getItemProperty(DIST_ID_NAME).setValue(distIdname);
            saveTblitem.getItemProperty(SOFTWARE_MODULE_ID_NAME).setValue(softIdName);
        }));
        return contactContainer;
    }

    private void saveAllAssignments(final ConfirmationTab tab) {
        manageDistUIState.getAssignedList().forEach((distIdName, softIdNameSet) -> {
            final List<Long> softIds = softIdNameSet.stream().map(softIdName -> softIdName.getId())
                    .collect(Collectors.toList());
            distributionSetManagement.assignSoftwareModules(distIdName.getId(), softIds);

        });

        int count = 0;
        for (final Entry<DistributionSetIdName, HashSet<SoftwareModuleIdName>> entry : manageDistUIState
                .getAssignedList().entrySet()) {
            count += entry.getValue().size();
        }
        addToConsolitatedMsg(FontAwesome.TASKS.getHtml() + SPUILabelDefinitions.HTML_SPACE
                + i18n.getMessage("message.software.assignment", new Object[] { count }));
        manageDistUIState.getAssignedList().clear();
        manageDistUIState.getConsolidatedDistSoftwarewList().clear();
        removeCurrentTab(tab);
        eventBus.publish(this, SaveActionWindowEvent.SAVED_ASSIGNMENTS);
    }

    private void discardAllSWAssignments(final ConfirmationTab tab) {
        removeCurrentTab(tab);
        manageDistUIState.getAssignedList().clear();
        manageDistUIState.getConsolidatedDistSoftwarewList().clear();
        setActionMessage(i18n.getMessage("message.assign.discard.success"));
        eventBus.publish(this, SaveActionWindowEvent.DISCARD_ALL_ASSIGNMENTS);
    }

    private void discardSWAssignment(final String discardSW, final Object itemId, final ConfirmationTab tab) {

        final Item rowitem = tab.getTable().getContainerDataSource().getItem(discardSW);
        final DistributionSetIdName discardDistIdName = (DistributionSetIdName) rowitem.getItemProperty(DIST_ID_NAME)
                .getValue();
        final SoftwareModuleIdName discardSoftIdName = (SoftwareModuleIdName) rowitem
                .getItemProperty(SOFTWARE_MODULE_ID_NAME).getValue();

        final Set<SoftwareModuleIdName> softIdNameSet = manageDistUIState.getAssignedList().get(discardDistIdName);
        manageDistUIState.getAssignedList().get(discardDistIdName).remove(discardSoftIdName);
        softIdNameSet.remove(discardSoftIdName);
        tab.getTable().getContainerDataSource().removeItem(itemId);
        if (softIdNameSet.isEmpty()) {
            manageDistUIState.getAssignedList().remove(discardDistIdName);
        }
        final Map<Long, HashSet<SoftwareModuleIdName>> map = manageDistUIState.getConsolidatedDistSoftwarewList()
                .get(discardDistIdName);
        map.keySet().forEach(typeId -> map.get(typeId).remove(discardSoftIdName));

        final int assigCount = tab.getTable().getContainerDataSource().size();
        if (0 == assigCount) {
            removeCurrentTab(tab);
            eventBus.publish(this, SaveActionWindowEvent.DISCARD_ALL_ASSIGNMENTS);
        } else {
            eventBus.publish(this, SaveActionWindowEvent.DISCARD_ASSIGNMENT);
        }

    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onEventDiscard(final SaveActionWindowEvent saveActionWindowEvent) {
        if (saveActionWindowEvent == SaveActionWindowEvent.DELETE_ALL_SOFWARE) {
            getSWAssignmentsTableContainer();
        }
    }

}
