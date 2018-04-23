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
import org.eclipse.hawkbit.ui.common.table.BaseEntityEventType;
import org.eclipse.hawkbit.ui.layouts.AbstractTagLayout;
import org.eclipse.hawkbit.ui.management.event.TargetTagTableEvent;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.springframework.util.StringUtils;
import org.vaadin.spring.events.EventBus.UIEventBus;

/**
 * Class for Create / Update Tag Layout of target
 */
public class CreateTargetTagLayoutWindow extends AbstractTagLayout<TargetTag> {

    private static final long serialVersionUID = 1L;

    private final transient TargetTagManagement targetTagManagement;

    /**
     * Constructor for CreateUpdateTargetTagLayoutWindow
     * 
     * @param i18n
     *            I18N
     * @param targetTagManagement
     *            TargetTagManagement
     * @param entityFactory
     *            EntityFactory
     * @param eventBus
     *            UIEventBus
     * @param permChecker
     *            SpPermissionChecker
     * @param uiNotification
     *            UINotification
     */
    public CreateTargetTagLayoutWindow(final VaadinMessageSource i18n, final TargetTagManagement targetTagManagement,
            final EntityFactory entityFactory, final UIEventBus eventBus, final SpPermissionChecker permChecker,
            final UINotification uiNotification) {
        super(i18n, entityFactory, eventBus, permChecker, uiNotification);
        this.targetTagManagement = targetTagManagement;
        init();
    }

    /**
     * Select tag & set tag name & tag desc values corresponding to selected
     * tag.
     * 
     * @param targetTagSelected
     *            as the selected tag from combo
     */
    @Override
    public void setTagDetails(final String targetTagSelected) {
        tagName.setValue(targetTagSelected);
        final Optional<TargetTag> selectedTargetTag = targetTagManagement.getByName(targetTagSelected);
        if (selectedTargetTag.isPresent()) {
            tagDesc.setValue(selectedTargetTag.get().getDescription());
            if (null == selectedTargetTag.get().getColour()) {
                setTagColor(getColorPickerLayout().getDefaultColor(), ColorPickerConstants.DEFAULT_COLOR);
            } else {
                setTagColor(ColorPickerHelper.rgbToColorConverter(selectedTargetTag.get().getColour()),
                        selectedTargetTag.get().getColour());
            }
        }
    }

    @Override
    protected Optional<TargetTag> findEntityByName() {
        return targetTagManagement.getByName(tagName.getValue());
    }

    /**
     * Create new tag.
     */
    @Override
    protected void createNewTag() {
        super.createNewTag();
        if (!StringUtils.isEmpty(tagNameValue)) {
            String colour = ColorPickerConstants.START_COLOR.getCSS();
            if (!StringUtils.isEmpty(getColorPicked())) {
                colour = getColorPicked();
            }

            final TargetTag newTargetTag = targetTagManagement
                    .create(entityFactory.tag().create().name(tagNameValue).description(tagDescValue).colour(colour));
            eventBus.publish(this, new TargetTagTableEvent(BaseEntityEventType.ADD_ENTITY, newTargetTag));
            displaySuccess(newTargetTag.getName());
        } else {
            displayValidationError(i18n.getMessage(MESSAGE_ERROR_MISSING_TAGNAME));
        }
    }

    @Override
    protected String getWindowCaption() {
        return i18n.getMessage("caption.configure", i18n.getMessage("caption.new"), i18n.getMessage("caption.tag"));
    }

    @Override
    protected void saveEntity() {
        createNewTag();
    }

}
