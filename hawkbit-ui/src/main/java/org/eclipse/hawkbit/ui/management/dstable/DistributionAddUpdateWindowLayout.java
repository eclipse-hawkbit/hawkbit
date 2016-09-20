/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.dstable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.TenantMetaData;
import org.eclipse.hawkbit.ui.common.CommonDialogWindow;
import org.eclipse.hawkbit.ui.common.CommonDialogWindow.SaveDialogCloseListener;
import org.eclipse.hawkbit.ui.common.DistributionSetIdName;
import org.eclipse.hawkbit.ui.common.DistributionSetTypeBeanQuery;
import org.eclipse.hawkbit.ui.common.builder.TextAreaBuilder;
import org.eclipse.hawkbit.ui.common.builder.TextFieldBuilder;
import org.eclipse.hawkbit.ui.common.builder.WindowBuilder;
import org.eclipse.hawkbit.ui.common.table.BaseEntityEventType;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.distributions.dstable.DistributionSetTable;
import org.eclipse.hawkbit.ui.management.event.DistributionTableEvent;
import org.eclipse.hawkbit.ui.management.event.DragEvent;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.SPUIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.SpringContextHelper;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.addons.lazyquerycontainer.BeanQueryFactory;
import org.vaadin.addons.lazyquerycontainer.LazyQueryContainer;
import org.vaadin.addons.lazyquerycontainer.LazyQueryDefinition;
import org.vaadin.spring.events.EventBus;

import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.themes.ValoTheme;

/**
 * WindowContent for adding/editing a Distribution
 */
@SpringComponent
@ViewScope
public class DistributionAddUpdateWindowLayout extends CustomComponent {

    private static final long serialVersionUID = -5602182034230568435L;

    @Autowired
    private I18N i18n;

    @Autowired
    private transient UINotification notificationMessage;

    @Autowired
    private transient EventBus.SessionEventBus eventBus;

    @Autowired
    private transient DistributionSetManagement distributionSetManagement;

    @Autowired
    private transient SystemManagement systemManagement;

    @Autowired
    private transient EntityFactory entityFactory;

    private TextField distNameTextField;
    private TextField distVersionTextField;
    private TextArea descTextArea;
    private CheckBox reqMigStepCheckbox;
    private ComboBox distsetTypeNameComboBox;
    private boolean editDistribution = Boolean.FALSE;
    private Long editDistId;
    private CommonDialogWindow window;

    private FormLayout formLayout;

    /**
     * Save or update distribution set.
     *
     */
    private final class SaveOnCloseDialogListener implements SaveDialogCloseListener {
        @Override
        public void saveOrUpdate() {
            if (editDistribution) {
                updateDistribution();
                return;
            }
            addNewDistribution();
        }

        @Override
        public boolean canWindowSaveOrUpdate() {
            return !isDuplicate();
        }

    }

    /**
     * Initialize Distribution Add and Edit Window.
     */
    @PostConstruct
    void init() {
        createRequiredComponents();
        buildLayout();
    }

    private void buildLayout() {
        addStyleName("lay-color");
        setSizeUndefined();

        formLayout = new FormLayout();
        formLayout.addComponent(distsetTypeNameComboBox);
        formLayout.addComponent(distNameTextField);
        formLayout.addComponent(distVersionTextField);
        formLayout.addComponent(descTextArea);
        formLayout.addComponent(reqMigStepCheckbox);

        setCompositionRoot(formLayout);
        distNameTextField.focus();
    }

    /**
     * Create required UI components.
     */
    private void createRequiredComponents() {
        distNameTextField = createTextField("textfield.name", SPUIComponentIdProvider.DIST_ADD_NAME);
        distVersionTextField = createTextField("textfield.version", SPUIComponentIdProvider.DIST_ADD_VERSION);

        distsetTypeNameComboBox = SPUIComponentProvider.getComboBox(i18n.get("label.combobox.type"), "", null, "",
                false, "", i18n.get("label.combobox.type"));
        distsetTypeNameComboBox.setImmediate(true);
        distsetTypeNameComboBox.setNullSelectionAllowed(false);
        distsetTypeNameComboBox.setId(SPUIComponentIdProvider.DIST_ADD_DISTSETTYPE);
        populateDistSetTypeNameCombo();

        descTextArea = new TextAreaBuilder().caption(i18n.get("textfield.description")).style("text-area-style")
                .prompt(i18n.get("textfield.description")).immediate(true).id(SPUIComponentIdProvider.DIST_ADD_DESC)
                .buildTextComponent();
        descTextArea.setNullRepresentation("");

        reqMigStepCheckbox = SPUIComponentProvider.getCheckBox(i18n.get("checkbox.dist.required.migration.step"),
                "dist-checkbox-style", null, false, "");
        reqMigStepCheckbox.addStyleName(ValoTheme.CHECKBOX_SMALL);
        reqMigStepCheckbox.setId(SPUIComponentIdProvider.DIST_ADD_MIGRATION_CHECK);
    }

