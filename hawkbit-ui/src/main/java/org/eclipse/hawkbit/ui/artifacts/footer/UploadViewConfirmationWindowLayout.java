/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts.footer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.repository.SoftwareModuleTypeManagement;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.ui.artifacts.event.SoftwareModuleEvent;
import org.eclipse.hawkbit.ui.artifacts.event.UploadArtifactUIEvent;
import org.eclipse.hawkbit.ui.artifacts.state.ArtifactUploadState;
import org.eclipse.hawkbit.ui.artifacts.state.CustomFile;
import org.eclipse.hawkbit.ui.common.confirmwindow.layout.AbstractConfirmationWindowLayout;
import org.eclipse.hawkbit.ui.common.confirmwindow.layout.ConfirmationTab;
import org.eclipse.hawkbit.ui.common.table.BaseEntityEventType;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus.UIEventBus;

import com.google.common.collect.Maps;
import com.vaadin.data.Container;
import com.vaadin.data.Item;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Table.Align;

/**
 * Layout of confirm actions window on the Upload View.
 */
public class UploadViewConfirmationWindowLayout extends AbstractConfirmationWindowLayout {

    private static final long serialVersionUID = 1L;

    private static final String SW_MODULE_NAME_MSG = "SW MOdule Name";

    private static final String SW_DISCARD_CHGS = "DiscardChanges";

    private static final String SW_MODULE_TYPE_NAME = "SoftwareModuleTypeName";

    private static final String DISCARD = "Discard";

    private final transient SoftwareModuleTypeManagement softwareModuleTypeManagement;

    private final transient SoftwareModuleManagement softwareModuleManagement;

    private final ArtifactUploadState artifactUploadState;

    UploadViewConfirmationWindowLayout(final VaadinMessageSource i18n, final UIEventBus eventBus,
            final SoftwareModuleManagement softwareModuleManagement,
            final SoftwareModuleTypeManagement softwareModuleTypeManagement,
            final ArtifactUploadState artifactUploadState) {
        super(i18n, eventBus);
        this.softwareModuleTypeManagement = softwareModuleTypeManagement;
        this.softwareModuleManagement = softwareModuleManagement;
        this.artifactUploadState = artifactUploadState;
    }

    @Override
    protected Map<String, ConfirmationTab> getConfirmationTabs() {
        final Map<String, ConfirmationTab> tabs = Maps.newHashMapWithExpectedSize(2);
        if (!artifactUploadState.getDeleteSofwareModules().isEmpty()) {
            tabs.put(i18n.getMessage("caption.delete.swmodule.accordion.tab"), createSMDeleteConfirmationTab());
        }
        if (!artifactUploadState.getSelectedDeleteSWModuleTypes().isEmpty()) {
            tabs.put(i18n.getMessage("caption.delete.sw.module.type.accordion.tab"),
                    createSMtypeDeleteConfirmationTab());
        }
        return tabs;
    }

    private ConfirmationTab createSMDeleteConfirmationTab() {
        final ConfirmationTab tab = new ConfirmationTab();

        tab.getConfirmAll().setId(UIComponentIdProvider.SW_DELETE_ALL);
        tab.getConfirmAll().setIcon(FontAwesome.TRASH_O);
        tab.getConfirmAll().setCaption(i18n.getMessage("button.delete.all"));
        tab.getConfirmAll().addClickListener(event -> deleteSMAll(tab));

        tab.getDiscardAll().setCaption(i18n.getMessage("button.discard.all"));
        tab.getDiscardAll().addClickListener(event -> discardSMAll(tab));

        // Add items container to the table.
        tab.getTable().setContainerDataSource(getSWModuleTableContainer());

        // Add the discard action column
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
        for (final Long swModuleID : artifactUploadState.getDeleteSofwareModules().keySet()) {
            final Item item = swcontactContainer.addItem(swModuleID);
            item.getItemProperty("SWModuleId").setValue(swModuleID.toString());
            item.getItemProperty(SW_MODULE_NAME_MSG)
                    .setValue(artifactUploadState.getDeleteSofwareModules().get(swModuleID));
        }
        return swcontactContainer;
    }

