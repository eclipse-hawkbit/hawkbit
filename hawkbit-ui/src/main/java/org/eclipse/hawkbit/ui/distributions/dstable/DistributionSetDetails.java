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

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.SoftwareManagement;
import org.eclipse.hawkbit.repository.SpPermissionChecker;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetIdName;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleIdName;
import org.eclipse.hawkbit.ui.artifacts.event.SoftwareModuleEvent;
import org.eclipse.hawkbit.ui.artifacts.event.SoftwareModuleEvent.SoftwareModuleEventType;
import org.eclipse.hawkbit.ui.common.detailslayout.AbstractTableDetailsLayout;
import org.eclipse.hawkbit.ui.common.detailslayout.SoftwareModuleDetailsTable;
import org.eclipse.hawkbit.ui.common.tagdetails.DistributionTagToken;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleSmallNoBorder;
import org.eclipse.hawkbit.ui.distributions.event.SaveActionWindowEvent;
import org.eclipse.hawkbit.ui.distributions.event.SoftwareModuleAssignmentDiscardEvent;
import org.eclipse.hawkbit.ui.distributions.state.ManageDistUIState;
import org.eclipse.hawkbit.ui.management.dstable.DistributionAddUpdateWindowLayout;
import org.eclipse.hawkbit.ui.management.event.DistributionTableEvent;
import org.eclipse.hawkbit.ui.management.event.DistributionTableEvent.DistributionComponentEvent;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.SPUIComponetIdProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.events.EventBus;
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
public class DistributionSetDetails extends AbstractTableDetailsLayout {

    private static final long serialVersionUID = -4595004466943546669L;

    private static final String SOFT_MODULE = "softwareModule";

    private static final String UNASSIGN_SOFT_MODULE = "unassignSoftModule";

    @Autowired
    private I18N i18n;

    @Autowired
    private transient EventBus.SessionEventBus eventBus;

    @Autowired
    private SpPermissionChecker permissionChecker;

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

    private DistributionSet selectedDsModule;

    private Long dsId;

    private UI ui;

    Map<String, StringBuilder> assignedSWModule = new HashMap<>();

    /**
     * softwareLayout Initialize the component.
     */
    @Override
    @PostConstruct
    protected void init() {
        softwareModuleTable = new SoftwareModuleDetailsTable();
        softwareModuleTable.init(i18n, true, permissionChecker, distributionSetManagement, eventBus, manageDistUIState);
        super.init();
        ui = UI.getCurrent();
        eventBus.subscribe(this);
    }

    protected VerticalLayout createTagsLayout() {
        tagsLayout = getTabLayout();
        return tagsLayout;
    }

    private void populateDetailsWidget(final DistributionSet ds) {
        if (ds != null) {
            setDsId(ds.getId());
            setName(getDefaultCaption(), HawkbitCommonUtil.getFormattedNameVersion(ds.getName(), ds.getVersion()));
            populateDetails(ds);
            populateDescription(ds);
            populateLog(ds);
            populteModule(ds);
            populateTags(ds);
        } else {
            setDsId(null);
            setName(getDefaultCaption(), HawkbitCommonUtil.SP_STRING_EMPTY);
            populateDetails(null);
            populateDescription(null);
            populteModule(null);
            populateTags(null);
            populateLog(null);
        }
    }

    private void populteModule(final DistributionSet distributionSet) {
        softwareModuleTable.populateModule(distributionSet);
        showUnsavedAssignment();
    }

