/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.distributions.disttype;

import java.util.Set;

import org.eclipse.hawkbit.repository.SoftwareModuleTypeManagement;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleNoBorder;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UIMessageIdProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.springframework.data.domain.PageRequest;
import org.springframework.util.StringUtils;

import com.vaadin.data.Item;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.AbstractSelect.ItemDescriptionGenerator;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Table;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Layout for the software modules select tables for managing Distribution Set
 * Types on the Distributions View.
 */
public class DistributionSetTypeSoftwareModuleSelectLayout extends VerticalLayout {

    private static final long serialVersionUID = 1L;

    private static final String DIST_TYPE_DESCRIPTION = "description";

    private static final String DIST_TYPE_MANDATORY = "mandatory";

    private static final String STAR = " * ";

    private static final String DIST_TYPE_NAME = "name";

    private final transient SoftwareModuleTypeManagement softwareModuleTypeManagement;

    private Table selectedTable;

    private Table sourceTable;

    private IndexedContainer selectedTableContainer;

    private IndexedContainer sourceTableContainer;

    private HorizontalLayout distTypeSelectLayout;

    private final VaadinMessageSource i18n;

    /**
     * Constructor
     * 
     * @param i18n
     *            VaadinMessageSource
     * @param softwareModuleTypeManagement
     *            SoftwareModuleTypeManagement
     */
    public DistributionSetTypeSoftwareModuleSelectLayout(final VaadinMessageSource i18n,
            final SoftwareModuleTypeManagement softwareModuleTypeManagement) {
        this.softwareModuleTypeManagement = softwareModuleTypeManagement;
        this.i18n = i18n;
        init();
    }

    protected void init() {
        distTypeSelectLayout = createTwinColumnLayout();
        setSizeFull();
        addComponent(distTypeSelectLayout);
    }

    private HorizontalLayout createTwinColumnLayout() {
        final HorizontalLayout twinColumnLayout = new HorizontalLayout();
        twinColumnLayout.setSizeFull();
        twinColumnLayout.setWidth("400px");

        buildSourceTable();
        buildSelectedTable();

        final VerticalLayout selectButtonLayout = new VerticalLayout();
        final Button selectButton = SPUIComponentProvider.getButton(UIComponentIdProvider.SELECT_DIST_TYPE, "", "",
                "arrow-button", true, FontAwesome.FORWARD, SPUIButtonStyleNoBorder.class);
        selectButton.addClickListener(event -> addSMType());
        final Button unSelectButton = SPUIComponentProvider.getButton("unselect-dist-type", "", "", "arrow-button",
                true, FontAwesome.BACKWARD, SPUIButtonStyleNoBorder.class);
        unSelectButton.addClickListener(event -> removeSMType());
        selectButtonLayout.addComponent(selectButton);
        selectButtonLayout.addComponent(unSelectButton);
        selectButtonLayout.setComponentAlignment(selectButton, Alignment.MIDDLE_CENTER);
        selectButtonLayout.setComponentAlignment(unSelectButton, Alignment.MIDDLE_CENTER);

        twinColumnLayout.addComponent(sourceTable);
        twinColumnLayout.addComponent(selectButtonLayout);
        twinColumnLayout.addComponent(selectedTable);
        twinColumnLayout.setComponentAlignment(sourceTable, Alignment.MIDDLE_LEFT);
        twinColumnLayout.setComponentAlignment(selectButtonLayout, Alignment.MIDDLE_CENTER);
        twinColumnLayout.setComponentAlignment(selectedTable, Alignment.MIDDLE_RIGHT);
        twinColumnLayout.setExpandRatio(sourceTable, 0.45F);
        twinColumnLayout.setExpandRatio(selectButtonLayout, 0.07F);
        twinColumnLayout.setExpandRatio(selectedTable, 0.48F);
        sourceTable.setVisibleColumns(DIST_TYPE_NAME);
        return twinColumnLayout;
    }

