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
import java.util.Map;
import java.util.Set;

import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.DistributionSetTagManagement;
import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.artifacts.event.SoftwareModuleEvent;
import org.eclipse.hawkbit.ui.artifacts.event.SoftwareModuleEvent.SoftwareModuleEventType;
import org.eclipse.hawkbit.ui.common.detailslayout.AbstractDistributionSetDetails;
import org.eclipse.hawkbit.ui.common.detailslayout.SoftwareModuleDetailsTable;
import org.eclipse.hawkbit.ui.common.detailslayout.TargetFilterQueryDetailsTable;
import org.eclipse.hawkbit.ui.common.entity.SoftwareModuleIdName;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleSmallNoBorder;
import org.eclipse.hawkbit.ui.distributions.event.SaveActionWindowEvent;
import org.eclipse.hawkbit.ui.distributions.state.ManageDistUIState;
import org.eclipse.hawkbit.ui.management.dstable.DistributionAddUpdateWindowLayout;
import org.eclipse.hawkbit.ui.management.state.ManagementUIState;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus.UIEventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.vaadin.data.Item;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

/**
 * Distribution set details layout.
 */
public class DistributionSetDetails extends AbstractDistributionSetDetails {

    private static final long serialVersionUID = 1L;

    private static final String SOFT_MODULE = "softwareModule";

    private final ManageDistUIState manageDistUIState;

    private final transient SoftwareModuleManagement softwareModuleManagement;

    private final transient TargetManagement targetManagement;

    private final TargetFilterQueryDetailsTable tfqDetailsTable;

    private Map<String, StringBuilder> assignedSWModule;

    DistributionSetDetails(final VaadinMessageSource i18n, final UIEventBus eventBus,
            final SpPermissionChecker permissionChecker, final ManageDistUIState manageDistUIState,
            final ManagementUIState managementUIState,
            final DistributionAddUpdateWindowLayout distributionAddUpdateWindowLayout,
            final SoftwareModuleManagement softwareManagement,
            final DistributionSetManagement distributionSetManagement, final TargetManagement targetManagement,
            final UINotification uiNotification, final DistributionSetTagManagement distributionSetTagManagement,
            final DsMetadataPopupLayout dsMetadataPopupLayout) {
        super(i18n, eventBus, permissionChecker, managementUIState, distributionAddUpdateWindowLayout,
                distributionSetManagement, dsMetadataPopupLayout, uiNotification, distributionSetTagManagement,
                createSoftwareModuleDetailsTable(i18n, permissionChecker, distributionSetManagement, eventBus,
                        manageDistUIState, uiNotification));
        this.manageDistUIState = manageDistUIState;
        this.softwareModuleManagement = softwareManagement;
        this.targetManagement = targetManagement;

        tfqDetailsTable = new TargetFilterQueryDetailsTable(i18n);

        addAdditionalTab();
        restoreState();
    }

    private void addAdditionalTab() {
        getDetailsTab().addTab(tfqDetailsTable, getI18n().getMessage("caption.auto.assignment.ds"), null);
    }

    private static final SoftwareModuleDetailsTable createSoftwareModuleDetailsTable(final VaadinMessageSource i18n,
            final SpPermissionChecker permissionChecker, final DistributionSetManagement distributionSetManagement,
            final UIEventBus eventBus, final ManageDistUIState manageDistUIState, final UINotification uiNotification) {
        return new SoftwareModuleDetailsTable(i18n, true, permissionChecker, distributionSetManagement, eventBus,
                manageDistUIState, uiNotification);
    }

    @Override
    protected void populateDetailsWidget() {
        populateDetails();
        populateModule();
        populateTags(getDistributionTagToken());
        populateMetadataDetails();
        populateTargetFilterQueries();
    }

    @Override
    protected void populateModule() {
        super.populateModule();
        showUnsavedAssignment();
    }

    @SuppressWarnings("unchecked")
    private void showUnsavedAssignment() {
        final Set<SoftwareModuleIdName> softwareModuleIdNameList = manageDistUIState.getLastSelectedDistribution()
                .map(selectedDistId -> manageDistUIState.getAssignedList().entrySet().stream()
                        .filter(entry -> entry.getKey().getId().equals(selectedDistId)).findAny()
                        .map(Map.Entry::getValue).orElse(null))
                .orElse(null);

        if (softwareModuleIdNameList != null) {
            if (assignedSWModule == null) {
                assignedSWModule = new HashMap<>();
            }

            softwareModuleIdNameList.stream().map(SoftwareModuleIdName::getId).map(softwareModuleManagement::get)
                    .forEach(found -> found.ifPresent(softwareModule -> {

                        if (assignedSWModule.containsKey(softwareModule.getType().getName())) {
                            assignedSWModule.get(softwareModule.getType().getName()).append("</br>").append("<I>")
                                    .append(HawkbitCommonUtil.getFormattedNameVersion(softwareModule.getName(),
                                            softwareModule.getVersion()))
                                    .append("<I>");

                        } else {
                            assignedSWModule
                                    .put(softwareModule.getType().getName(),
                                            new StringBuilder().append("<I>")
                                                    .append(HawkbitCommonUtil.getFormattedNameVersion(
                                                            softwareModule.getName(), softwareModule.getVersion()))
                                                    .append("<I>"));
                        }

                    }));
            for (final Map.Entry<String, StringBuilder> entry : assignedSWModule.entrySet()) {
                final Item item = getSoftwareModuleTable().getContainerDataSource().getItem(entry.getKey());
                if (item != null) {
                    item.getItemProperty(SOFT_MODULE).setValue(createSoftModuleLayout(entry.getValue().toString()));
                }
            }
        }
    }

