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

import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.DistributionSetTagManagement;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.artifacts.event.SoftwareModuleEvent;
import org.eclipse.hawkbit.ui.artifacts.event.SoftwareModuleEvent.SoftwareModuleEventType;
import org.eclipse.hawkbit.ui.common.detailslayout.AbstractDistributionSetDetails;
import org.eclipse.hawkbit.ui.common.detailslayout.SoftwareModuleDetailsTable;
import org.eclipse.hawkbit.ui.common.detailslayout.TargetFilterQueryDetailsTable;
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

    private final TargetFilterQueryDetailsTable tfqDetailsTable;

    private Map<String, StringBuilder> assignedSWModule;

    DistributionSetDetails(final VaadinMessageSource i18n, final UIEventBus eventBus,
            final SpPermissionChecker permissionChecker, final ManageDistUIState manageDistUIState,
            final ManagementUIState managementUIState,
            final DistributionAddUpdateWindowLayout distributionAddUpdateWindowLayout,
            final DistributionSetManagement distributionSetManagement, final UINotification uiNotification,
            final DistributionSetTagManagement distributionSetTagManagement,
            final DsMetadataPopupLayout dsMetadataPopupLayout, final TenantConfigurationManagement configManagement,
            final SystemSecurityContext systemSecurityContext) {
        super(i18n, eventBus, permissionChecker, managementUIState, distributionAddUpdateWindowLayout,
                distributionSetManagement, dsMetadataPopupLayout, uiNotification, distributionSetTagManagement,
                createSoftwareModuleDetailsTable(i18n, permissionChecker, distributionSetManagement, eventBus,
                        manageDistUIState, uiNotification),
                configManagement, systemSecurityContext);
        this.manageDistUIState = manageDistUIState;

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

    private static String getUnsavedAssignedSwModule(final String name, final String version) {
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
             * If the module type allows multiple assignments, just append the
             * module entry to the list.
             */
            if (module.getType().getMaxAssignments() > 1) {
                assignedSWModule.get(module.getType().getName()).append("</br>").append("<I>")
                        .append(getUnsavedAssignedSwModule(module.getName(), module.getVersion())).append("</I>");
            }

            /*
             * If the module type does not allow multiple assignments, override
             * the previous module entry.
             */
            if (module.getType().getMaxAssignments() == 1) {
                assignedSWModule.put(module.getType().getName(), new StringBuilder().append("<I>")
                        .append(getUnsavedAssignedSwModule(module.getName(), module.getVersion())).append("</I>"));
            }

        } else {
            assignedSWModule.put(module.getType().getName(), new StringBuilder().append("<I>")
                    .append(getUnsavedAssignedSwModule(module.getName(), module.getVersion())).append("</I>"));
        }

        for (final Map.Entry<String, StringBuilder> entry : assignedSWModule.entrySet()) {
            final Item item = getSoftwareModuleTable().getContainerDataSource().getItem(entry.getKey());
            if (item != null) {
                item.getItemProperty(SOFT_MODULE).setValue(createSoftwareModuleLayout(entry.getValue().toString()));
            }
        }
    }

    private static VerticalLayout createSoftwareModuleLayout(final String softwareModuleName) {
        final VerticalLayout verticalLayout = new VerticalLayout();
        final HorizontalLayout horizontalLayout = new HorizontalLayout();
        horizontalLayout.setSizeFull();
        final Label softwareModule = HawkbitCommonUtil.getFormatedLabel("");
        softwareModule.setValue(softwareModuleName);
        softwareModule.setDescription(softwareModuleName);
        softwareModule.setId(softwareModuleName + "-label");
        horizontalLayout.addComponent(softwareModule);
        horizontalLayout.setExpandRatio(softwareModule, 1F);
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
            clearAssignments();
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
            clearAssignments();
        }
    }

    private void clearAssignments() {
        if (assignedSWModule != null) {
            assignedSWModule.clear();
        }
    }

}
