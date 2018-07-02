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
import org.eclipse.hawkbit.ui.artifacts.event.SoftwareModuleTypeEvent;
import org.eclipse.hawkbit.ui.artifacts.event.SoftwareModuleTypeEvent.SoftwareModuleTypeEnum;
import org.eclipse.hawkbit.ui.colorpicker.ColorPickerHelper;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus.UIEventBus;

/**
 * Layout for the pop-up window which is created when creating a software module
 * type on the Upload or Distribution View.
 */
public class CreateSoftwareModuleTypeLayout extends AbstractSoftwareModuleTypeLayout {

    private static final long serialVersionUID = 1L;

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
    public CreateSoftwareModuleTypeLayout(final VaadinMessageSource i18n, final EntityFactory entityFactory,
            final UIEventBus eventBus, final SpPermissionChecker permChecker, final UINotification uiNotification,
            final SoftwareModuleTypeManagement softwareModuleTypeManagement) {
        super(i18n, entityFactory, eventBus, permChecker, uiNotification, softwareModuleTypeManagement);
    }

    @Override
    protected String getWindowCaption() {
        return getI18n().getMessage("caption.create.new", getI18n().getMessage("caption.type"));
    }

    @Override
    protected void saveEntity() {
        createNewSWModuleType();
    }

    private void createNewSWModuleType() {
        int assignNumber = 0;
        final String colorPicked = ColorPickerHelper.getColorPickedString(getColorPickerLayout().getSelPreview());
        final String typeNameValue = getTagName().getValue();
        final String typeKeyValue = getTypeKey().getValue();
        final String typeDescValue = getTagDesc().getValue();
        final String assignValue = (String) getAssignOptiongroup().getValue();
        if (assignValue != null && assignValue.equalsIgnoreCase(getSingleAssignStr())) {
            assignNumber = 1;
        } else if (assignValue != null && assignValue.equalsIgnoreCase(getMultiAssignStr())) {
            assignNumber = Integer.MAX_VALUE;
        }

        if (typeNameValue != null && typeKeyValue != null) {
            final SoftwareModuleType newSWType = getSoftwareModuleTypeManagement()
                    .create(getEntityFactory().softwareModuleType().create().key(typeKeyValue).name(typeNameValue)
                            .description(typeDescValue).colour(colorPicked).maxAssignments(assignNumber));
            getUiNotification().displaySuccess(getI18n().getMessage("message.save.success", newSWType.getName()));
            getEventBus().publish(this,
                    new SoftwareModuleTypeEvent(SoftwareModuleTypeEnum.ADD_SOFTWARE_MODULE_TYPE, newSWType));
        } else {
            getUiNotification().displayValidationError(getI18n().getMessage("message.error.missing.typenameorkey"));
        }
    }

    @Override
    protected boolean isUpdateAction() {
        return false;
    }

}
