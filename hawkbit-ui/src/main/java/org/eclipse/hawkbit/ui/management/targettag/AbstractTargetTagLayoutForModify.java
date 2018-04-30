/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.targettag;

import java.util.List;
import java.util.Optional;

import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.TargetTagManagement;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.colorpicker.ColorPickerConstants;
import org.eclipse.hawkbit.ui.colorpicker.ColorPickerHelper;
import org.eclipse.hawkbit.ui.layouts.AbstractTagLayoutForModify;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.springframework.data.domain.PageRequest;
import org.vaadin.spring.events.EventBus.UIEventBus;

/**
 * Abstract class for Target Tag Layout which is used for updating and deleting
 * Target Tags, includes the combobox for selecting the tag to modify
 */
public abstract class AbstractTargetTagLayoutForModify extends AbstractTagLayoutForModify<TargetTag> {

    private static final long serialVersionUID = 1L;

    private transient TargetTagManagement targetTagManagement;

    AbstractTargetTagLayoutForModify(final VaadinMessageSource i18n, final TargetTagManagement targetTagManagement,
            final EntityFactory entityFactory, final UIEventBus eventBus, final SpPermissionChecker permChecker,
            final UINotification uiNotification) {
        super(i18n, entityFactory, eventBus, permChecker, uiNotification);
        this.targetTagManagement = targetTagManagement;
        init();
    }

    @Override
    protected void setTagDetails(final String distTagSelected) {
        getTagName().setValue(distTagSelected);
        getTagName().setEnabled(false);
        final Optional<TargetTag> selectedTargetTag = targetTagManagement.getByName(distTagSelected);
        selectedTargetTag.ifPresent(tag -> {
            getTagDesc().setValue(selectedTargetTag.get().getDescription());
            if (tag.getColour() == null) {
                setTagColor(getColorPickerLayout().getDefaultColor(), ColorPickerConstants.DEFAULT_COLOR);
            } else {
                setTagColor(ColorPickerHelper.rgbToColorConverter(tag.getColour()), tag.getColour());
            }
        });
    }

    @Override
    protected Optional<TargetTag> findEntityByName() {
        return targetTagManagement.getByName(getTagName().getValue());
    }

    @Override
    protected void populateTagNameCombo() {
        if (getUpdateCombobox().getTagNameComboBox() == null) {
            return;
        }
        getUpdateCombobox().getTagNameComboBox().removeAllItems();
        final List<TargetTag> distTagNameList = targetTagManagement.findAll(new PageRequest(0, getMaxTags()))
                .getContent();
        distTagNameList.forEach(value -> getUpdateCombobox().getTagNameComboBox().addItem(value.getName()));
    }

    public TargetTagManagement getTargetTagManagement() {
        return targetTagManagement;
    }

}