    private void buildSelectedTable() {
        selectedTable = new Table();
        selectedTable.setId(SPUIDefinitions.TWIN_TABLE_SELECTED_ID);
        selectedTable.setSelectable(true);
        selectedTable.setMultiSelect(true);
        selectedTable.setSortEnabled(false);
        selectedTable.addStyleName(ValoTheme.TABLE_NO_HORIZONTAL_LINES);
        selectedTable.addStyleName(ValoTheme.TABLE_NO_STRIPES);
        selectedTable.addStyleName(ValoTheme.TABLE_NO_VERTICAL_LINES);
        selectedTable.addStyleName(ValoTheme.TABLE_SMALL);
        selectedTable.addStyleName("dist_type_twin-table");
        selectedTable.setSizeFull();
        createSelectedTableContainer();
        selectedTable.setContainerDataSource(selectedTableContainer);
        addTooltTipToSelectedTable();
        selectedTable.setImmediate(true);
        selectedTable.setVisibleColumns(DIST_TYPE_NAME, DIST_TYPE_MANDATORY);
        selectedTable.setColumnHeaders(i18n.getMessage("header.dist.twintable.selected"), STAR);
        selectedTable.setColumnExpandRatio(DIST_TYPE_NAME, 0.75F);
        selectedTable.setColumnExpandRatio(DIST_TYPE_MANDATORY, 0.25F);
        selectedTable.setRequired(true);
    }

    private void addTooltTipToSelectedTable() {
        selectedTable.setItemDescriptionGenerator(new ItemDescriptionGenerator() {
            private static final long serialVersionUID = 1L;

            @Override
            public String generateDescription(final Component source, final Object itemId, final Object propertyId) {
                final Item item = selectedTable.getItem(itemId);
                final String description = (String) (item.getItemProperty(DIST_TYPE_DESCRIPTION).getValue());
                if (DIST_TYPE_NAME.equals(propertyId) && !StringUtils.isEmpty(description)) {
                    return i18n.getMessage("label.description") + description;
                } else if (DIST_TYPE_MANDATORY.equals(propertyId)) {
                    return i18n.getMessage(UIMessageIdProvider.TOOLTIP_CHECK_FOR_MANDATORY);
                }
                return null;
            }
        });
    }

    private void buildSourceTable() {
        sourceTable = new Table();
        sourceTable.setId(SPUIDefinitions.TWIN_TABLE_SOURCE_ID);
        sourceTable.setSelectable(true);
        sourceTable.setMultiSelect(true);
        sourceTable.addStyleName(ValoTheme.TABLE_NO_HORIZONTAL_LINES);
        sourceTable.addStyleName(ValoTheme.TABLE_NO_STRIPES);
        sourceTable.addStyleName(ValoTheme.TABLE_NO_VERTICAL_LINES);
        sourceTable.addStyleName(ValoTheme.TABLE_SMALL);
        sourceTable.setImmediate(true);
        sourceTable.setSizeFull();
        sourceTable.addStyleName("dist_type_twin-table");
        sourceTable.setSortEnabled(false);
        sourceTableContainer = new IndexedContainer();
        sourceTableContainer.addContainerProperty(DIST_TYPE_NAME, String.class, "");
        sourceTableContainer.addContainerProperty(DIST_TYPE_DESCRIPTION, String.class, "");
        sourceTable.setContainerDataSource(sourceTableContainer);

        sourceTable.setVisibleColumns(DIST_TYPE_NAME);
        sourceTable.setColumnHeaders(i18n.getMessage("header.dist.twintable.available"));
        sourceTable.setColumnExpandRatio(DIST_TYPE_NAME, 1.0F);
        createSourceTableData();
        addTooltip();
        sourceTable.select(sourceTable.firstItemId());
    }

    @SuppressWarnings("unchecked")
    protected void createSourceTableData() {
        sourceTableContainer.removeAllItems();
        final Iterable<SoftwareModuleType> moduleTypeBeans = softwareModuleTypeManagement
                .findAll(new PageRequest(0, 1000));
        Item saveTblitem;
        for (final SoftwareModuleType swTypeTag : moduleTypeBeans) {
            saveTblitem = sourceTableContainer.addItem(swTypeTag.getId());
            saveTblitem.getItemProperty(DIST_TYPE_NAME).setValue(swTypeTag.getName());
            saveTblitem.getItemProperty(DIST_TYPE_DESCRIPTION).setValue(swTypeTag.getDescription());
        }
    }

    protected void addTooltip() {
        sourceTable.setItemDescriptionGenerator(new ItemDescriptionGenerator() {
            private static final long serialVersionUID = 1L;

            @Override
            public String generateDescription(final Component source, final Object itemId, final Object propertyId) {
                final Item item = sourceTable.getItem(itemId);
                final String description = (String) item.getItemProperty(DIST_TYPE_DESCRIPTION).getValue();
                if (DIST_TYPE_NAME.equals(propertyId) && !StringUtils.isEmpty(description)) {
                    return i18n.getMessage("label.description") + description;
                }
                return null;
            }
        });
    }