    private TextField createTextField(final String in18Key, final String id) {
        final TextField buildTextField = new TextFieldBuilder().caption(i18n.get(in18Key)).required(true)
                .prompt(i18n.get(in18Key)).immediate(true).id(id).buildTextComponent();
        buildTextField.setNullRepresentation("");
        return buildTextField;
    }

    /**
     * Get the LazyQueryContainer instance for DistributionSetTypes.
     *
     * @return
     */
    private LazyQueryContainer getDistSetTypeLazyQueryContainer() {
        final BeanQueryFactory<DistributionSetTypeBeanQuery> dtQF = new BeanQueryFactory<>(
                DistributionSetTypeBeanQuery.class);
        dtQF.setQueryConfiguration(Collections.emptyMap());

        final LazyQueryContainer disttypeContainer = new LazyQueryContainer(
                new LazyQueryDefinition(true, SPUIDefinitions.DIST_TYPE_SIZE, SPUILabelDefinitions.VAR_NAME), dtQF);

        disttypeContainer.addContainerProperty(SPUILabelDefinitions.VAR_NAME, String.class, "", true, true);

        return disttypeContainer;
    }

    private DistributionSetType getDefaultDistributionSetType() {
        final TenantMetaData tenantMetaData = systemManagement.getTenantMetadata();
        return tenantMetaData.getDefaultDsType();
    }

    /**
     * Update Distribution.
     */
    private void updateDistribution() {

        if (isDuplicate()) {
            return;
        }
        final String name = HawkbitCommonUtil.trimAndNullIfEmpty(distNameTextField.getValue());
        final String version = HawkbitCommonUtil.trimAndNullIfEmpty(distVersionTextField.getValue());
        final String distSetTypeName = HawkbitCommonUtil
                .trimAndNullIfEmpty((String) distsetTypeNameComboBox.getValue());
        final DistributionSet currentDS = distributionSetManagement.findDistributionSetByIdWithDetails(editDistId);
        final String desc = HawkbitCommonUtil.trimAndNullIfEmpty(descTextArea.getValue());
        final boolean isMigStepReq = reqMigStepCheckbox.getValue();

        /* identify the changes */
        setDistributionValues(currentDS, name, version, distSetTypeName, desc, isMigStepReq);
        distributionSetManagement.updateDistributionSet(currentDS);
        notificationMessage.displaySuccess(i18n.get("message.new.dist.save.success",
                new Object[] { currentDS.getName(), currentDS.getVersion() }));
        // update table row+details layout
        eventBus.publish(this, new DistributionTableEvent(BaseEntityEventType.UPDATED_ENTITY, currentDS));

    }

    /**
     * Add new Distribution set.
     */
    private void addNewDistribution() {
        editDistribution = Boolean.FALSE;

        final String name = HawkbitCommonUtil.trimAndNullIfEmpty(distNameTextField.getValue());
        final String version = HawkbitCommonUtil.trimAndNullIfEmpty(distVersionTextField.getValue());
        final String distSetTypeName = HawkbitCommonUtil
                .trimAndNullIfEmpty((String) distsetTypeNameComboBox.getValue());

        final String desc = HawkbitCommonUtil.trimAndNullIfEmpty(descTextArea.getValue());
        final boolean isMigStepReq = reqMigStepCheckbox.getValue();
        DistributionSet newDist = entityFactory.generateDistributionSet();

        setDistributionValues(newDist, name, version, distSetTypeName, desc, isMigStepReq);
        newDist = distributionSetManagement.createDistributionSet(newDist);

        notificationMessage.displaySuccess(
                i18n.get("message.new.dist.save.success", new Object[] { newDist.getName(), newDist.getVersion() }));

        final Set<DistributionSetIdName> s = new HashSet<>();
        s.add(new DistributionSetIdName(newDist.getId(), newDist.getName(), newDist.getVersion()));
        final DistributionSetTable distributionSetTable = SpringContextHelper.getBean(DistributionSetTable.class);
        distributionSetTable.setValue(s);
    }

