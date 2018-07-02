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
import org.eclipse.hawkbit.ui.layouts.UpdateTag;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus.UIEventBus;

import com.vaadin.ui.Window.CloseListener;

/**
 * Layout for the pop-up window which is created when updating a Software Module
 * Type on the Upload View.
 *
 */
public class UpdateSoftwareModuleTypeLayout extends AbstractSoftwareModuleTypeLayout implements UpdateTag {

    private static final long serialVersionUID = 1L;

    private final String selectedTypeName;

    private final CloseListener closeListener;

    /**
     * Constructor for initializing the pop-up window for updating a software
     * module type. The form fields are filled with the current data of the
     * selected software module type.
     * 
     * @param i18n
     *            VaadinMessageSource
     * @param entityFactory
     *            EntityFactory
     * @param eventBus
     *            UIEventBus
     * @param permChecker
     *            SpPermissionChecker
     * @param uiNotification
     *            UINotification
     * @param softwareModuleTypeManagement
     *            SoftwareModuleTypeManagement
     * @param selectedTypeName
     *            The name of the selected software module type to update
     * @param closeListener
     *            CloseListener which describes the action to do when closing
     *            the window
     */
    public UpdateSoftwareModuleTypeLayout(final VaadinMessageSource i18n, final EntityFactory entityFactory,
            final UIEventBus eventBus, final SpPermissionChecker permChecker, final UINotification uiNotification,
            final SoftwareModuleTypeManagement softwareModuleTypeManagement, final String selectedTypeName,
            final CloseListener closeListener) {
        super(i18n, entityFactory, eventBus, permChecker, uiNotification, softwareModuleTypeManagement);
        this.selectedTypeName = selectedTypeName;
        this.closeListener = closeListener;
        initUpdatePopup();
    }

    private void initUpdatePopup() {
        setTagDetails(selectedTypeName);
        getWindow().addCloseListener(closeListener);
    }

    @Override
    protected String getWindowCaption() {
        return getI18n().getMessage("caption.update", getI18n().getMessage("caption.type"));
    }

    @Override
    protected void saveEntity() {
        updateSWModuleType(findEntityByName()
                .orElseThrow(() -> new EntityNotFoundException(DistributionSetType.class, getTagName().getValue())));
    }

    @Override
    protected void buildLayout() {
        super.buildLayout();
        disableFields();
    }

    @Override
    protected void disableFields() {
        getTagName().setEnabled(false);
        getTypeKey().setEnabled(false);
        getAssignOptiongroup().setEnabled(false);
    }

    @Override
    public void setTagDetails(final String selectedEntity) {
        getSoftwareModuleTypeManagement().getByName(selectedEntity).ifPresent(selectedTypeTag -> {
            getTagName().setValue(selectedTypeTag.getName());
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

    private void updateSWModuleType(final SoftwareModuleType existingType) {
        getSoftwareModuleTypeManagement().update(getEntityFactory().softwareModuleType().update(existingType.getId())
                .description(getTagDesc().getValue())
                .colour(ColorPickerHelper.getColorPickedString(getColorPickerLayout().getSelPreview())));
        getUiNotification().displaySuccess(getI18n().getMessage("message.update.success", existingType.getName()));
        getEventBus().publish(this,
                new SoftwareModuleTypeEvent(SoftwareModuleTypeEnum.UPDATE_SOFTWARE_MODULE_TYPE, existingType));
    }

    @Override
    protected boolean isUpdateAction() {
        return true;
    }

}