    protected void createSelectedTableContainer() {
        selectedTableContainer = new IndexedContainer();
        selectedTableContainer.addContainerProperty(DIST_TYPE_NAME, String.class, "");
        selectedTableContainer.addContainerProperty(DIST_TYPE_DESCRIPTION, String.class, "");
        selectedTableContainer.addContainerProperty(DIST_TYPE_MANDATORY, CheckBox.class, null);
    }

    @SuppressWarnings("unchecked")
    private void addSMType() {
        final Set<Long> selectedIds = (Set<Long>) sourceTable.getValue();
        if (selectedIds == null) {
            return;
        }
        for (final Long id : selectedIds) {
            addTargetTableData(id);
        }
    }

    private void addTargetTableData(final Long selectedId) {
        getSelectedTableItemData(selectedId);
        sourceTable.removeItem(selectedId);
    }

    @SuppressWarnings("unchecked")
    private void getSelectedTableItemData(final Long id) {
        Item saveTblitem;
        if (selectedTableContainer != null) {
            saveTblitem = selectedTableContainer.addItem(id);
            saveTblitem.getItemProperty(DIST_TYPE_NAME).setValue(
                    sourceTable.getContainerDataSource().getItem(id).getItemProperty(DIST_TYPE_NAME).getValue());
            final CheckBox mandatoryCheckBox = new CheckBox();
            saveTblitem.getItemProperty(DIST_TYPE_MANDATORY).setValue(mandatoryCheckBox);
            saveTblitem.getItemProperty(DIST_TYPE_DESCRIPTION).setValue(
                    sourceTable.getContainerDataSource().getItem(id).getItemProperty(DIST_TYPE_DESCRIPTION).getValue());
        }
    }

    @SuppressWarnings("unchecked")
    private void addSourceTableData(final Long selectedId) {
        if (sourceTableContainer != null) {
            Item saveTblitem;
            saveTblitem = sourceTableContainer.addItem(selectedId);
            saveTblitem.getItemProperty(DIST_TYPE_NAME).setValue(selectedTable.getContainerDataSource()
                    .getItem(selectedId).getItemProperty(DIST_TYPE_NAME).getValue());
            saveTblitem.getItemProperty(DIST_TYPE_DESCRIPTION).setValue(selectedTable.getContainerDataSource()
                    .getItem(selectedId).getItemProperty(DIST_TYPE_DESCRIPTION).getValue());
        }
    }

    private void removeSMType() {
        @SuppressWarnings("unchecked")
        final Set<Long> selectedIds = (Set<Long>) selectedTable.getValue();
        if (selectedIds == null) {
            return;
        }
        for (final Long id : selectedIds) {
            addSourceTableData(id);
            selectedTable.removeItem(id);
        }
    }

    public Table getSelectedTable() {
        return selectedTable;
    }

    public static String getDistTypeMandatory() {
        return DIST_TYPE_MANDATORY;
    }

    public static String getDistTypeDescription() {
        return DIST_TYPE_DESCRIPTION;
    }

    public static String getDistTypeName() {
        return DIST_TYPE_NAME;
    }

    public Table getSourceTable() {
        return sourceTable;
    }

    public IndexedContainer getSourceTableContainer() {
        return sourceTableContainer;
    }

    protected static boolean isMandatoryModuleType(final Item item) {
        final CheckBox mandatoryCheckBox = (CheckBox) item.getItemProperty(getDistTypeMandatory()).getValue();
        return mandatoryCheckBox.getValue();
    }

    public IndexedContainer getSelectedTableContainer() {
        return selectedTableContainer;
    }

    protected static boolean isOptionalModuleType(final Item item) {
        return !isMandatoryModuleType(item);
    }

    public HorizontalLayout getDistTypeSelectLayout() {
        return distTypeSelectLayout;
    }

    public void setDistTypeSelectLayout(final HorizontalLayout distTypeSelectLayout) {
        this.distTypeSelectLayout = distTypeSelectLayout;
    }

    /**
     * Resets the tables for selecting the software modules
     */
    public void reset() {
        selectedTableContainer.removeAllItems();
        createSourceTableData();
    }

}
