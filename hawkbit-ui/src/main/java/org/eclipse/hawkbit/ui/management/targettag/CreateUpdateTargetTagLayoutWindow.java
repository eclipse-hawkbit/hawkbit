/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.targettag;

import java.util.Objects;
import java.util.Optional;

import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.TargetTagManagement;
import org.eclipse.hawkbit.repository.builder.TagUpdate;
import org.eclipse.hawkbit.repository.model.Tag;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.colorpicker.ColorPickerConstants;
import org.eclipse.hawkbit.ui.colorpicker.ColorPickerHelper;
import org.eclipse.hawkbit.ui.common.table.BaseEntityEventType;
import org.eclipse.hawkbit.ui.components.RefreshableContainer;
import org.eclipse.hawkbit.ui.layouts.AbstractCreateUpdateTagLayout;
import org.eclipse.hawkbit.ui.management.event.TargetTagTableEvent;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.springframework.data.domain.PageRequest;
import org.springframework.util.StringUtils;
import org.vaadin.spring.events.EventBus.UIEventBus;

import com.vaadin.shared.ui.colorpicker.Color;

/**
 * Class for Create / Update Tag Layout of target
 */
public class CreateUpdateTargetTagLayoutWindow extends AbstractCreateUpdateTagLayout<TargetTag>
        implements RefreshableContainer {

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
    public CreateUpdateTargetTagLayoutWindow(final VaadinMessageSource i18n,
            final TargetTagManagement targetTagManagement, final EntityFactory entityFactory, final UIEventBus eventBus,
            final SpPermissionChecker permChecker, final UINotification uiNotification) {
        super(i18n, entityFactory, eventBus, permChecker, uiNotification);
        this.targetTagManagement = targetTagManagement;
    }

    @Override
    protected void addListeners() {
        super.addListeners();
        optiongroup.addValueChangeListener(this::optionValueChanged);
    }

    /**
     * Populate target name combo.
     */
    @Override
    public void populateTagNameCombo() {
        tagNameComboBox.removeAllItems();
        targetTagManagement.findAll(new PageRequest(0, MAX_TAGS))
                .forEach(value -> tagNameComboBox.addItem(value.getName()));
    }

    /**
     * @return the color which should be selected in the color-picker component.
     */
    @Override
    protected Color getColorForColorPicker() {
        return ColorPickerHelper
                .rgbToColorConverter(targetTagManagement.getByName(tagNameComboBox.getValue().toString())
                        .map(TargetTag::getColour).filter(Objects::nonNull).orElse(ColorPickerConstants.DEFAULT_COLOR));
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
    protected void updateEntity(final TargetTag entity) {
        updateExistingTag(entity);
    }

    @Override
    protected void createEntity() {
        createNewTag();
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

    /**
     * update tag.
     */
    protected void updateExistingTag(final Tag targetObj) {
        final TagUpdate update = entityFactory.tag().update(targetObj.getId()).name(tagName.getValue())
                .description(tagDesc.getValue())
                .colour(ColorPickerHelper.getColorPickedString(colorPickerLayout.getSelPreview()));

        targetTagManagement.update(update);
        eventBus.publish(this, new TargetTagTableEvent(BaseEntityEventType.UPDATED_ENTITY, (TargetTag) targetObj));

        uiNotification.displaySuccess(i18n.getMessage("message.update.success", new Object[] { targetObj.getName() }));

    }

    @Override
    protected void createRequiredComponents() {
        super.createRequiredComponents();
        createOptionGroup(permChecker.hasCreateTargetPermission(), permChecker.hasUpdateTargetPermission());
    }

    @Override
    protected void reset() {

        super.reset();
        setOptionGroupDefaultValue(permChecker.hasCreateTargetPermission(), permChecker.hasUpdateTargetPermission());
    }

    @Override
    protected String getWindowCaption() {
        return i18n.getMessage("caption.add.tag");
    }

    @Override
    public void refreshContainer() {
        populateTagNameCombo();
    }

}
