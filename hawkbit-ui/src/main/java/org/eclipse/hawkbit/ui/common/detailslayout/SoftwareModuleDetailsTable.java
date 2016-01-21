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
import org.eclipse.hawkbit.repository.SpPermissionChecker;
import org.eclipse.hawkbit.repository.exception.EntityLockedException;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleSmallNoBorder;
import org.eclipse.hawkbit.ui.distributions.event.DistributionsUIEvent;
import org.eclipse.hawkbit.ui.distributions.state.ManageDistUIState;
import org.eclipse.hawkbit.ui.management.event.DistributionTableEvent;
import org.eclipse.hawkbit.ui.management.event.DistributionTableEvent.DistributionComponentEvent;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.SpringContextHelper;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.spring.events.EventBus.SessionEventBus;

import com.vaadin.data.Item;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Label;
import com.vaadin.ui.Table;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Software module details table.
 * 
 *
 *
 *
 */
public class SoftwareModuleDetailsTable extends Table {

    private static final long serialVersionUID = 2913758200611837718L;

    private static final Logger LOG = LoggerFactory.getLogger(SoftwareModuleDetailsTable.class);

    private static final String SOFT_TYPE_NAME = "name";

    private static final String SOFT_MODULE = "softwareModule";

    private static final String SOFT_TYPE_MANDATORY = "mandatory";

    private static final String UNASSIGN_SOFT_MODULE = "unassignSoftModule";

    private boolean isTargetAssigned;

    private boolean isUnassignSoftModAllowed;
    private SpPermissionChecker permissionChecker;

    private transient DistributionSetManagement distributionSetManagement;

    private I18N i18n;

    private transient SessionEventBus eventBus;

    private transient ManageDistUIState manageDistUIState;

    private transient UINotification uiNotification;

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
     */
    public void init(final I18N i18n, final boolean isUnassignSoftModAllowed,
            final SpPermissionChecker permissionChecker, final DistributionSetManagement distributionSetManagement,
            final SessionEventBus eventBus, final ManageDistUIState manageDistUIState) {
        this.i18n = i18n;
        this.isUnassignSoftModAllowed = isUnassignSoftModAllowed;
        this.permissionChecker = permissionChecker;
        this.distributionSetManagement = distributionSetManagement;
        this.manageDistUIState = manageDistUIState;
        this.eventBus = eventBus;
        this.uiNotification = SpringContextHelper.getBean(UINotification.class);
        createSwModuleTable();
    }

    private void createSwModuleTable() {
        addStyleName(ValoTheme.TABLE_NO_HORIZONTAL_LINES);
        addStyleName(ValoTheme.TABLE_NO_STRIPES);
        setSelectable(false);
        setImmediate(true);
        setContainerDataSource(getSwModuleContainer());
        setColumnHeaderMode(ColumnHeaderMode.EXPLICIT);
        addSWModuleTableHeader();
        setSizeFull(); // check if this style is required
        addStyleName(SPUIStyleDefinitions.SW_MODULE_TABLE);
    }

    private IndexedContainer getSwModuleContainer() {
        final IndexedContainer container = new IndexedContainer();
        container.addContainerProperty(SOFT_TYPE_MANDATORY, Label.class, "");
        container.addContainerProperty(SOFT_TYPE_NAME, Label.class, "");
        container.addContainerProperty(SOFT_MODULE, Label.class, "");
        if (isUnassignSoftModAllowed && permissionChecker.hasUpdateDistributionPermission()) {
            container.addContainerProperty(UNASSIGN_SOFT_MODULE, Button.class, "");
        }
        setColumnExpandRatio(SOFT_TYPE_MANDATORY, 0.1f);
        setColumnExpandRatio(SOFT_TYPE_NAME, 0.4f);
        setColumnExpandRatio(SOFT_MODULE, 0.3f);
        if (isUnassignSoftModAllowed && permissionChecker.hasUpdateDistributionPermission()) {
            setColumnExpandRatio(UNASSIGN_SOFT_MODULE, 0.2F);
        }
        setColumnAlignment(SOFT_TYPE_MANDATORY, Align.RIGHT);
        setColumnAlignment(SOFT_TYPE_NAME, Align.LEFT);
        setColumnAlignment(SOFT_MODULE, Align.LEFT);
        if (isUnassignSoftModAllowed && permissionChecker.hasUpdateDistributionPermission()) {
            setColumnAlignment(UNASSIGN_SOFT_MODULE, Align.RIGHT);
        }
        return container;
    }

    private void addSWModuleTableHeader() {
        setColumnHeader(SOFT_TYPE_MANDATORY, "");
        setColumnHeader(SOFT_TYPE_NAME, i18n.get("header.caption.typename"));
        setColumnHeader(SOFT_MODULE, i18n.get("header.caption.softwaremodule"));
        if (isUnassignSoftModAllowed && permissionChecker.hasUpdateDistributionPermission()) {
            setColumnHeader(UNASSIGN_SOFT_MODULE, i18n.get("header.caption.unassign"));
        }

    }

