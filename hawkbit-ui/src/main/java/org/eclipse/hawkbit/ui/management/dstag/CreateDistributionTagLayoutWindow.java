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
import org.eclipse.hawkbit.ui.common.table.BaseEntityEventType;
import org.eclipse.hawkbit.ui.layouts.AbstractTagLayout;
import org.eclipse.hawkbit.ui.management.event.DistributionSetTagTableEvent;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.springframework.util.StringUtils;
import org.vaadin.spring.events.EventBus.UIEventBus;

/**
 * Class for Create/Update Tag Layout of distribution set
 */
public class CreateDistributionTagLayoutWindow extends AbstractTagLayout<DistributionSetTag> {

    private static final long serialVersionUID = 1L;

    private final transient DistributionSetTagManagement distributionSetTagManagement;

    CreateDistributionTagLayoutWindow(final VaadinMessageSource i18n,
            final DistributionSetTagManagement distributionSetTagManagement, final EntityFactory entityFactory,
            final UIEventBus eventBus, final SpPermissionChecker permChecker, final UINotification uiNotification) {
        super(i18n, entityFactory, eventBus, permChecker, uiNotification);
        this.distributionSetTagManagement = distributionSetTagManagement;
        init();
    }

    @Override
    protected void saveEntity() {
        createNewTag();
    }

    @Override
    protected Optional<DistributionSetTag> findEntityByName() {
        return distributionSetTagManagement.getByName(tagName.getValue());
    }

    /**
     * Create new tag.
     */
    @Override
    protected void createNewTag() {
        super.createNewTag();
        String colour = ColorPickerConstants.START_COLOR.getCSS();
        if (!StringUtils.isEmpty(getColorPicked())) {
            colour = getColorPicked();
        }

        final DistributionSetTag newDistTag = distributionSetTagManagement
                .create(entityFactory.tag().create().name(tagNameValue).description(tagDescValue).colour(colour));
        eventBus.publish(this, new DistributionSetTagTableEvent(BaseEntityEventType.ADD_ENTITY, newDistTag));
        displaySuccess(newDistTag.getName());
    }

    /**
     * Select tag & set tag name & tag desc values corresponding to selected
     * tag.
     *
     * @param distTagSelected
     *            as the selected tag from combo
     */
    @Override
    public void setTagDetails(final String distTagSelected) {
        tagName.setValue(distTagSelected);
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
        return i18n.getMessage("caption.configure", i18n.getMessage("caption.new"), i18n.getMessage("caption.tag"));
    }

}
