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

import org.eclipse.hawkbit.repository.DistributionSetTypeManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.SoftwareModuleTypeManagement;
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
 * Layout for pop-up window for creating a new Distribution Set Type on
 * Distribution View. Logic for storing the new entity.
 */
public class CreateDistributionSetTypeLayout extends AbstractDistributionSetTypeLayout {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor
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
     * @param distributionSetTypeManagement
     *            DistributionSetTypeManagement
     */
    public CreateDistributionSetTypeLayout(final VaadinMessageSource i18n, final EntityFactory entityFactory,
            final UIEventBus eventBus, final SpPermissionChecker permChecker, final UINotification uiNotification,
            final SoftwareModuleTypeManagement softwareModuleTypeManagement,
            final DistributionSetTypeManagement distributionSetTypeManagement) {
        super(i18n, entityFactory, eventBus, permChecker, uiNotification, distributionSetTypeManagement,
                softwareModuleTypeManagement);
    }

    @Override
    protected String getWindowCaption() {
        return getI18n().getMessage("caption.create.new", getI18n().getMessage("caption.type"));
    }

    @Override
    protected void saveEntity() {
        createNewDistributionSetType();
    }

    @SuppressWarnings("unchecked")
    private void createNewDistributionSetType() {

        final String colorPicked = ColorPickerHelper.getColorPickedString(getColorPickerLayout().getSelPreview());
        final String typeNameValue = getTagName().getValue();
        final String typeKeyValue = getTypeKey().getValue();
        final String typeDescValue = getTagDesc().getValue();
        final List<Long> itemIds = (List<Long>) getTwinTables().getSelectedTable().getItemIds();

        if (typeNameValue != null && typeKeyValue != null && !CollectionUtils.isEmpty(itemIds)) {
            final List<Long> mandatory = itemIds.stream()
                    .filter(itemId -> DistributionSetTypeSoftwareModuleSelectLayout
                            .isMandatoryModuleType(getTwinTables().getSelectedTable().getItem(itemId)))
                    .collect(Collectors.toList());

            final List<Long> optional = itemIds.stream()
                    .filter(itemId -> DistributionSetTypeSoftwareModuleSelectLayout
                            .isOptionalModuleType(getTwinTables().getSelectedTable().getItem(itemId)))
                    .collect(Collectors.toList());

            final DistributionSetType newDistType = getDistributionSetTypeManagement()
                    .create(getEntityFactory().distributionSetType().create().key(typeKeyValue).name(typeNameValue)
                            .description(typeDescValue).colour(colorPicked).mandatory(mandatory).optional(optional));
            getUiNotification().displaySuccess(getI18n().getMessage("message.save.success", newDistType.getName()));
            getEventBus().publish(this,
                    new DistributionSetTypeEvent(DistributionSetTypeEnum.ADD_DIST_SET_TYPE, newDistType));
        } else {
            getUiNotification().displayValidationError(getI18n().getMessage("message.error.missing.typenameorkey"));
        }
    }

    @Override
    protected boolean isUpdateAction() {
        return false;
    }

}
