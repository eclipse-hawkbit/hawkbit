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

import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;

import com.vaadin.data.Item;
import com.vaadin.data.util.IndexedContainer;
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

    private static final String SOFT_TYPE_NAME = "name";

    private static final String SOFT_MODULE = "softwareModule";

    private static final String SOFT_TYPE_MANDATORY = "mandatory";

    private I18N i18n;

    /**
     * Initialize software module table- to be displayed in details layout.
     * 
     * @param i18n
     */
    public void init(final I18N i18n) {
        this.i18n = i18n;
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
        setColumnExpandRatio(SOFT_TYPE_MANDATORY, 0.1f);
        setColumnExpandRatio(SOFT_TYPE_NAME, 0.5f);
        setColumnExpandRatio(SOFT_MODULE, 0.4f);
        setColumnAlignment(SOFT_TYPE_MANDATORY, Align.RIGHT);
        setColumnAlignment(SOFT_TYPE_NAME, Align.LEFT);
        setColumnAlignment(SOFT_MODULE, Align.LEFT);

        return container;
    }

    private void addSWModuleTableHeader() {
        setColumnHeader(SOFT_TYPE_MANDATORY, "");
        setColumnHeader(SOFT_TYPE_NAME, i18n.get("header.caption.typename"));
        setColumnHeader(SOFT_MODULE, i18n.get("header.caption.softwaremodule"));
    }

    /**
     * Populate software module table.
     * 
     * @param distributionSet
     */
    public void populateModule(final DistributionSet distributionSet) {
        removeAllItems();
        if (null != distributionSet) {
            final Set<SoftwareModuleType> swModuleMandatoryTypes = distributionSet.getType().getMandatoryModuleTypes();
            final Set<SoftwareModuleType> swModuleOptionalTypes = distributionSet.getType().getOptionalModuleTypes();

            if (null != swModuleMandatoryTypes && !swModuleMandatoryTypes.isEmpty()) {
                swModuleMandatoryTypes
                        .forEach(swModule -> setSwModuleProperties(swModule, true, distributionSet.getModules()));
            }
            if (null != swModuleOptionalTypes && !swModuleOptionalTypes.isEmpty()) {
                swModuleOptionalTypes
                        .forEach(swModule -> setSwModuleProperties(swModule, false, distributionSet.getModules()));
            }
        }

    }

    private void setSwModuleProperties(final SoftwareModuleType swModType, final Boolean isMandatory,
            final Set<SoftwareModule> alreadyAssignedSwModules) {
        final Item saveTblitem = getContainerDataSource().addItem(swModType.getName());
        final Label mandatoryLabel = createMandatoryLabel(isMandatory);
        final Label typeName = HawkbitCommonUtil.getFormatedLabel(swModType.getName());

        final Label softwareModule = HawkbitCommonUtil.getFormatedLabel(HawkbitCommonUtil.SP_STRING_EMPTY);

        if (null != alreadyAssignedSwModules && !alreadyAssignedSwModules.isEmpty()) {
            final String swModuleName = getSwModuleName(alreadyAssignedSwModules, swModType);
            softwareModule.setValue(swModuleName);
            softwareModule.setDescription(swModuleName);
        }
        saveTblitem.getItemProperty(SOFT_TYPE_MANDATORY).setValue(mandatoryLabel);
        saveTblitem.getItemProperty(SOFT_TYPE_NAME).setValue(typeName);
        saveTblitem.getItemProperty(SOFT_MODULE).setValue(softwareModule);
    }

    private Label createMandatoryLabel(final boolean mandatory) {
        final Label mandatoryLable = mandatory ? HawkbitCommonUtil.getFormatedLabel(" * ")
                : HawkbitCommonUtil.getFormatedLabel("  ");
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
