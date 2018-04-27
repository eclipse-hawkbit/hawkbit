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
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.artifacts.event.SoftwareModuleTypeEvent;
import org.eclipse.hawkbit.ui.artifacts.event.SoftwareModuleTypeEvent.SoftwareModuleTypeEnum;
import org.eclipse.hawkbit.ui.colorpicker.ColorPickerHelper;
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
 * Layout for the pop-up window which is created when updating a software module
 * type on the Upload or Distributions View.
 */
public class UpdateSoftwareTypeLayout extends AbstractSoftwareModuleTypeLayout {

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
    public UpdateSoftwareTypeLayout(final VaadinMessageSource i18n, final EntityFactory entityFactory,
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
            resetTagNameField();
        }
        if (isUpdateAction()) {
            getWindow().setOrginaleValues();
        }
    }

    @Override
    protected void buildLayout() {
        super.buildLayout();
        getFormLayout().addComponent(updateCombobox, 0);
        getTypeKey().setEnabled(false);
        getAssignOptiongroup().setEnabled(false);
    }

    @Override
    protected String getWindowCaption() {
        return getI18n().getMessage("caption.configure", getI18n().getMessage("caption.update"),
                getI18n().getMessage("caption.type"));
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
    }

    @Override
    protected void saveEntity() {
        updateSWModuleType(findEntityByName()
                .orElseThrow(() -> new EntityNotFoundException(DistributionSetType.class, getTagName().getValue())));
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

    private void updateSWModuleType(final SoftwareModuleType existingType) {
        getSoftwareModuleTypeManagement().update(getEntityFactory().softwareModuleType().update(existingType.getId())
                .description(getTagDesc().getValue())
                .colour(ColorPickerHelper.getColorPickedString(getColorPickerLayout().getSelPreview())));
        getUiNotification().displaySuccess(
                getI18n().getMessage("message.update.success", new Object[] { existingType.getName() }));
        getEventBus().publish(this,
                new SoftwareModuleTypeEvent(SoftwareModuleTypeEnum.UPDATE_SOFTWARE_MODULE_TYPE, existingType));
    }

    public UpdateComboBoxForTagsAndTypes getUpdateCombobox() {
        return updateCombobox;
    }

}
