/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.dstag;

import java.util.List;
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
import org.springframework.data.domain.PageRequest;
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
            final UIEventBus eventBus, final SpPermissionChecker permChecker, final UINotification uiNotification) {
        super(i18n, entityFactory, eventBus, permChecker, uiNotification);
        this.distributionSetTagManagement = distributionSetTagManagement;
        init();
    }

    @Override
    protected void setTagDetails(final String distTagSelected) {
        getTagName().setValue(distTagSelected);
        getTagName().setEnabled(false);
        final Optional<DistributionSetTag> selectedDistTag = distributionSetTagManagement.getByName(distTagSelected);
        if (selectedDistTag.isPresent()) {
            getTagDesc().setValue(selectedDistTag.get().getDescription());
            if (selectedDistTag.get().getColour() == null) {
                setTagColor(getColorPickerLayout().getDefaultColor(), ColorPickerConstants.DEFAULT_COLOR);
            } else {
                setTagColor(ColorPickerHelper.rgbToColorConverter(selectedDistTag.get().getColour()),
                        selectedDistTag.get().getColour());
            }
        }
    }

    @Override
    protected Optional<DistributionSetTag> findEntityByName() {
        return distributionSetTagManagement.getByName(getTagName().getValue());
    }

    @Override
    protected void populateTagNameCombo() {
        if (getUpdateCombobox().getTagNameComboBox() == null) {
            return;
        }
        getUpdateCombobox().getTagNameComboBox().removeAllItems();
        final List<DistributionSetTag> distTagNameList = distributionSetTagManagement
                .findAll(new PageRequest(0, getMaxTags())).getContent();
        distTagNameList.forEach(value -> getUpdateCombobox().getTagNameComboBox().addItem(value.getName()));
    }

    public DistributionSetTagManagement getDistributionSetTagManagement() {
        return distributionSetTagManagement;
    }

}
