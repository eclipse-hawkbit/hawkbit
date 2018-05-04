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
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus.UIEventBus;

/**
 * Layout for the pop-up window which is created when updating a Distribution
 * Set Type on the Distributions View.
 *
 */
public class UpdateSoftwareModuleTypeLayout extends AbstractSoftwareModuleTypeLayoutForModify {

    private static final long serialVersionUID = 1L;

    public UpdateSoftwareModuleTypeLayout(final VaadinMessageSource i18n, final EntityFactory entityFactory,
            final UIEventBus eventBus, final SpPermissionChecker permChecker, final UINotification uiNotification,
            final SoftwareModuleTypeManagement softwareModuleTypeManagement, final String selectedTypeName) {
        super(i18n, entityFactory, eventBus, permChecker, uiNotification, softwareModuleTypeManagement,
                selectedTypeName);
    }

    @Override
    protected String getWindowCaption() {
        return getI18n().getMessage("caption.update") + " " + getI18n().getMessage("caption.type");
    }

    @Override
    protected void saveEntity() {
        updateSWModuleType(findEntityByName()
                .orElseThrow(() -> new EntityNotFoundException(DistributionSetType.class, getTagName().getValue())));
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

}
