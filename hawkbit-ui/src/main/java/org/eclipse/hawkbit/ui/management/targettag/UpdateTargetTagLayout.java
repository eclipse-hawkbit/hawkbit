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
import org.eclipse.hawkbit.repository.builder.TagUpdate;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.model.Tag;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.colorpicker.ColorPickerConstants;
import org.eclipse.hawkbit.ui.colorpicker.ColorPickerHelper;
import org.eclipse.hawkbit.ui.common.table.BaseEntityEventType;
import org.eclipse.hawkbit.ui.layouts.AbstractUpdateTagLayout;
import org.eclipse.hawkbit.ui.management.event.TargetTagTableEvent;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.springframework.data.domain.PageRequest;
import org.vaadin.spring.events.EventBus.UIEventBus;

/**
 * Layout for pop-up window which is created when updating a target tag on the
 * Deployment View.
 */
public class UpdateTargetTagLayout extends AbstractUpdateTagLayout<TargetTag> {

    private static final long serialVersionUID = 1L;

    private final transient TargetTagManagement targetTagManagement;

    UpdateTargetTagLayout(final VaadinMessageSource i18n, final TargetTagManagement targetTagManagement,
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
    protected String getWindowCaption() {
        return getI18n().getMessage("caption.configure", getI18n().getMessage("caption.update"),
                getI18n().getMessage("caption.tag"));
    }

    @Override
    protected Optional<TargetTag> findEntityByName() {
        return targetTagManagement.getByName(getTagName().getValue());
    }

    @Override
    protected void saveEntity() {
        updateExistingTag(findEntityByName()
                .orElseThrow(() -> new EntityNotFoundException(TargetTag.class, getTagName().getValue())));
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

    private void updateExistingTag(final Tag targetObj) {
        final TagUpdate update = getEntityFactory().tag().update(targetObj.getId()).name(getTagName().getValue())
                .description(getTagDesc().getValue())
                .colour(ColorPickerHelper.getColorPickedString(getColorPickerLayout().getSelPreview()));

        targetTagManagement.update(update);
        getEventBus().publish(this, new TargetTagTableEvent(BaseEntityEventType.UPDATED_ENTITY, (TargetTag) targetObj));
        getUiNotification()
                .displaySuccess(getI18n().getMessage("message.update.success", new Object[] { targetObj.getName() }));
    }
}
