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
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.springframework.data.domain.PageRequest;
import org.vaadin.spring.events.EventBus.UIEventBus;

import com.vaadin.ui.UI;

/**
 * Class for Update Tag Layout of distribution set
 */
public class UpdateTargetTagLayoutWindow extends AbstractUpdateTagLayout<TargetTag> {

    private static final long serialVersionUID = 1L;

    private final transient TargetTagManagement targetTagManagement;

    UpdateTargetTagLayoutWindow(final VaadinMessageSource i18n, final TargetTagManagement targetTagManagement,
            final EntityFactory entityFactory, final UIEventBus eventBus, final SpPermissionChecker permChecker,
            final UINotification uiNotification) {
        super(i18n, entityFactory, eventBus, permChecker, uiNotification);
        this.targetTagManagement = targetTagManagement;
        init();
    }

    @Override
    protected void setTagDetails(final String distTagSelected) {
        tagName.setValue(distTagSelected);
        tagName.setEnabled(false);
        final Optional<TargetTag> selectedDistTag = targetTagManagement.getByName(distTagSelected);
        if (selectedDistTag.isPresent()) {
            tagDesc.setValue(selectedDistTag.get().getDescription());
            if (selectedDistTag.get().getColour() == null) {
                setTagColor(getColorPickerLayout().getDefaultColor(), ColorPickerConstants.DEFAULT_COLOR);
            } else {
                setTagColor(ColorPickerHelper.rgbToColorConverter(selectedDistTag.get().getColour()),
                        selectedDistTag.get().getColour());
            }
        }
    }

    @Override
    protected String getWindowCaption() {
        return i18n.getMessage("caption.configure", i18n.getMessage("caption.update"), i18n.getMessage("caption.tag"));
    }

    @Override
    protected Optional<TargetTag> findEntityByName() {
        return targetTagManagement.getByName(tagName.getValue());
    }

    @Override
    protected void saveEntity() {
        updateExistingTag(
                findEntityByName().orElseThrow(() -> new EntityNotFoundException(TargetTag.class, tagName.getValue())));
    }

    @Override
    protected void populateTagNameCombo() {
        if (tagNameComboBox == null) {
            return;
        }
        tagNameComboBox.removeAllItems();
        final List<TargetTag> distTagNameList = targetTagManagement.findAll(new PageRequest(0, MAX_TAGS)).getContent();
        distTagNameList.forEach(value -> tagNameComboBox.addItem(value.getName()));
    }

    private void resetTargetTagValues() {
        tagName.removeStyleName("new-target-tag-name");
        tagName.addStyleName(SPUIStyleDefinitions.NEW_TARGET_TAG_NAME);
        tagName.setValue("");
        tagName.setInputPrompt(i18n.getMessage("textfield.name"));
        setColor(ColorPickerConstants.START_COLOR);
        getWindow().setVisible(false);
        tagPreviewBtnClicked = false;
        UI.getCurrent().removeWindow(getWindow());
    }

    private void updateExistingTag(final Tag targetObj) {
        final TagUpdate update = entityFactory.tag().update(targetObj.getId()).name(tagName.getValue())
                .description(tagDesc.getValue())
                .colour(ColorPickerHelper.getColorPickedString(colorPickerLayout.getSelPreview()));

        targetTagManagement.update(update);
        eventBus.publish(this, new TargetTagTableEvent(BaseEntityEventType.UPDATED_ENTITY, (TargetTag) targetObj));
        uiNotification.displaySuccess(i18n.getMessage("message.update.success", new Object[] { targetObj.getName() }));
    }
}
