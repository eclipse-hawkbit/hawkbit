/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.dstag;

import java.util.Optional;

import org.eclipse.hawkbit.repository.DistributionSetTagManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.colorpicker.ColorPickerConstants;
import org.eclipse.hawkbit.ui.colorpicker.ColorPickerHelper;
import org.eclipse.hawkbit.ui.layouts.AbstractTagLayoutForModify;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus.UIEventBus;

/**
 * Abstract class for Distribution Set Tag Layout which is used for updating and
 * deleting Distribution Set Tags, includes the combobox for selecting the tag
 * to modify
 */
public abstract class AbstractDistributionSetTagLayoutForModify extends AbstractTagLayoutForModify<DistributionSetTag> {

    private static final long serialVersionUID = 1L;

    private final transient DistributionSetTagManagement distributionSetTagManagement;

    AbstractDistributionSetTagLayoutForModify(final VaadinMessageSource i18n,
            final DistributionSetTagManagement distributionSetTagManagement, final EntityFactory entityFactory,
            final UIEventBus eventBus, final SpPermissionChecker permChecker, final UINotification uiNotification,
            final String selectedTagId) {
        super(i18n, entityFactory, eventBus, permChecker, uiNotification, selectedTagId);
        this.distributionSetTagManagement = distributionSetTagManagement;
    }

    @Override
    protected void setTagDetails(final String selectedTagName) {
        final Optional<DistributionSetTag> selectedDistTag = distributionSetTagManagement.getByName(selectedTagName);
        selectedDistTag.ifPresent(tag -> {
            getTagName().setValue(tag.getName());
            getTagName().setEnabled(false);
            getTagDesc().setValue(selectedDistTag.get().getDescription());
            if (selectedDistTag.get().getColour() == null) {
                setTagColor(getColorPickerLayout().getDefaultColor(), ColorPickerConstants.DEFAULT_COLOR);
            } else {
                setTagColor(ColorPickerHelper.rgbToColorConverter(selectedDistTag.get().getColour()),
                        selectedDistTag.get().getColour());
            }
            if (isUpdateAction()) {
                getWindow().setOrginaleValues();
            }
        });
    }

    @Override
    protected Optional<DistributionSetTag> findEntityByName() {
        return distributionSetTagManagement.getByName(getTagName().getValue());
    }

    public DistributionSetTagManagement getDistributionSetTagManagement() {
        return distributionSetTagManagement;
    }

}
