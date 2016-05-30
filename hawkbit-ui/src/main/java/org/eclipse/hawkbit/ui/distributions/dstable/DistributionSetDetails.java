/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.distributions.dstable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.SoftwareManagement;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetIdName;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleIdName;
import org.eclipse.hawkbit.ui.artifacts.event.SoftwareModuleEvent;
import org.eclipse.hawkbit.ui.artifacts.event.SoftwareModuleEvent.SoftwareModuleEventType;
import org.eclipse.hawkbit.ui.common.detailslayout.AbstractNamedVersionedEntityTableDetailsLayout;
import org.eclipse.hawkbit.ui.common.detailslayout.SoftwareModuleDetailsTable;
import org.eclipse.hawkbit.ui.common.tagdetails.DistributionTagToken;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleSmallNoBorder;
import org.eclipse.hawkbit.ui.distributions.event.SaveActionWindowEvent;
import org.eclipse.hawkbit.ui.distributions.event.SoftwareModuleAssignmentDiscardEvent;
import org.eclipse.hawkbit.ui.distributions.state.ManageDistUIState;
import org.eclipse.hawkbit.ui.management.dstable.DistributionAddUpdateWindowLayout;
import org.eclipse.hawkbit.ui.management.event.DistributionTableEvent;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.SPUIComponentIdProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.vaadin.data.Item;
import com.vaadin.server.FontAwesome;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Label;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

/**
 * Distribution set details layout.
 *
 *
 *
 */
@SpringComponent
@ViewScope
public class DistributionSetDetails extends AbstractNamedVersionedEntityTableDetailsLayout<DistributionSet> {

    private static final long serialVersionUID = -4595004466943546669L;

    private static final String SOFT_MODULE = "softwareModule";

    private static final String UNASSIGN_SOFT_MODULE = "unassignSoftModule";

    @Autowired
    private ManageDistUIState manageDistUIState;

    @Autowired
    private DistributionAddUpdateWindowLayout distributionAddUpdateWindowLayout;

    @Autowired
    private DistributionTagToken distributionTagToken;

    @Autowired
    private transient SoftwareManagement softwareManagement;

    @Autowired
    private transient DistributionSetManagement distributionSetManagement;

    private SoftwareModuleDetailsTable softwareModuleTable;

    private VerticalLayout tagsLayout;

    Map<String, StringBuilder> assignedSWModule = new HashMap<>();

    /**
     * softwareLayout Initialize the component.
     */
    @Override
    protected void init() {
        softwareModuleTable = new SoftwareModuleDetailsTable();
        softwareModuleTable.init(getI18n(), true, getPermissionChecker(), distributionSetManagement, getEventBus(),
                manageDistUIState);
        super.init();
    }

    protected VerticalLayout createTagsLayout() {
        tagsLayout = getTabLayout();
        return tagsLayout;
    }

    @Override
    protected void populateDetailsWidget() {
        populateDetails();
        populateModule();
        populateTags();
    }

    private void populateModule() {
        softwareModuleTable.populateModule(getSelectedBaseEntity());
        showUnsavedAssignment();
    }

    @SuppressWarnings("unchecked")
    private void showUnsavedAssignment() {
        Item item;
        final Map<DistributionSetIdName, HashSet<SoftwareModuleIdName>> assignedList = manageDistUIState
                .getAssignedList();
        final Long selectedDistId = manageDistUIState.getLastSelectedDistribution().isPresent()
                ? manageDistUIState.getLastSelectedDistribution().get().getId() : null;
        Set<SoftwareModuleIdName> softwareModuleIdNameList = null;

        for (final Map.Entry<DistributionSetIdName, HashSet<SoftwareModuleIdName>> entry : assignedList.entrySet()) {
            if (entry.getKey().getId().equals(selectedDistId)) {
                softwareModuleIdNameList = entry.getValue();
                break;
            }
        }

        if (null != softwareModuleIdNameList) {
            for (final SoftwareModuleIdName swIdName : softwareModuleIdNameList) {
                final SoftwareModule softwareModule = softwareManagement.findSoftwareModuleById(swIdName.getId());
                if (assignedSWModule.containsKey(softwareModule.getType().getName())) {
                    assignedSWModule.get(softwareModule.getType().getName()).append("</br>").append("<I>")
                            .append(getUnsavedAssigedSwModule(softwareModule.getName(), softwareModule.getVersion()))
                            .append("<I>");

                } else {
                    assignedSWModule.put(softwareModule.getType().getName(),
                            new StringBuilder().append("<I>").append(
                                    getUnsavedAssigedSwModule(softwareModule.getName(), softwareModule.getVersion()))
                                    .append("<I>"));
                }

            }
            for (final Map.Entry<String, StringBuilder> entry : assignedSWModule.entrySet()) {
                item = softwareModuleTable.getContainerDataSource().getItem(entry.getKey());
                if (item != null) {
                    item.getItemProperty(SOFT_MODULE)
                            .setValue(HawkbitCommonUtil.getFormatedLabel(entry.getValue().toString()));
                    assignSoftModuleButton(item, entry);

                }
            }
        }
    }