    private void discardSoftwareDelete(final Button.ClickEvent event, final Object itemId, final ConfirmationTab tab) {

        final Long swmoduleId = (Long) ((Button) event.getComponent()).getData();
        if (null != artifactUploadState.getDeleteSofwareModules()
                && !artifactUploadState.getDeleteSofwareModules().isEmpty()
                && artifactUploadState.getDeleteSofwareModules().containsKey(swmoduleId)) {
            artifactUploadState.getDeleteSofwareModules().remove(swmoduleId);
        }
        tab.getTable().getContainerDataSource().removeItem(itemId);
        final int deleteCount = tab.getTable().size();
        if (0 == deleteCount) {
            removeCurrentTab(tab);
            eventBus.publish(this, UploadArtifactUIEvent.DISCARD_ALL_DELETE_SOFTWARE);
        } else {
            eventBus.publish(this, UploadArtifactUIEvent.DISCARD_DELETE_SOFTWARE);
        }
    }

    private void deleteSMAll(final ConfirmationTab tab) {
        final Set<Long> swmoduleIds = artifactUploadState.getDeleteSofwareModules().keySet();
        softwareModuleManagement.delete(swmoduleIds);
        eventBus.publish(this, new SoftwareModuleEvent(BaseEntityEventType.REMOVE_ENTITY, swmoduleIds));

        addToConsolitatedMsg(FontAwesome.TRASH_O.getHtml() + SPUILabelDefinitions.HTML_SPACE
                + i18n.getMessage("message.swModule.deleted", artifactUploadState.getDeleteSofwareModules().size()));
        /*
         * Check if any information / files pending to upload for the deleted
         * software modules. If so, then delete the files from the upload list.
         */
        final List<CustomFile> tobeRemoved = new ArrayList<>();
        for (final Long id : swmoduleIds) {
            final String deleteSoftwareNameVersion = artifactUploadState.getDeleteSofwareModules().get(id);

            for (final CustomFile customFile : artifactUploadState.getFileSelected()) {
                final String swNameVersion = HawkbitCommonUtil.getFormattedNameVersion(
                        customFile.getBaseSoftwareModuleName(), customFile.getBaseSoftwareModuleVersion());
                if (deleteSoftwareNameVersion != null && deleteSoftwareNameVersion.equals(swNameVersion)) {
                    tobeRemoved.add(customFile);
                }
            }
        }
        if (!tobeRemoved.isEmpty()) {
            artifactUploadState.getFileSelected().removeAll(tobeRemoved);
        }
        artifactUploadState.getDeleteSofwareModules().clear();
        removeCurrentTab(tab);
        setActionMessage(i18n.getMessage("message.software.delete.success"));
        eventBus.publish(this, UploadArtifactUIEvent.DELETED_ALL_SOFWARE);
    }

    private void discardSMAll(final ConfirmationTab tab) {
        removeCurrentTab(tab);
        artifactUploadState.getDeleteSofwareModules().clear();
        setActionMessage(i18n.getMessage("message.software.discard.success"));
        eventBus.publish(this, UploadArtifactUIEvent.DISCARD_ALL_DELETE_SOFTWARE);
    }

