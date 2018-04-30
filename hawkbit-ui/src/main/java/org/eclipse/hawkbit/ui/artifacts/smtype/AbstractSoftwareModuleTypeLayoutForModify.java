/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts.smtype;

import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.SoftwareModuleTypeManagement;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.common.SoftwareModuleTypeBeanQuery;
import org.eclipse.hawkbit.ui.components.UpdateComboBoxForTagsAndTypes;
import org.eclipse.hawkbit.ui.distributions.smtype.AbstractSoftwareModuleTypeLayout;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.addons.lazyquerycontainer.BeanQueryFactory;
import org.vaadin.spring.events.EventBus.UIEventBus;

import com.vaadin.data.Property.ValueChangeEvent;

/**
 * Abstract class for Software Module Type Layout which is used for updating and
 * deleting Software Module Types, includes the combobox for selecting the type
 * to modify
 *
 */
public abstract class AbstractSoftwareModuleTypeLayoutForModify extends AbstractSoftwareModuleTypeLayout {

    private static final long serialVersionUID = 1L;

    private UpdateComboBoxForTagsAndTypes updateCombobox;

    /**
     * Constructor for CreateUpdateSoftwareTypeLayout
     * 
     * @param i18n
     *            I18N
     * @param entityFactory
     *            EntityFactory
     * @param eventBus
     *            UIEventBus
     * @param permChecker
     *            SpPermissionChecker
     * @param uiNotification
     *            UINotification
     * @param softwareModuleTypeManagement
     *            management for {@link SoftwareModuleType}s
     */
    public AbstractSoftwareModuleTypeLayoutForModify(final VaadinMessageSource i18n, final EntityFactory entityFactory,
            final UIEventBus eventBus, final SpPermissionChecker permChecker, final UINotification uiNotification,
            final SoftwareModuleTypeManagement softwareModuleTypeManagement) {
        super(i18n, entityFactory, eventBus, permChecker, uiNotification, softwareModuleTypeManagement);
        init();
        populateTagNameCombo();
    }

    @Override
    protected void createRequiredComponents() {
        super.createRequiredComponents();
        updateCombobox = new UpdateComboBoxForTagsAndTypes(
                getI18n().getMessage("label.choose.type", getI18n().getMessage("label.choose.tag.update")),
                getI18n().getMessage("label.combobox.type"));
        updateCombobox.getTagNameComboBox().addValueChangeListener(this::tagNameChosen);
    }

    protected void tagNameChosen(final ValueChangeEvent event) {
        final String tagSelected = (String) event.getProperty().getValue();
        if (tagSelected != null) {
            setTagDetails(tagSelected);
        } else {
            resetFields();
        }
        if (isUpdateAction()) {
            getWindow().setOrginaleValues();
        }
    }

    @Override
    protected void buildLayout() {
        super.buildLayout();
        getFormLayout().addComponent(updateCombobox, 0);
        disableFields();
    }

    @Override
    protected void disableFields() {
        getTagName().setEnabled(false);
        getTypeKey().setEnabled(false);
        getAssignOptiongroup().setEnabled(false);
    }

    /**
     * Select tag & set tag name & tag desc values corresponding to selected
     * tag.
     * 
     * @param targetTagSelected
     *            as the selected tag from combo
     */
    protected void setTagDetails(final String targetTagSelected) {
        getTagName().setValue(targetTagSelected);
        getSoftwareModuleTypeManagement().getByName(targetTagSelected).ifPresent(selectedTypeTag -> {
            getTagDesc().setValue(selectedTypeTag.getDescription());
            getTypeKey().setValue(selectedTypeTag.getKey());
            if (selectedTypeTag.getMaxAssignments() == 1) {
                getAssignOptiongroup().setValue(getSingleAssignStr());
            } else {
                getAssignOptiongroup().setValue(getMultiAssignStr());
            }
            setColorPickerComponentsColor(selectedTypeTag.getColour());
        });
        disableFields();
    }

    @Override
    protected boolean isUpdateAction() {
        return true;
    }

    private void populateTagNameCombo() {
        updateCombobox.getTagNameComboBox().setContainerDataSource(
                HawkbitCommonUtil.createLazyQueryContainer(new BeanQueryFactory<>(SoftwareModuleTypeBeanQuery.class)));
        updateCombobox.getTagNameComboBox().setItemCaptionPropertyId(SPUILabelDefinitions.VAR_NAME);
    }

    public UpdateComboBoxForTagsAndTypes getUpdateCombobox() {
        return updateCombobox;
    }

}