    private void assignSoftModuleButton(final Item item, final Map.Entry<String, StringBuilder> entry) {
        if (getPermissionChecker().hasUpdateDistributionPermission() && distributionSetManagement
                .findDistributionSetById(manageDistUIState.getLastSelectedDistribution().get().getId())
                .getAssignedTargets().isEmpty()) {
            final Button reassignSoftModule = SPUIComponentProvider.getButton(entry.getKey(), "", "", "", true,
                    FontAwesome.TIMES, SPUIButtonStyleSmallNoBorder.class);
            reassignSoftModule.setEnabled(false);
            item.getItemProperty(UNASSIGN_SOFT_MODULE).setValue(reassignSoftModule);
        }
    }

    private String getUnsavedAssigedSwModule(final String name, final String version) {
        return HawkbitCommonUtil.getFormattedNameVersion(name, version);
    }

    @SuppressWarnings("unchecked")
    private void updateSoftwareModule(final SoftwareModule module) {

        softwareModuleTable.getContainerDataSource().getItemIds();
        if (assignedSWModule.containsKey(module.getType().getName())) {
            /*
             * If software module type is software, means multiple softwares can
             * assigned to that type. Hence if multipe softwares belongs to same
             * type is drroped, then add to the list.
             */

            if (module.getType().getMaxAssignments() == Integer.MAX_VALUE) {
                assignedSWModule.get(module.getType().getName()).append("</br>").append("<I>")
                        .append(getUnsavedAssigedSwModule(module.getName(), module.getVersion())).append("</I>");
            }

            /*
             * If software mocule type is firmware, means single software can be
             * assigned to that type. Hence if multiple softwares belongs to
             * same type is dropped, then override with previous one.
             */
            if (module.getType().getMaxAssignments() == 1) {
                assignedSWModule.put(module.getType().getName(), new StringBuilder().append("<I>")
                        .append(getUnsavedAssigedSwModule(module.getName(), module.getVersion())).append("</I>"));
            }

        } else {
            assignedSWModule.put(module.getType().getName(), new StringBuilder().append("<I>")
                    .append(getUnsavedAssigedSwModule(module.getName(), module.getVersion())).append("</I>"));
        }

        for (final Map.Entry<String, StringBuilder> entry : assignedSWModule.entrySet()) {
            final Item item = softwareModuleTable.getContainerDataSource().getItem(entry.getKey());
            if (item != null) {
                item.getItemProperty(SOFT_MODULE)
                        .setValue(HawkbitCommonUtil.getFormatedLabel(entry.getValue().toString()));
                assignSoftModuleButton(item, entry);

            }
        }
    }

    private VerticalLayout createSoftwareModuleTab() {
        final VerticalLayout softwareLayout = getTabLayout();
        softwareLayout.setSizeFull();
        softwareLayout.addComponent(softwareModuleTable);
        return softwareLayout;
    }

    private void populateTags() {
        tagsLayout.removeAllComponents();
        if (getSelectedBaseEntity() == null) {
            return;
        }
        tagsLayout.addComponent(distributionTagToken.getTokenField());
    }

    private void populateDetails() {
        if (getSelectedBaseEntity() != null) {
            updateDistributionSetDetailsLayout(getSelectedBaseEntity().getType().getName(),
                    getSelectedBaseEntity().isRequiredMigrationStep());
        } else {
            updateDistributionSetDetailsLayout(null, null);
        }
    }

    private void updateDistributionSetDetailsLayout(final String type, final Boolean isMigrationRequired) {
        final VerticalLayout detailsTabLayout = getDetailsLayout();
        detailsTabLayout.removeAllComponents();

        if (type != null) {
            final Label typeLabel = SPUIComponentProvider.createNameValueLabel(getI18n().get("label.dist.details.type"),
                    type);
            typeLabel.setId(SPUIComponentIdProvider.DETAILS_TYPE_LABEL_ID);
            detailsTabLayout.addComponent(typeLabel);
        }

        if (isMigrationRequired != null) {
            detailsTabLayout.addComponent(SPUIComponentProvider.createNameValueLabel(
                    getI18n().get("checkbox.dist.migration.required"),
                    isMigrationRequired.equals(Boolean.TRUE) ? getI18n().get("label.yes") : getI18n().get("label.no")));
        }
    }