    private ConfirmationTab createSMtypeDeleteConfirmationTab() {
        final ConfirmationTab tab = new ConfirmationTab();

        tab.getConfirmAll().setId(UIComponentIdProvider.SAVE_DELETE_SW_MODULE_TYPE);
        tab.getConfirmAll().setIcon(FontAwesome.TRASH_O);
        tab.getConfirmAll().setCaption(i18n.getMessage("button.delete.all"));
        tab.getConfirmAll().addClickListener(event -> deleteSMtypeAll(tab));

        tab.getDiscardAll().setCaption(i18n.getMessage("button.discard.all"));
        tab.getDiscardAll().addClickListener(event -> discardSMtypeAll(tab));

        // Add items container to the table.
        tab.getTable().setContainerDataSource(getSWModuleTypeTableContainer());

        // Add the discard action column
        tab.getTable().addGeneratedColumn(DISCARD, (source, itemId, columnId) -> {
            final ClickListener clickListener = event -> discardSoftwareTypeDelete(
                    (String) ((Button) event.getComponent()).getData(), itemId, tab);
            return createDiscardButton(itemId, clickListener);
        });

        tab.getTable().setVisibleColumns(SW_MODULE_TYPE_NAME, DISCARD);
        tab.getTable().setColumnHeaders(i18n.getMessage("header.first.delete.swmodule.type.table"),
                i18n.getMessage("header.second.delete.swmodule.type.table"));

        tab.getTable().setColumnExpandRatio(SW_MODULE_TYPE_NAME, 2);
        tab.getTable().setColumnExpandRatio(SW_DISCARD_CHGS, SPUIDefinitions.DISCARD_COLUMN_WIDTH);
        tab.getTable().setColumnAlignment(SW_DISCARD_CHGS, Align.CENTER);
        return tab;
    }

    private Container getSWModuleTypeTableContainer() {
        final IndexedContainer contactContainer = new IndexedContainer();
        contactContainer.addContainerProperty(SW_MODULE_TYPE_NAME, String.class, "");
        for (final String swModuleTypeName : artifactUploadState.getSelectedDeleteSWModuleTypes()) {
            final Item saveTblitem = contactContainer.addItem(swModuleTypeName);
            saveTblitem.getItemProperty(SW_MODULE_TYPE_NAME).setValue(swModuleTypeName);
        }
        return contactContainer;
    }

    private void discardSoftwareTypeDelete(final String discardSWModuleType, final Object itemId,
            final ConfirmationTab tab) {
        if (null != artifactUploadState.getSelectedDeleteSWModuleTypes()
                && !artifactUploadState.getSelectedDeleteSWModuleTypes().isEmpty()
                && artifactUploadState.getSelectedDeleteSWModuleTypes().contains(discardSWModuleType)) {
            artifactUploadState.getSelectedDeleteSWModuleTypes().remove(discardSWModuleType);
        }
        tab.getTable().getContainerDataSource().removeItem(itemId);
        final int deleteCount = tab.getTable().size();
        if (0 == deleteCount) {
            removeCurrentTab(tab);
            eventBus.publish(this, UploadArtifactUIEvent.DISCARD_ALL_DELETE_SOFTWARE_TYPE);
        } else {
            eventBus.publish(this, UploadArtifactUIEvent.DISCARD_DELETE_SOFTWARE_TYPE);
        }
    }

    private void deleteSMtypeAll(final ConfirmationTab tab) {
        final int deleteSWModuleTypeCount = artifactUploadState.getSelectedDeleteSWModuleTypes().size();
        for (final String swModuleTypeName : artifactUploadState.getSelectedDeleteSWModuleTypes()) {
            softwareModuleTypeManagement.getByName(swModuleTypeName).map(SoftwareModuleType::getId)
                    .ifPresent(softwareModuleTypeManagement::delete);
        }
        addToConsolitatedMsg(FontAwesome.TASKS.getHtml() + SPUILabelDefinitions.HTML_SPACE
                + i18n.getMessage("message.sw.module.type.delete", new Object[] { deleteSWModuleTypeCount }));
        artifactUploadState.getSelectedDeleteSWModuleTypes().clear();
        removeCurrentTab(tab);
        setActionMessage(i18n.getMessage("message.software.type.delete.success"));
        eventBus.publish(this, UploadArtifactUIEvent.DELETED_ALL_SOFWARE_TYPE);
    }

    private void discardSMtypeAll(final ConfirmationTab tab) {
        removeCurrentTab(tab);
        artifactUploadState.getSelectedDeleteSWModuleTypes().clear();
        setActionMessage(i18n.getMessage("message.software.type.discard.success"));
        eventBus.publish(this, UploadArtifactUIEvent.DISCARD_ALL_DELETE_SOFTWARE_TYPE);
    }
}
