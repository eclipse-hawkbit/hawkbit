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
import org.eclipse.hawkbit.repository.builder.TagUpdate;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.repository.model.Tag;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.colorpicker.ColorPickerConstants;
import org.eclipse.hawkbit.ui.colorpicker.ColorPickerHelper;
import org.eclipse.hawkbit.ui.common.table.BaseEntityEventType;
import org.eclipse.hawkbit.ui.components.RefreshableContainer;
import org.eclipse.hawkbit.ui.layouts.AbstractUpdateTagLayout;
import org.eclipse.hawkbit.ui.management.event.DistributionSetTagTableEvent;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.springframework.data.domain.PageRequest;
import org.vaadin.spring.events.EventBus.UIEventBus;

import com.vaadin.ui.UI;

/**
 * Class for Update Tag Layout of distribution set
 */
public class UpdateDistributionTagLayoutWindow extends AbstractUpdateTagLayout<DistributionSetTag>
        implements RefreshableContainer {

    private static final long serialVersionUID = 1L;

    private final transient DistributionSetTagManagement distributionSetTagManagement;

    UpdateDistributionTagLayoutWindow(final VaadinMessageSource i18n,
            final DistributionSetTagManagement distributionSetTagManagement, final EntityFactory entityFactory,
            final UIEventBus eventBus, final SpPermissionChecker permChecker, final UINotification uiNotification) {
        super(i18n, entityFactory, eventBus, permChecker, uiNotification);
        this.distributionSetTagManagement = distributionSetTagManagement;
    }

    @Override
    public void discard() {
        super.discard();
        resetDistTagValues();
    }

    @Override
    public void refreshContainer() {
        populateTagNameCombo();
    }

    @Override
    protected void setTagDetails(final String distTagSelected) {
        tagName.setValue(distTagSelected);
        tagName.setEnabled(false);
        final Optional<DistributionSetTag> selectedDistTag = distributionSetTagManagement.getByName(distTagSelected);
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
    protected Optional<DistributionSetTag> findEntityByName() {
        return distributionSetTagManagement.getByName(tagName.getValue());
    }

    @Override
    protected void saveEntity() {
        updateExistingTag(findEntityByName()
                .orElseThrow(() -> new EntityNotFoundException(DistributionSetTag.class, tagName.getValue())));
    }

    @Override
    protected void populateTagNameCombo() {
        if (tagNameComboBox == null) {
            return;
        }
        tagNameComboBox.removeAllItems();
        final List<DistributionSetTag> distTagNameList = distributionSetTagManagement
                .findAll(new PageRequest(0, MAX_TAGS)).getContent();
        distTagNameList.forEach(value -> tagNameComboBox.addItem(value.getName()));
    }

    private void resetDistTagValues() {
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
        distributionSetTagManagement.update(update);
        eventBus.publish(this,
                new DistributionSetTagTableEvent(BaseEntityEventType.UPDATED_ENTITY, (DistributionSetTag) targetObj));
        uiNotification.displaySuccess(i18n.getMessage("message.update.success", new Object[] { targetObj.getName() }));
    }
}
