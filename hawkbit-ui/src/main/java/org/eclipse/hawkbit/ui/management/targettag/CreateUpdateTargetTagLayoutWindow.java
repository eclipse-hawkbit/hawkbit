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

import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.ui.colorpicker.ColorPickerConstants;
import org.eclipse.hawkbit.ui.colorpicker.ColorPickerHelper;
import org.eclipse.hawkbit.ui.layouts.AbstractCreateUpdateTagLayout;
import org.eclipse.hawkbit.ui.push.TargetTagCreatedEventContainer;
import org.eclipse.hawkbit.ui.push.TargetTagDeletedEventContainer;
import org.eclipse.hawkbit.ui.push.TargetTagUpdatedEventContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;

/**
 *
 * Class for Create / Update Tag Layout of target
 */
@SpringComponent
@ViewScope
public class CreateUpdateTargetTagLayoutWindow extends AbstractCreateUpdateTagLayout<TargetTag> {

    private static final long serialVersionUID = 2446682350481560235L;

    @Autowired
    private transient EntityFactory entityFactory;

    @EventBusListenerMethod(scope = EventScope.SESSION)
    // Exception squid:S1172 - event not needed
    @SuppressWarnings({ "squid:S1172" })
    void onEventTargetTagCreated(final TargetTagCreatedEventContainer eventContainer) {
        populateTagNameCombo();
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    // Exception squid:S1172 - event not needed
    @SuppressWarnings({ "squid:S1172" })
    void onEventTargetDeletedEvent(final TargetTagDeletedEventContainer eventContainer) {
        populateTagNameCombo();
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    // Exception squid:S1172 - event not needed
    @SuppressWarnings({ "squid:S1172" })
    void onEventTargetTagUpdateEvent(final TargetTagUpdatedEventContainer eventContainer) {
        populateTagNameCombo();
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
    protected void updateEntity(final TargetTag entity) {
        updateExistingTag(entity);
    }

    @Override
    protected void createEntity() {
        createNewTag();
    }

    @Override
    protected TargetTag findEntityByName() {
        return tagManagement.findTargetTag(tagName.getValue());
    }

    /**
     * Create new tag.
     */
    @Override
    protected void createNewTag() {
        super.createNewTag();
        if (isNotEmpty(getTagNameValue())) {
            TargetTag newTargetTag = entityFactory.generateTargetTag(getTagNameValue());
            if (isNotEmpty(getTagDescValue())) {
                newTargetTag.setDescription(getTagDescValue());
            }
            newTargetTag.setColour(ColorPickerConstants.START_COLOR.getCSS());
            if (isNotEmpty(getColorPicked())) {
                newTargetTag.setColour(getColorPicked());
            }
            newTargetTag = tagManagement.createTargetTag(newTargetTag);
            displaySuccess(newTargetTag.getName());
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

    @Override
    protected String getWindowCaption() {
        return i18n.get("caption.add.tag");
    }

}