    private Button assignSoftModuleButton(final String softwareModuleName) {
        if (getPermissionChecker().hasUpdateRepositoryPermission() && manageDistUIState.getLastSelectedDistribution()
                .map(selected -> targetManagement.countByAssignedDistributionSet(selected) <= 0).orElse(false)) {

            final Button reassignSoftModule = SPUIComponentProvider.getButton(softwareModuleName, "", "", "", true,
                    FontAwesome.TIMES, SPUIButtonStyleSmallNoBorder.class);
            reassignSoftModule.setEnabled(false);
            return reassignSoftModule;
        }
        return null;
    }

    private static String getUnsavedAssigedSwModule(final String name, final String version) {
        return HawkbitCommonUtil.getFormattedNameVersion(name, version);
    }

    @SuppressWarnings("unchecked")
    private void updateSoftwareModule(final SoftwareModule module) {
        if (assignedSWModule == null) {
            assignedSWModule = new HashMap<>();
        }

        getSoftwareModuleTable().getContainerDataSource().getItemIds();
        if (assignedSWModule.containsKey(module.getType().getName())) {
            /*
             * If software module type is software, means multiple softwares can
             * assigned to that type. Hence if multipe softwares belongs to same
             * type is drroped, then add to the list.
             */

            if (module.getType().getMaxAssignments() > 1) {
                assignedSWModule.get(module.getType().getName()).append("</br>").append("<I>")
                        .append(getUnsavedAssigedSwModule(module.getName(), module.getVersion())).append("</I>");
            }

            /*
             * If software module type is firmware, means single software can be
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
            final Item item = getSoftwareModuleTable().getContainerDataSource().getItem(entry.getKey());
            if (item != null) {
                item.getItemProperty(SOFT_MODULE).setValue(createSoftModuleLayout(entry.getValue().toString()));
            }
        }
    }

    private VerticalLayout createSoftModuleLayout(final String softwareModuleName) {
        final VerticalLayout verticalLayout = new VerticalLayout();
        final HorizontalLayout horizontalLayout = new HorizontalLayout();
        horizontalLayout.setSizeFull();
        final Label softwareModule = HawkbitCommonUtil.getFormatedLabel("");
        final Button reassignSoftModule = assignSoftModuleButton(softwareModuleName);
        softwareModule.setValue(softwareModuleName);
        softwareModule.setDescription(softwareModuleName);
        softwareModule.setId(softwareModuleName + "-label");
        horizontalLayout.addComponent(softwareModule);
        horizontalLayout.setExpandRatio(softwareModule, 1F);
        horizontalLayout.addComponent(reassignSoftModule);
        verticalLayout.addComponent(horizontalLayout);
        return verticalLayout;
    }

    protected void populateTargetFilterQueries() {
        tfqDetailsTable.populateTableByDistributionSet(getSelectedBaseEntity());
    }

    @Override
    protected boolean onLoadIsTableMaximized() {
        return manageDistUIState.isDsTableMaximized();
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onEvent(final SoftwareModuleEvent event) {
        if (event.getSoftwareModuleEventType() == SoftwareModuleEventType.ASSIGN_SOFTWARE_MODULE) {
            UI.getCurrent().access(() -> updateSoftwareModule(event.getEntity()));
        }
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onEvent(final SaveActionWindowEvent saveActionWindowEvent) {
        if ((saveActionWindowEvent == SaveActionWindowEvent.SAVED_ASSIGNMENTS
                || saveActionWindowEvent == SaveActionWindowEvent.DISCARD_ALL_ASSIGNMENTS)
                && getSelectedBaseEntity() != null) {
            if (assignedSWModule != null) {
                assignedSWModule.clear();
            }

            getDistributionSetManagement().getWithDetails(getSelectedBaseEntityId()).ifPresent(set -> {
                setSelectedBaseEntity(set);
                UI.getCurrent().access(this::populateModule);
            });
        }
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onEventDiscard(final SaveActionWindowEvent saveActionWindowEvent) {
        if (saveActionWindowEvent == SaveActionWindowEvent.DISCARD_ASSIGNMENT
                || saveActionWindowEvent == SaveActionWindowEvent.DISCARD_ALL_ASSIGNMENTS
                || saveActionWindowEvent == SaveActionWindowEvent.DELETE_ALL_SOFWARE) {
            if (assignedSWModule != null) {
                assignedSWModule.clear();
            }
            showUnsavedAssignment();
        }
    }

}
