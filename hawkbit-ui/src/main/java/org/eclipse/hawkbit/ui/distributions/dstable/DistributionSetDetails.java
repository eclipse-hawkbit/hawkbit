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
        this.softwareModuleTable = new SoftwareModuleDetailsTable();
        this.softwareModuleTable.init(this.i18n, true, this.permissionChecker, this.distributionSetManagement,
                this.eventBus, this.manageDistUIState);
        super.init();
        this.ui = UI.getCurrent();
        this.eventBus.subscribe(this);
    }

    protected VerticalLayout createTagsLayout() {
        this.tagsLayout = getTabLayout();
        return this.tagsLayout;
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
        this.softwareModuleTable.populateModule(distributionSet);
        showUnsavedAssignment();
    }

    @SuppressWarnings("unchecked")
    private void showUnsavedAssignment() {
        Item item;
        final Map<DistributionSetIdName, HashSet<SoftwareModuleIdName>> assignedList = this.manageDistUIState
                .getAssignedList();
        final Long selectedDistId = this.manageDistUIState.getLastSelectedDistribution().isPresent()
                ? this.manageDistUIState.getLastSelectedDistribution().get().getId() : null;
        Set<SoftwareModuleIdName> softwareModuleIdNameList = null;

        for (final Map.Entry<DistributionSetIdName, HashSet<SoftwareModuleIdName>> entry : assignedList.entrySet()) {
            if (entry.getKey().getId().equals(selectedDistId)) {
                softwareModuleIdNameList = entry.getValue();
                break;
            }
        }

        if (null != softwareModuleIdNameList) {
            for (final SoftwareModuleIdName swIdName : softwareModuleIdNameList) {
                final SoftwareModule softwareModule = this.softwareManagement.findSoftwareModuleById(swIdName.getId());
                if (this.assignedSWModule.containsKey(softwareModule.getType().getName())) {
                    this.assignedSWModule.get(softwareModule.getType().getName()).append("</br>").append("<I>")
                            .append(getUnsavedAssigedSwModule(softwareModule.getName(), softwareModule.getVersion()))
                            .append("<I>");

                } else {
                    this.assignedSWModule.put(softwareModule.getType().getName(),
                            new StringBuilder().append("<I>").append(
                                    getUnsavedAssigedSwModule(softwareModule.getName(), softwareModule.getVersion()))
                            .append("<I>"));
                }

            }
            for (final Map.Entry<String, StringBuilder> entry : this.assignedSWModule.entrySet()) {
                item = this.softwareModuleTable.getContainerDataSource().getItem(entry.getKey());
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
        if (this.permissionChecker.hasUpdateDistributionPermission() && this.distributionSetManagement
                .findDistributionSetById(this.manageDistUIState.getLastSelectedDistribution().get().getId())
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

        this.softwareModuleTable.getContainerDataSource().getItemIds();
        if (this.assignedSWModule.containsKey(module.getType().getName())) {
            /*
             * If software module type is software, means multiple softwares can
             * assigned to that type. Hence if multipe softwares belongs to same
             * type is drroped, then add to the list.
             */

            if (module.getType().getMaxAssignments() == Integer.MAX_VALUE) {
                this.assignedSWModule.get(module.getType().getName()).append("</br>").append("<I>")
                        .append(getUnsavedAssigedSwModule(module.getName(), module.getVersion())).append("</I>");
            }

            /*
             * If software mocule type is firmware, means single software can be
             * assigned to that type. Hence if multiple softwares belongs to
             * same type is dropped, then override with previous one.
             */
            if (module.getType().getMaxAssignments() == 1) {
                this.assignedSWModule.put(module.getType().getName(), new StringBuilder().append("<I>")
                        .append(getUnsavedAssigedSwModule(module.getName(), module.getVersion())).append("</I>"));
            }

        } else {
            this.assignedSWModule.put(module.getType().getName(), new StringBuilder().append("<I>")
                    .append(getUnsavedAssigedSwModule(module.getName(), module.getVersion())).append("</I>"));
        }

        for (final Map.Entry<String, StringBuilder> entry : this.assignedSWModule.entrySet()) {
            final Item item = this.softwareModuleTable.getContainerDataSource().getItem(entry.getKey());
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
        softwareLayout.addComponent(this.softwareModuleTable);
        return softwareLayout;
    }

    private void populateTags(final DistributionSet ds) {
        this.tagsLayout.removeAllComponents();
        if (null != ds) {
            this.tagsLayout.addComponent(this.distributionTagToken.getTokenField());
        }
    }

    private void populateLog(final DistributionSet ds) {
        if (null != ds) {
            updateLogLayout(getLogLayout(), ds.getLastModifiedAt(), ds.getLastModifiedBy(), ds.getCreatedAt(),
                    ds.getCreatedBy(), this.i18n);
        } else {
            updateLogLayout(getLogLayout(), null, HawkbitCommonUtil.SP_STRING_EMPTY, null, null, this.i18n);
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
            updateDescriptionLayout(this.i18n.get("label.description"), ds.getDescription());
        } else {
            updateDescriptionLayout(this.i18n.get("label.description"), null);
        }
    }

    private void updateDistributionSetDetailsLayout(final String type, final Boolean isMigrationRequired) {
        final VerticalLayout detailsTabLayout = getDetailsLayout();
        detailsTabLayout.removeAllComponents();

        if (type != null) {
            final Label typeLabel = SPUIComponentProvider.createNameValueLabel(this.i18n.get("label.dist.details.type"),
                    type);
            typeLabel.setId(SPUIComponetIdProvider.DETAILS_TYPE_LABEL_ID);
            detailsTabLayout.addComponent(typeLabel);
        }

        if (isMigrationRequired != null) {
            detailsTabLayout.addComponent(SPUIComponentProvider.createNameValueLabel(
                    this.i18n.get("checkbox.dist.migration.required"),
                    isMigrationRequired.equals(Boolean.TRUE) ? this.i18n.get("label.yes") : this.i18n.get("label.no")));
        }
    }

    public Long getDsId() {
        return this.dsId;
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
        final Window newDistWindow = this.distributionAddUpdateWindowLayout.getWindow();
        this.distributionAddUpdateWindowLayout.populateValuesOfDistribution(getDsId());
        newDistWindow.setCaption(this.i18n.get("caption.update.dist"));
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
        return this.manageDistUIState.getSelectedDistributions().isPresent()
                && !this.manageDistUIState.getSelectedDistributions().get().isEmpty();
    }

    /*
     * (non-Javadoc)
     *
     * @see hawkbit.server.ui.common.detailslayout.AbstractTableDetailsLayout#
     * onLoadIsTableMaximized ()
     */
    @Override
    protected Boolean onLoadIsTableMaximized() {
        return this.manageDistUIState.isDsTableMaximized();
    }

    /*
     * (non-Javadoc)
     *
     * @see hawkbit.server.ui.common.detailslayout.AbstractTableDetailsLayout#
     * populateDetailsWidget()
     */
    @Override
    protected void populateDetailsWidget() {
        populateDetailsWidget(this.selectedDsModule);
    }

    /*
     * (non-Javadoc)
     *
     * @see hawkbit.server.ui.common.detailslayout.AbstractTableDetailsLayout#
     * getDefaultCaption()
     */
    @Override
    protected String getDefaultCaption() {
        return this.i18n.get("distribution.details.header");
    }

    /*
     * (non-Javadoc)
     *
     * @see hawkbit.server.ui.common.detailslayout.AbstractTableDetailsLayout#
     * addTabs(com.vaadin. ui.TabSheet)
     */
    @Override
    protected void addTabs(final TabSheet detailsTab) {
        detailsTab.addTab(createDetailsLayout(), this.i18n.get("caption.tab.details"), null);
        detailsTab.addTab(createDescriptionLayout(), this.i18n.get("caption.tab.description"), null);
        detailsTab.addTab(createSoftwareModuleTab(), this.i18n.get("caption.softwares.distdetail.tab"), null);
        detailsTab.addTab(createTagsLayout(), this.i18n.get("caption.tags.tab"), null);
        detailsTab.addTab(createLogLayout(), this.i18n.get("caption.logs.tab"), null);
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
        return this.permissionChecker.hasUpdateDistributionPermission();
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final SoftwareModuleEvent event) {
        if (event.getSoftwareModuleEventType() == SoftwareModuleEventType.ASSIGN_SOFTWARE_MODULE) {
            this.ui.access(() -> updateSoftwareModule(event.getSoftwareModule()));
        }
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final DistributionTableEvent distributionTableEvent) {
        if (distributionTableEvent.getDistributionComponentEvent() == DistributionComponentEvent.ON_VALUE_CHANGE
                || distributionTableEvent
                        .getDistributionComponentEvent() == DistributionComponentEvent.EDIT_DISTRIBUTION) {
            this.assignedSWModule.clear();
            this.ui.access(() -> {
                /**
                 * distributionTableEvent.getDistributionSet() is null when
                 * table has no data.
                 */
                if (distributionTableEvent.getDistributionSet() != null) {
                    this.selectedDsModule = distributionTableEvent.getDistributionSet();
                    populateData(true);
                } else {
                    populateData(false);
                }
            });
        } else if (distributionTableEvent.getDistributionComponentEvent() == DistributionComponentEvent.MINIMIZED) {
            this.ui.access(() -> showLayout());
        } else if (distributionTableEvent.getDistributionComponentEvent() == DistributionComponentEvent.MAXIMIZED) {
            this.ui.access(() -> hideLayout());
        }
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final SoftwareModuleAssignmentDiscardEvent softwareModuleAssignmentDiscardEvent) {
        if (softwareModuleAssignmentDiscardEvent.getDistributionSetIdName() != null) {
            this.ui.access(() -> {
                final DistributionSetIdName distIdName = softwareModuleAssignmentDiscardEvent
                        .getDistributionSetIdName();
                if (distIdName.getId().equals(this.selectedDsModule.getId())
                        && distIdName.getName().equals(this.selectedDsModule.getName())) {
                    this.selectedDsModule = this.distributionSetManagement
                            .findDistributionSetByIdWithDetails(this.selectedDsModule.getId());
                    populteModule(this.selectedDsModule);
                }
            });
        }
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final SaveActionWindowEvent saveActionWindowEvent) {
        if ((saveActionWindowEvent == SaveActionWindowEvent.SAVED_ASSIGNMENTS
                || saveActionWindowEvent == SaveActionWindowEvent.DISCARD_ALL_ASSIGNMENTS)
                && this.selectedDsModule != null) {
            this.assignedSWModule.clear();
            this.selectedDsModule = this.distributionSetManagement
                    .findDistributionSetByIdWithDetails(this.selectedDsModule.getId());
            this.ui.access(() -> populteModule(this.selectedDsModule));
        }
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEventDiscard(final SaveActionWindowEvent saveActionWindowEvent) {
        if (saveActionWindowEvent == SaveActionWindowEvent.DISCARD_ASSIGNMENT
                || saveActionWindowEvent == SaveActionWindowEvent.DISCARD_ALL_ASSIGNMENTS
                || saveActionWindowEvent == SaveActionWindowEvent.DELETE_ALL_SOFWARE) {
            this.assignedSWModule.clear();
            showUnsavedAssignment();
        }
    }

    @PreDestroy
    void destroy() {
        /*
         * It's good to do this, even though vaadin-spring will automatically
         * unsubscribe .
         */
        this.eventBus.unsubscribe(this);
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
