/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.detailslayout;

import java.util.Set;

import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.exception.EntityReadOnlyException;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.common.table.BaseEntityEventType;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleSmallNoBorder;
import org.eclipse.hawkbit.ui.distributions.event.DistributionsUIEvent;
import org.eclipse.hawkbit.ui.distributions.state.ManageDistUIState;
import org.eclipse.hawkbit.ui.management.event.DistributionTableEvent;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import org.vaadin.spring.events.EventBus;

import com.vaadin.data.Item;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Table;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Software module details table.
 * 
 */
public class SoftwareModuleDetailsTable extends Table {

    private static final long serialVersionUID = 2913758200611837718L;

    private static final Logger LOG = LoggerFactory.getLogger(SoftwareModuleDetailsTable.class);

    private static final String SOFT_TYPE_NAME = "name";

    private static final String SOFT_MODULE = "softwareModule";

    private static final String SOFT_TYPE_MANDATORY = "mandatory";

    private boolean isTargetAssigned;

    private final boolean isUnassignSoftModAllowed;
    private final SpPermissionChecker permissionChecker;

    private final transient DistributionSetManagement distributionSetManagement;

    private final VaadinMessageSource i18n;

    private final transient EventBus.UIEventBus eventBus;

    private final ManageDistUIState manageDistUIState;

    private final UINotification uiNotification;

    /**
     * Initialize software module table- to be displayed in details layout.
     * 
     * @param i18n
     *            I18N
     * @param isUnassignSoftModAllowed
     *            boolean flag to check for unassign functionality allowed for
     *            the view.
     * @param distributionSetManagement
     *            DistributionSetManagement
     * @param permissionChecker
     *            SpPermissionChecker
     * @param eventBus
     *            SessionEventBus
     * @param manageDistUIState
     *            ManageDistUIState
     * @param uiNotification
     *            UINotification for displaying error and success notifications
     */
    public SoftwareModuleDetailsTable(final VaadinMessageSource i18n, final boolean isUnassignSoftModAllowed,
            final SpPermissionChecker permissionChecker, final DistributionSetManagement distributionSetManagement,
            final EventBus.UIEventBus eventBus, final ManageDistUIState manageDistUIState,
            final UINotification uiNotification) {
        this.i18n = i18n;
        this.isUnassignSoftModAllowed = isUnassignSoftModAllowed;
        this.permissionChecker = permissionChecker;
        this.distributionSetManagement = distributionSetManagement;
        this.manageDistUIState = manageDistUIState;
        this.eventBus = eventBus;
        this.uiNotification = uiNotification;
        createSwModuleTable();
    }

    private void createSwModuleTable() {
        addStyleName(ValoTheme.TABLE_NO_HORIZONTAL_LINES);
        setSelectable(false);
        setImmediate(true);
        setContainerDataSource(getSwModuleContainer());
        setColumnHeaderMode(ColumnHeaderMode.EXPLICIT);
        addSWModuleTableHeader();
        setSizeFull();
        addStyleName(SPUIStyleDefinitions.SW_MODULE_TABLE);
    }

    private IndexedContainer getSwModuleContainer() {
        final IndexedContainer container = new IndexedContainer();
        container.addContainerProperty(SOFT_TYPE_MANDATORY, Label.class, "");
        container.addContainerProperty(SOFT_TYPE_NAME, Label.class, "");
        container.addContainerProperty(SOFT_MODULE, VerticalLayout.class, "");
        setColumnExpandRatio(SOFT_TYPE_MANDATORY, 0.1F);
        setColumnExpandRatio(SOFT_TYPE_NAME, 0.4F);
        setColumnExpandRatio(SOFT_MODULE, 0.3F);
        setColumnAlignment(SOFT_TYPE_MANDATORY, Align.RIGHT);
        setColumnAlignment(SOFT_TYPE_NAME, Align.LEFT);
        setColumnAlignment(SOFT_MODULE, Align.LEFT);
        return container;
    }

    private void addSWModuleTableHeader() {
        setColumnHeader(SOFT_TYPE_MANDATORY, "");
        setColumnHeader(SOFT_TYPE_NAME, i18n.getMessage("header.caption.typename"));
        setColumnHeader(SOFT_MODULE, i18n.getMessage("header.caption.softwaremodule"));
    }

    /**
     * Populate software module table.
     * 
     * @param distributionSet
     */
    public void populateModule(final DistributionSet distributionSet) {
        removeAllItems();
        if (distributionSet != null) {
            if (isUnassignSoftModAllowed && permissionChecker.hasUpdateRepositoryPermission()) {
                try {
                    isTargetAssigned = false;
                } catch (final EntityReadOnlyException exception) {
                    isTargetAssigned = true;
                    LOG.info("Target already assigned for the distribution set: " + distributionSet.getName(),
                            exception);
                }
            }
            final Set<SoftwareModuleType> swModuleMandatoryTypes = distributionSet.getType().getMandatoryModuleTypes();
            final Set<SoftwareModuleType> swModuleOptionalTypes = distributionSet.getType().getOptionalModuleTypes();

            if (!CollectionUtils.isEmpty(swModuleMandatoryTypes)) {
                swModuleMandatoryTypes.forEach(swModule -> setSwModuleProperties(swModule, true, distributionSet));
            }
            if (!CollectionUtils.isEmpty(swModuleOptionalTypes)) {
                swModuleOptionalTypes.forEach(swModule -> setSwModuleProperties(swModule, false, distributionSet));
            }
            setAmountOfTableRows(getContainerDataSource().size());
        }

    }