    @Override
    protected void onEdit(final ClickEvent event) {
        final Window newDistWindow = distributionAddUpdateWindowLayout.getWindow();
        distributionAddUpdateWindowLayout.populateValuesOfDistribution(getSelectedBaseEntityId());
        newDistWindow.setCaption(getI18n().get("caption.update.dist"));
        UI.getCurrent().addWindow(newDistWindow);
        newDistWindow.setVisible(Boolean.TRUE);
    }

    @Override
    protected String getEditButtonId() {
        return SPUIComponentIdProvider.DS_EDIT_BUTTON;
    }

    @Override
    protected Boolean onLoadIsTableRowSelected() {
        return manageDistUIState.getSelectedDistributions().isPresent()
                && !manageDistUIState.getSelectedDistributions().get().isEmpty();
    }

    @Override
    protected Boolean onLoadIsTableMaximized() {
        return manageDistUIState.isDsTableMaximized();
    }

    @Override
    protected String getDefaultCaption() {
        return getI18n().get("distribution.details.header");
    }

    @Override
    protected void addTabs(final TabSheet detailsTab) {
        detailsTab.addTab(createDetailsLayout(), getI18n().get("caption.tab.details"), null);
        detailsTab.addTab(createDescriptionLayout(), getI18n().get("caption.tab.description"), null);
        detailsTab.addTab(createSoftwareModuleTab(), getI18n().get("caption.softwares.distdetail.tab"), null);
        detailsTab.addTab(createTagsLayout(), getI18n().get("caption.tags.tab"), null);
        detailsTab.addTab(createLogLayout(), getI18n().get("caption.logs.tab"), null);
    }

    @Override
    protected Boolean hasEditPermission() {
        return getPermissionChecker().hasUpdateDistributionPermission();
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final SoftwareModuleEvent event) {
        if (event.getSoftwareModuleEventType() == SoftwareModuleEventType.ASSIGN_SOFTWARE_MODULE) {
            UI.getCurrent().access(() -> updateSoftwareModule(event.getEntity()));
        }
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final DistributionTableEvent distributionTableEvent) {
        onBaseEntityEvent(distributionTableEvent);
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final SoftwareModuleAssignmentDiscardEvent softwareModuleAssignmentDiscardEvent) {
        if (softwareModuleAssignmentDiscardEvent.getDistributionSetIdName() != null) {
            UI.getCurrent().access(() -> {
                final DistributionSetIdName distIdName = softwareModuleAssignmentDiscardEvent
                        .getDistributionSetIdName();
                if (distIdName.getId().equals(getSelectedBaseEntityId())
                        && distIdName.getName().equals(getSelectedBaseEntity().getName())) {
                    setSelectedBaseEntity(
                            distributionSetManagement.findDistributionSetByIdWithDetails(getSelectedBaseEntityId()));
                    populateModule();
                }
            });
        }
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final SaveActionWindowEvent saveActionWindowEvent) {
        if ((saveActionWindowEvent == SaveActionWindowEvent.SAVED_ASSIGNMENTS
                || saveActionWindowEvent == SaveActionWindowEvent.DISCARD_ALL_ASSIGNMENTS)
                && getSelectedBaseEntity() != null) {
            assignedSWModule.clear();
            setSelectedBaseEntity(
                    distributionSetManagement.findDistributionSetByIdWithDetails(getSelectedBaseEntityId()));
            UI.getCurrent().access(() -> populateModule());
        }
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEventDiscard(final SaveActionWindowEvent saveActionWindowEvent) {
        if (saveActionWindowEvent == SaveActionWindowEvent.DISCARD_ASSIGNMENT
                || saveActionWindowEvent == SaveActionWindowEvent.DISCARD_ALL_ASSIGNMENTS
                || saveActionWindowEvent == SaveActionWindowEvent.DELETE_ALL_SOFWARE) {
            assignedSWModule.clear();
            showUnsavedAssignment();
        }
    }

    @Override
    protected String getTabSheetId() {
        return null;
    }

    @Override
    protected String getDetailsHeaderCaptionId() {
        return SPUIComponentIdProvider.DISTRIBUTION_DETAILS_HEADER_LABEL_ID;
    }

}
