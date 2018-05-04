/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.targettag;

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
            final UINotification uiNotification, final String selectedTagName) {
        super(i18n, entityFactory, eventBus, permChecker, uiNotification, selectedTagName);
        this.targetTagManagement = targetTagManagement;
    }

    @Override
    protected void setTagDetails(final String selectedTagName) {
        final Optional<TargetTag> selectedTargetTag = targetTagManagement.getByName(selectedTagName);
        selectedTargetTag.ifPresent(tag -> {
            getTagName().setValue(tag.getName());
            getTagName().setEnabled(false);
            getTagDesc().setValue(tag.getDescription());
            if (tag.getColour() == null) {
                setTagColor(getColorPickerLayout().getDefaultColor(), ColorPickerConstants.DEFAULT_COLOR);
            } else {
                setTagColor(ColorPickerHelper.rgbToColorConverter(tag.getColour()), tag.getColour());
            }
            if (isUpdateAction()) {
                getWindow().setOrginaleValues();
            }
        });
    }

    @Override
    protected Optional<TargetTag> findEntityByName() {
        return targetTagManagement.getByName(getTagName().getValue());
    }

    public TargetTagManagement getTargetTagManagement() {
        return targetTagManagement;
    }

}