    private void setAmountOfTableRows(final int amountOfRows) {
        setPageLength(amountOfRows);
    }

    private void setSwModuleProperties(final SoftwareModuleType swModType, final Boolean isMandatory,
            final DistributionSet distributionSet) {
        final Set<SoftwareModule> alreadyAssignedSwModules = distributionSet.getModules();
        final Item saveTblitem = getContainerDataSource().addItem(swModType.getName());
        final Label mandatoryLabel = createMandatoryLabel(isMandatory);
        final Label typeName = HawkbitCommonUtil.getFormatedLabel(swModType.getName());
        final VerticalLayout verticalLayout = createSoftModuleLayout(swModType, distributionSet,
                alreadyAssignedSwModules);

        saveTblitem.getItemProperty(SOFT_TYPE_MANDATORY).setValue(mandatoryLabel);
        saveTblitem.getItemProperty(SOFT_TYPE_NAME).setValue(typeName);
        saveTblitem.getItemProperty(SOFT_MODULE).setValue(verticalLayout);
    }

    private void unassignSW(final ClickEvent event, final DistributionSet distributionSet,
            final Set<SoftwareModule> alreadyAssignedSwModules) {
        final SoftwareModule unAssignedSw = getSoftwareModule(event.getButton().getId(), alreadyAssignedSwModules);
        if (distributionSetManagement.isInUse(distributionSet.getId())) {
            uiNotification.displayValidationError(i18n.getMessage("message.error.notification.ds.target.assigned",
                    distributionSet.getName(), distributionSet.getVersion()));
        } else {
            final DistributionSet newDistributionSet = distributionSetManagement
                    .unassignSoftwareModule(distributionSet.getId(), unAssignedSw.getId());
            manageDistUIState.setLastSelectedEntityId(newDistributionSet.getId());
            eventBus.publish(this, new DistributionTableEvent(BaseEntityEventType.SELECTED_ENTITY, newDistributionSet));
            eventBus.publish(this, DistributionsUIEvent.ORDER_BY_DISTRIBUTION);
            uiNotification.displaySuccess(i18n.getMessage("message.sw.unassigned", unAssignedSw.getName()));
        }
    }

    private static boolean isSoftModAvaiableForSoftType(final Set<SoftwareModule> swModulesSet,
            final SoftwareModuleType swModType) {
        for (final SoftwareModule sw : swModulesSet) {
            if (swModType.getName().equals(sw.getType().getName())) {
                return true;
            }
        }

        return false;

    }

    private VerticalLayout createSoftModuleLayout(final SoftwareModuleType swModType,
            final DistributionSet distributionSet, final Set<SoftwareModule> alreadyAssignedSwModules) {
        final VerticalLayout verticalLayout = new VerticalLayout();
        for (final SoftwareModule sw : alreadyAssignedSwModules) {
            if (swModType.getKey().equals(sw.getType().getKey())) {
                final HorizontalLayout horizontalLayout = new HorizontalLayout();
                horizontalLayout.setSizeFull();
                final Label softwareModule = HawkbitCommonUtil.getFormatedLabel("");
                final Button reassignSoftModule = SPUIComponentProvider.getButton(sw.getName(), "", "", "", true,
                        FontAwesome.TIMES, SPUIButtonStyleSmallNoBorder.class);
                reassignSoftModule
                        .addClickListener(event -> unassignSW(event, distributionSet, alreadyAssignedSwModules));
                final String softwareModNameVersion = HawkbitCommonUtil.getFormattedNameVersion(sw.getName(),
                        sw.getVersion());
                softwareModule.setValue(softwareModNameVersion);
                softwareModule.setDescription(softwareModNameVersion);
                softwareModule.setId(sw.getName() + "-label");
                horizontalLayout.addComponent(softwareModule);
                horizontalLayout.setExpandRatio(softwareModule, 1F);
                if (isUnassignSoftModAllowed && permissionChecker.hasUpdateRepositoryPermission() && !isTargetAssigned
                        && (isSoftModAvaiableForSoftType(alreadyAssignedSwModules, swModType))) {
                    horizontalLayout.addComponent(reassignSoftModule);
                }
                verticalLayout.addComponent(horizontalLayout);
            }

        }

        return verticalLayout;
    }

    /**
     * @param value
     * @param alreadyAssignedSwModules
     * @return
     */
    protected SoftwareModule getSoftwareModule(final String softwareModule,
            final Set<SoftwareModule> alreadyAssignedSwModules) {
        for (final SoftwareModule sw : alreadyAssignedSwModules) {
            if (softwareModule.equals(sw.getName())) {
                return sw;
            }
        }
        return null;
    }

    private static Label createMandatoryLabel(final boolean mandatory) {
        final Label mandatoryLable = mandatory ? HawkbitCommonUtil.getFormatedLabel(" * ")
                : HawkbitCommonUtil.getFormatedLabel("  ");
        if (mandatory) {
            mandatoryLable.setStyleName(SPUIStyleDefinitions.SP_TEXTFIELD_ERROR);
        }
        return mandatoryLable;
    }
}
