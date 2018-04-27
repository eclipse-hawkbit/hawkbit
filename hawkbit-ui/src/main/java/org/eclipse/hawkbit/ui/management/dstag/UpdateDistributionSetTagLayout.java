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
import org.eclipse.hawkbit.ui.layouts.AbstractUpdateTagLayout;
import org.eclipse.hawkbit.ui.management.event.DistributionSetTagTableEvent;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.springframework.data.domain.PageRequest;
import org.vaadin.spring.events.EventBus.UIEventBus;

/**
 * Layout for the pop-up window which is created when updating a distribution
 * set tag on the Deployment View.
 */
public class UpdateDistributionSetTagLayout extends AbstractUpdateTagLayout<DistributionSetTag> {

    private static final long serialVersionUID = 1L;

    private final transient DistributionSetTagManagement distributionSetTagManagement;

    UpdateDistributionSetTagLayout(final VaadinMessageSource i18n,
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
    protected String getWindowCaption() {
        return getI18n().getMessage("caption.configure", getI18n().getMessage("caption.update"),
                getI18n().getMessage("caption.tag"));
    }

    @Override
    protected Optional<DistributionSetTag> findEntityByName() {
        return distributionSetTagManagement.getByName(getTagName().getValue());
    }

    @Override
    protected void saveEntity() {
        updateExistingTag(findEntityByName()
                .orElseThrow(() -> new EntityNotFoundException(DistributionSetTag.class, getTagName().getValue())));
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

    private void updateExistingTag(final Tag targetObj) {
        final TagUpdate update = getEntityFactory().tag().update(targetObj.getId()).name(getTagName().getValue())
                .description(getTagDesc().getValue())
                .colour(ColorPickerHelper.getColorPickedString(getColorPickerLayout().getSelPreview()));
        distributionSetTagManagement.update(update);
        getEventBus().publish(this,
                new DistributionSetTagTableEvent(BaseEntityEventType.UPDATED_ENTITY, (DistributionSetTag) targetObj));
        getUiNotification()
                .displaySuccess(getI18n().getMessage("message.update.success", new Object[] { targetObj.getName() }));
    }
}