    /**
     * Populate software module table.
     * 
     * @param distributionSet
     */
    public void populateModule(final DistributionSet distributionSet) {
        removeAllItems();
        if (null != distributionSet) {
            if (isUnassignSoftModAllowed && permissionChecker.hasUpdateDistributionPermission()) {
                try {
                    distributionSetManagement.checkDistributionSetAlreadyUse(distributionSet);
                    isTargetAssigned = false;
                } catch (final EntityLockedException exception) {
                    isTargetAssigned = true;
                    LOG.info("Target already assigned for the distribution set: " + distributionSet.getName(),
                            exception);
                }
            }
            final Set<SoftwareModuleType> swModuleMandatoryTypes = distributionSet.getType().getMandatoryModuleTypes();
            final Set<SoftwareModuleType> swModuleOptionalTypes = distributionSet.getType().getOptionalModuleTypes();

            if (null != swModuleMandatoryTypes && !swModuleMandatoryTypes.isEmpty()) {
                swModuleMandatoryTypes.forEach(swModule -> setSwModuleProperties(swModule, true, distributionSet));
            }
            if (null != swModuleOptionalTypes && !swModuleOptionalTypes.isEmpty()) {
                swModuleOptionalTypes.forEach(swModule -> setSwModuleProperties(swModule, false, distributionSet));
            }
        }

    }

    private void setSwModuleProperties(final SoftwareModuleType swModType, final Boolean isMandatory,
            final DistributionSet distributionSet) {
        final Set<SoftwareModule> alreadyAssignedSwModules = distributionSet.getModules();
        final Item saveTblitem = getContainerDataSource().addItem(swModType.getName());
        final Label mandatoryLabel = createMandatoryLabel(isMandatory);
        final Label typeName = HawkbitCommonUtil.getFormatedLabel(swModType.getName());

        final Label softwareModule = HawkbitCommonUtil.getFormatedLabel(HawkbitCommonUtil.SP_STRING_EMPTY);
        final Button reassignSoftModule = SPUIComponentProvider.getButton(swModType.getName(), "", "", "", true,
                FontAwesome.TIMES, SPUIButtonStyleSmallNoBorder.class);

        reassignSoftModule.addClickListener(event -> unassignSW(event, distributionSet, alreadyAssignedSwModules));
        if (null != alreadyAssignedSwModules && !alreadyAssignedSwModules.isEmpty()) {
            final String swModuleName = getSwModuleName(alreadyAssignedSwModules, swModType);
            softwareModule.setValue(swModuleName);
            softwareModule.setDescription(swModuleName);
        }
        saveTblitem.getItemProperty(SOFT_TYPE_MANDATORY).setValue(mandatoryLabel);
        saveTblitem.getItemProperty(SOFT_TYPE_NAME).setValue(typeName);
        saveTblitem.getItemProperty(SOFT_MODULE).setValue(softwareModule);
        if (isUnassignSoftModAllowed && permissionChecker.hasUpdateDistributionPermission() && !isTargetAssigned
                && (isSoftModAvaiableForSoftType(alreadyAssignedSwModules, swModType))) {
            saveTblitem.getItemProperty(UNASSIGN_SOFT_MODULE).setValue(reassignSoftModule);
        }

    }

    private void unassignSW(final ClickEvent event, final DistributionSet distributionSet,
            final Set<SoftwareModule> alreadyAssignedSwModules) {
        final SoftwareModule unAssignedSw = getSoftwareModule(
                (Label) getContainerDataSource().getItem(event.getButton().getId()).getItemProperty(SOFT_MODULE)
                        .getValue(), alreadyAssignedSwModules);
        final DistributionSet newDistributionSet = distributionSetManagement.unassignSoftwareModule(distributionSet,
                unAssignedSw);
        manageDistUIState.setLastSelectedDistribution(newDistributionSet.getDistributionSetIdName());
        eventBus.publish(this, new DistributionTableEvent(DistributionComponentEvent.ON_VALUE_CHANGE,
                newDistributionSet));
        eventBus.publish(this, DistributionsUIEvent.ORDER_BY_DISTRIBUTION);
        uiNotification.displaySuccess(i18n.get("message.sw.unassigned", unAssignedSw.getName()));
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

    /**
     * @param value
     * @param alreadyAssignedSwModules
     * @return
     */
    protected SoftwareModule getSoftwareModule(final Label softwareModule,
            final Set<SoftwareModule> alreadyAssignedSwModules) {
        for (final SoftwareModule sw : alreadyAssignedSwModules) {
            if (softwareModule.getValue().contains(sw.getName())) {
                return sw;
            }
        }
        return null;
    }

    private Label createMandatoryLabel(final boolean mandatory) {
        final Label mandatoryLable = mandatory ? HawkbitCommonUtil.getFormatedLabel(" * ") : HawkbitCommonUtil
                .getFormatedLabel("  ");
        if (mandatory) {
            mandatoryLable.setStyleName(SPUIStyleDefinitions.SP_TEXTFIELD_ERROR);
        }
        return mandatoryLable;
    }

    private String getSwModuleName(final Set<SoftwareModule> swModulesSet, final SoftwareModuleType swModType) {
        final StringBuilder assignedSWModules = new StringBuilder();
        for (final SoftwareModule sw : swModulesSet) {
            if (swModType.getKey().equals(sw.getType().getKey())) {
                assignedSWModules.append(HawkbitCommonUtil.getFormattedNameVersion(sw.getName(), sw.getVersion()))
                        .append("</br>");
            }
        }
        return assignedSWModules.toString();
    }
}
