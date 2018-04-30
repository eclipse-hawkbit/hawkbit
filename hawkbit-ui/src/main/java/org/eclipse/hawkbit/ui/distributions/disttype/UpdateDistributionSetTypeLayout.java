/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.distributions.disttype;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.DistributionSetTypeManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.SoftwareModuleTypeManagement;
import org.eclipse.hawkbit.repository.builder.DistributionSetTypeUpdate;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.colorpicker.ColorPickerHelper;
import org.eclipse.hawkbit.ui.distributions.event.DistributionSetTypeEvent;
import org.eclipse.hawkbit.ui.distributions.event.DistributionSetTypeEvent.DistributionSetTypeEnum;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.springframework.util.CollectionUtils;
import org.vaadin.spring.events.EventBus.UIEventBus;

/**
 * Layout for the pop-up window which is created when updating a Distribution
 * Set Type on the Distributions View.
 *
 */
public class UpdateDistributionSetTypeLayout extends AbstractDistributionSetTypeLayoutForModify {

    private static final long serialVersionUID = 1L;

    public UpdateDistributionSetTypeLayout(final VaadinMessageSource i18n, final EntityFactory entityFactory,
            final UIEventBus eventBus, final SpPermissionChecker permChecker, final UINotification uiNotification,
            final SoftwareModuleTypeManagement softwareModuleTypeManagement,
            final DistributionSetTypeManagement distributionSetTypeManagement,
            final DistributionSetManagement distributionSetManagement) {
        super(i18n, entityFactory, eventBus, permChecker, uiNotification, softwareModuleTypeManagement,
                distributionSetTypeManagement, distributionSetManagement);
    }

    @Override
    protected String getWindowCaption() {
        return getI18n().getMessage("caption.update") + " " + getI18n().getMessage("caption.type");
    }

    @Override
    protected void saveEntity() {
        updateDistributionSetType(findEntityByName()
                .orElseThrow(() -> new EntityNotFoundException(DistributionSetType.class, getTagName().getValue())));
    }

    @SuppressWarnings("unchecked")
    private void updateDistributionSetType(final DistributionSetType existingType) {
        final List<Long> itemIds = (List<Long>) getTwinTables().getSelectedTable().getItemIds();
        final DistributionSetTypeUpdate update = getEntityFactory().distributionSetType().update(existingType.getId())
                .description(getTagDesc().getValue())
                .colour(ColorPickerHelper.getColorPickedString(getColorPickerLayout().getSelPreview()));
        if (getDistributionSetManagement().countByTypeId(existingType.getId()) <= 0
                && !CollectionUtils.isEmpty(itemIds)) {
            update.mandatory(itemIds.stream()
                    .filter(itemId -> DistributionTypeSoftwareModuleSelectLayout
                            .isMandatoryModuleType(getTwinTables().getSelectedTable().getItem(itemId)))
                    .collect(Collectors.toList()))
                    .optional(itemIds.stream()
                            .filter(itemId -> DistributionTypeSoftwareModuleSelectLayout
                                    .isOptionalModuleType(getTwinTables().getSelectedTable().getItem(itemId)))
                            .collect(Collectors.toList()));
        }
        final DistributionSetType updateDistSetType = getDistributionSetTypeManagement().update(update);

        getUiNotification().displaySuccess(getI18n().getMessage("message.update.success", updateDistSetType.getName()));
        getEventBus().publish(this,
                new DistributionSetTypeEvent(DistributionSetTypeEnum.UPDATE_DIST_SET_TYPE, updateDistSetType));
    }

}