    /**
     * Set Values for Distribution set.
     *
     * @param distributionSet
     *            as reference
     * @param name
     *            as string
     * @param version
     *            as string
     * @param desc
     *            as string
     * @param isMigStepReq
     *            as string
     */
    private void setDistributionValues(final DistributionSet distributionSet, final String name, final String version,
            final String distSetTypeName, final String desc, final boolean isMigStepReq) {
        distributionSet.setName(name);
        distributionSet.setVersion(version);
        distributionSet.setType(distributionSetManagement.findDistributionSetTypeByName(distSetTypeName));
        distributionSet.setDescription(desc != null ? desc : "");
        distributionSet.setRequiredMigrationStep(isMigStepReq);
    }

    private boolean isDuplicate() {
        final String name = distNameTextField.getValue();
        final String version = distVersionTextField.getValue();

        final DistributionSet existingDs = distributionSetManagement.findDistributionSetByNameAndVersion(name, version);
        /*
         * Distribution should not exists with the same name & version. Display
         * error message, when the "existingDs" is not null and it is add window
         * (or) when the "existingDs" is not null and it is edit window and the
         * distribution Id of the edit window is different then the "existingDs"
         */
        if (existingDs != null && !existingDs.getId().equals(editDistId)) {
            distNameTextField.addStyleName(SPUIStyleDefinitions.SP_TEXTFIELD_LAYOUT_ERROR_HIGHTLIGHT);
            distVersionTextField.addStyleName(SPUIStyleDefinitions.SP_TEXTFIELD_LAYOUT_ERROR_HIGHTLIGHT);
            notificationMessage.displayValidationError(
                    i18n.get("message.duplicate.dist", new Object[] { existingDs.getName(), existingDs.getVersion() }));

            return true;
        }

        return false;

    }

    /**
     * clear all the fields.
     */
    public void resetComponents() {
        editDistribution = Boolean.FALSE;
        distNameTextField.clear();
        distNameTextField.removeStyleName("v-textfield-error");
        distVersionTextField.clear();
        distVersionTextField.removeStyleName(SPUIStyleDefinitions.SP_TEXTFIELD_LAYOUT_ERROR_HIGHTLIGHT);
        distsetTypeNameComboBox.removeStyleName(SPUIStyleDefinitions.SP_TEXTFIELD_LAYOUT_ERROR_HIGHTLIGHT);
        descTextArea.clear();
        reqMigStepCheckbox.clear();
    }

    private void populateValuesOfDistribution(final Long editDistId) {
        this.editDistId = editDistId;

        if (editDistId == null) {
            return;
        }

        final DistributionSet distSet = distributionSetManagement.findDistributionSetByIdWithDetails(editDistId);
        if (distSet == null) {
            return;
        }

        editDistribution = Boolean.TRUE;
        distNameTextField.setValue(distSet.getName());
        distVersionTextField.setValue(distSet.getVersion());
        if (distSet.getType().isDeleted()) {
            distsetTypeNameComboBox.addItem(distSet.getType().getName());
        }
        distsetTypeNameComboBox.setValue(distSet.getType().getName());
        reqMigStepCheckbox.setValue(distSet.isRequiredMigrationStep());
        if (distSet.getDescription() != null) {
            descTextArea.setValue(distSet.getDescription());
        }
    }

    /**
     * Returns the dialog window for the distributions.
     * 
     * @param editDistId
     * @return window
     */
    public CommonDialogWindow getWindow(final Long editDistId) {
        eventBus.publish(this, DragEvent.HIDE_DROP_HINT);
        resetComponents();
        populateDistSetTypeNameCombo();
        populateValuesOfDistribution(editDistId);
        window = new WindowBuilder(SPUIDefinitions.CREATE_UPDATE_WINDOW).caption(i18n.get("caption.add.new.dist"))
                .content(this).layout(formLayout).i18n(i18n).saveDialogCloseListener(new SaveOnCloseDialogListener())
                .buildCommonDialogWindow();

        return window;
    }

    /**
     * Populate DistributionSet Type name combo.
     */
    private void populateDistSetTypeNameCombo() {
        distsetTypeNameComboBox.setContainerDataSource(getDistSetTypeLazyQueryContainer());
        distsetTypeNameComboBox.setItemCaptionPropertyId(SPUILabelDefinitions.VAR_NAME);
        distsetTypeNameComboBox.setValue(getDefaultDistributionSetType().getName());
    }

}