    @SuppressWarnings("unchecked")
    private void showUnsavedAssignment() {
        Item item;
        final Map<DistributionSetIdName, Set<SoftwareModuleIdName>> assignedList = manageDistUIState.getAssignedList();
        final Long selectedDistId = manageDistUIState.getLastSelectedDistribution().isPresent()
                ? manageDistUIState.getLastSelectedDistribution().get().getId() : null;
        Set<SoftwareModuleIdName> softwareModuleIdNameList = null;

        for (final Map.Entry<DistributionSetIdName, Set<SoftwareModuleIdName>> entry : assignedList.entrySet()) {
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

    /**
     * @param item
     * @param entry
     */
    private void assignSoftModuleButton(final Item item, final Map.Entry<String, StringBuilder> entry) {
        if (permissionChecker.hasUpdateDistributionPermission() && distributionSetManagement
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

    private void populateTags(final DistributionSet ds) {
        tagsLayout.removeAllComponents();
        if (null != ds) {
            tagsLayout.addComponent(distributionTagToken.getTokenField());
        }
    }

    private void populateLog(final DistributionSet ds) {
        if (null != ds) {
            updateLogLayout(getLogLayout(), ds.getLastModifiedAt(), ds.getLastModifiedBy(), ds.getCreatedAt(),
                    ds.getCreatedBy(), i18n);
        } else {
            updateLogLayout(getLogLayout(), null, HawkbitCommonUtil.SP_STRING_EMPTY, null, null, i18n);
        }
    }

    private void populateDetails(final DistributionSet ds) {
        if (ds != null) {
            updateDistributionSetDetailsLayout(ds.getType().getName(), ds.isRequiredMigrationStep());
        } else {
            updateDistributionSetDetailsLayout(null, null);
        }
    }

    private void populateDescription(final DistributionSet ds) {
        if (ds != null) {
            updateDescriptionLayout(i18n.get("label.description"), ds.getDescription());
        } else {
            updateDescriptionLayout(i18n.get("label.description"), null);
        }
    }

    private void updateDistributionSetDetailsLayout(final String type, final Boolean isMigrationRequired) {
        final VerticalLayout detailsTabLayout = getDetailsLayout();
        detailsTabLayout.removeAllComponents();

        if (type != null) {
            final Label typeLabel = SPUIComponentProvider.createNameValueLabel(i18n.get("label.dist.details.type"),
                    type);
            typeLabel.setId(SPUIComponetIdProvider.DETAILS_TYPE_LABEL_ID);
            detailsTabLayout.addComponent(typeLabel);
        }

        if (isMigrationRequired != null) {
            detailsTabLayout.addComponent(
                    SPUIComponentProvider.createNameValueLabel(i18n.get("checkbox.dist.migration.required"),
                            isMigrationRequired.equals(Boolean.TRUE) ? i18n.get("label.yes") : i18n.get("label.no")));
        }
    }

    public Long getDsId() {
        return dsId;
    }

    public void setDsId(final Long dsId) {
        this.dsId = dsId;
    }

    /*
     * (non-Javadoc)
     * 
     * @see hawkbit.server.ui.common.detailslayout.AbstractTableDetailsLayout#
     * onEdit(com.vaadin.ui .Button.ClickEvent)
     */
    @Override
    protected void onEdit(final ClickEvent event) {
        final Window newDistWindow = distributionAddUpdateWindowLayout.getWindow();
        distributionAddUpdateWindowLayout.populateValuesOfDistribution(getDsId());
        newDistWindow.setCaption(i18n.get("caption.update.dist"));
        UI.getCurrent().addWindow(newDistWindow);
        newDistWindow.setVisible(Boolean.TRUE);
    }

    /*
     * (non-Javadoc)
     * 
     * @see hawkbit.server.ui.common.detailslayout.AbstractTableDetailsLayout#
     * getEditButtonId()
     */
    @Override
    protected String getEditButtonId() {
        return SPUIComponetIdProvider.DS_EDIT_BUTTON;
    }

    /*
     * (non-Javadoc)
     * 
     * @see hawkbit.server.ui.common.detailslayout.AbstractTableDetailsLayout#
     * onLoadIsSwModuleSelected ()
     */
    @Override
    protected Boolean onLoadIsTableRowSelected() {
        return manageDistUIState.getSelectedDistributions().isPresent()
                && !manageDistUIState.getSelectedDistributions().get().isEmpty();
    }

    /*
     * (non-Javadoc)
     * 
     * @see hawkbit.server.ui.common.detailslayout.AbstractTableDetailsLayout#
     * onLoadIsTableMaximized ()
     */
    @Override
    protected Boolean onLoadIsTableMaximized() {
        return manageDistUIState.isDsTableMaximized();
    }

    /*
     * (non-Javadoc)
     * 
     * @see hawkbit.server.ui.common.detailslayout.AbstractTableDetailsLayout#
     * populateDetailsWidget()
     */
    @Override
    protected void populateDetailsWidget() {
        populateDetailsWidget(selectedDsModule);
    }

    /*
     * (non-Javadoc)
     * 
     * @see hawkbit.server.ui.common.detailslayout.AbstractTableDetailsLayout#
     * getDefaultCaption()
     */
    @Override
    protected String getDefaultCaption() {
        return i18n.get("distribution.details.header");
    }

    /*
     * (non-Javadoc)
     * 
     * @see hawkbit.server.ui.common.detailslayout.AbstractTableDetailsLayout#
     * addTabs(com.vaadin. ui.TabSheet)
     */
    @Override
    protected void addTabs(final TabSheet detailsTab) {
        detailsTab.addTab(createDetailsLayout(), i18n.get("caption.tab.details"), null);
        detailsTab.addTab(createDescriptionLayout(), i18n.get("caption.tab.description"), null);
        detailsTab.addTab(createSoftwareModuleTab(), i18n.get("caption.softwares.distdetail.tab"), null);
        detailsTab.addTab(createTagsLayout(), i18n.get("caption.tags.tab"), null);
        detailsTab.addTab(createLogLayout(), i18n.get("caption.logs.tab"), null);
    }

    /*
     * (non-Javadoc)
     * 
     * @see hawkbit.server.ui.common.detailslayout.AbstractTableDetailsLayout#
     * clearDetails()
     */
    @Override
    protected void clearDetails() {
        populateDetailsWidget(null);
    }

    /*
     * (non-Javadoc)
     * 
     * @see hawkbit.server.ui.common.detailslayout.AbstractTableDetailsLayout#
     * hasEditSoftwareModulePermission()
     */
    @Override
    protected Boolean hasEditPermission() {
        return permissionChecker.hasUpdateDistributionPermission();
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final SoftwareModuleEvent event) {
        if (event.getSoftwareModuleEventType() == SoftwareModuleEventType.ASSIGN_SOFTWARE_MODULE) {
            ui.access(() -> updateSoftwareModule(event.getSoftwareModule()));
        }
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final DistributionTableEvent distributionTableEvent) {
        if (distributionTableEvent.getDistributionComponentEvent() == DistributionComponentEvent.ON_VALUE_CHANGE
                || distributionTableEvent
                        .getDistributionComponentEvent() == DistributionComponentEvent.EDIT_DISTRIBUTION) {
            assignedSWModule.clear();
            ui.access(() -> {
                /**
                 * distributionTableEvent.getDistributionSet() is null when
                 * table has no data.
                 */
                if (distributionTableEvent.getDistributionSet() != null) {
                    selectedDsModule = distributionTableEvent.getDistributionSet();
                    populateData(true);
                } else {
                    populateData(false);
                }
            });
        } else if (distributionTableEvent.getDistributionComponentEvent() == DistributionComponentEvent.MINIMIZED) {
            ui.access(() -> showLayout());
        } else if (distributionTableEvent.getDistributionComponentEvent() == DistributionComponentEvent.MAXIMIZED) {
            ui.access(() -> hideLayout());
        }
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final SoftwareModuleAssignmentDiscardEvent softwareModuleAssignmentDiscardEvent) {
        if (softwareModuleAssignmentDiscardEvent.getDistributionSetIdName() != null) {
            ui.access(() -> {
                final DistributionSetIdName distIdName = softwareModuleAssignmentDiscardEvent
                        .getDistributionSetIdName();
                if (distIdName.getId().equals(selectedDsModule.getId())
                        && distIdName.getName().equals(selectedDsModule.getName())) {
                    selectedDsModule = distributionSetManagement
                            .findDistributionSetByIdWithDetails(selectedDsModule.getId());
                    populteModule(selectedDsModule);
                }
            });
        }
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final SaveActionWindowEvent saveActionWindowEvent) {
        if ((saveActionWindowEvent == SaveActionWindowEvent.SAVED_ASSIGNMENTS
                || saveActionWindowEvent == SaveActionWindowEvent.DISCARD_ALL_ASSIGNMENTS)
                && selectedDsModule != null) {
            assignedSWModule.clear();
            selectedDsModule = distributionSetManagement.findDistributionSetByIdWithDetails(selectedDsModule.getId());
            ui.access(() -> populteModule(selectedDsModule));
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

    @PreDestroy
    void destroy() {
        /*
         * It's good to do this, even though vaadin-spring will automatically
         * unsubscribe .
         */
        eventBus.unsubscribe(this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see hawkbit.server.ui.common.detailslayout.AbstractTableDetailsLayout#
     * getTabSheetId()
     */
    @Override
    protected String getTabSheetId() {
        return null;
    }

    @Override
    protected String getDetailsHeaderCaptionId() {
        return SPUIComponetIdProvider.DISTRIBUTION_DETAILS_HEADER_LABEL_ID;
    }

}
