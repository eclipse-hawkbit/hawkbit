/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.targettag;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.util.List;

import org.eclipse.hawkbit.eventbus.event.TargetTagCreatedBulkEvent;
import org.eclipse.hawkbit.eventbus.event.TargetTagDeletedEvent;
import org.eclipse.hawkbit.eventbus.event.TargetTagUpdateEvent;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.ui.colorPicker.ColorPickerConstants;
import org.eclipse.hawkbit.ui.colorPicker.ColorPickerHelper;
import org.eclipse.hawkbit.ui.layouts.CreateUpdateTagLayout;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.Button.ClickEvent;

/**
 *
 *
 */
@SpringComponent
@ViewScope
public class CreateUpdateTargetTagLayoutWindow extends CreateUpdateTagLayout {

    private static final long serialVersionUID = 2446682350481560235L;

    @EventBusListenerMethod(scope = EventScope.SESSION)
    // Exception squid:S1172 - event not needed
    @SuppressWarnings({ "squid:S1172" })
    void onEventTargetTagCreated(final TargetTagCreatedBulkEvent event) {
        populateTagNameCombo();
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    // Exception squid:S1172 - event not needed
    @SuppressWarnings({ "squid:S1172" })
    void onEventTargetDeletedEvent(final TargetTagDeletedEvent event) {
        populateTagNameCombo();
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    // Exception squid:S1172 - event not needed
    @SuppressWarnings({ "squid:S1172" })
    void onEventTargetTagUpdateEvent(final TargetTagUpdateEvent event) {
        populateTagNameCombo();
    }

    @Override
    protected void addListeners() {
        super.addListeners();
        optiongroup.addValueChangeListener(event -> optionValueChanged(event));
    }

    /**
     * Populate target name combo.
     */
    @Override
    public void populateTagNameCombo() {
        tagNameComboBox.removeAllItems();
        final List<TargetTag> trgTagNameList = tagManagement.findAllTargetTags();
        trgTagNameList.forEach(value -> tagNameComboBox.addItem(value.getName()));
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
        final TargetTag selectedTargetTag = tagManagement.findTargetTag(targetTagSelected);
        if (null != selectedTargetTag) {
            tagDesc.setValue(selectedTargetTag.getDescription());
            if (null == selectedTargetTag.getColour()) {
                setTagColor(getColorPickerLayout().getDefaultColor(), ColorPickerConstants.DEFAULT_COLOR);
            } else {
                setTagColor(ColorPickerHelper.rgbToColorConverter(selectedTargetTag.getColour()),
                        selectedTargetTag.getColour());
            }
        }
    }

    @Override
    public void save(final ClickEvent event) {
        if (mandatoryValuesPresent()) {
            final TargetTag existingTag = tagManagement.findTargetTag(tagName.getValue());
            if (optiongroup.getValue().equals(createTagStr)) {
                if (!checkIsDuplicate(existingTag)) {
                    createNewTag();
                }
            } else {
                updateExistingTag(existingTag);
            }
        }
    }

    /**
     * Create new tag.
     */
    @Override
    protected void createNewTag() {
        super.createNewTag();
        if (isNotEmpty(getTagNameValue())) {
            TargetTag newTargetTag = new TargetTag(getTagNameValue());
            if (isNotEmpty(getTagDescValue())) {
                newTargetTag.setDescription(getTagDescValue());
            }
            newTargetTag.setColour(ColorPickerConstants.START_COLOR.getCSS());
            if (isNotEmpty(getColorPicked())) {
                newTargetTag.setColour(getColorPicked());
            }
            newTargetTag = tagManagement.createTargetTag(newTargetTag);
            displaySuccess(newTargetTag.getName());
            closeWindow();
        } else {
            displayValidationError(i18n.get(MESSAGE_ERROR_MISSING_TAGNAME));
        }
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

}
