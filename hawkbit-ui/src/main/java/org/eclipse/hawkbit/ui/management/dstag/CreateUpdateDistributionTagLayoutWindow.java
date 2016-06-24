/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.dstag;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.util.List;

import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.eventbus.event.DistributionSetTagCreatedBulkEvent;
import org.eclipse.hawkbit.repository.eventbus.event.DistributionSetTagDeletedEvent;
import org.eclipse.hawkbit.repository.eventbus.event.DistributionSetTagUpdateEvent;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.ui.colorpicker.ColorPickerConstants;
import org.eclipse.hawkbit.ui.colorpicker.ColorPickerHelper;
import org.eclipse.hawkbit.ui.layouts.AbstractCreateUpdateTagLayout;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.UI;

/**
 * Class for Create/Update Tag Layout of distribution set
 */
@SpringComponent
@ViewScope
public class CreateUpdateDistributionTagLayoutWindow extends AbstractCreateUpdateTagLayout {

    private static final long serialVersionUID = 444276149954167545L;

    @Autowired
    private transient EntityFactory entityFactory;

    private static final String TARGET_TAG_NAME_DYNAMIC_STYLE = "new-target-tag-name";
    private static final String MSG_TEXTFIELD_NAME = "textfield.name";

    @EventBusListenerMethod(scope = EventScope.SESSION)
    // Exception squid:S1172 - event not needed
    @SuppressWarnings({ "squid:S1172" })
    void onDistributionSetTagCreatedBulkEvent(final DistributionSetTagCreatedBulkEvent event) {
        populateTagNameCombo();
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    // Exception squid:S1172 - event not needed
    @SuppressWarnings({ "squid:S1172" })
    void onDistributionSetTagDeletedEvent(final DistributionSetTagDeletedEvent event) {
        populateTagNameCombo();
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    // Exception squid:S1172 - event not needed
    @SuppressWarnings({ "squid:S1172" })
    void onDistributionSetTagUpdateEvent(final DistributionSetTagUpdateEvent event) {
        populateTagNameCombo();
    }

    @Override
    protected void populateTagNameCombo() {
        tagNameComboBox.removeAllItems();
        final List<DistributionSetTag> distTagNameList = tagManagement.findAllDistributionSetTags();
        distTagNameList.forEach(value -> tagNameComboBox.addItem(value.getName()));
    }

    @Override
    protected void addListeners() {
        super.addListeners();
        optiongroup.addValueChangeListener(this::optionValueChanged);
    }

    /**
     * Update DistributionTag.
     */
    @Override
    public void save(final ClickEvent event) {
        if (mandatoryValuesPresent()) {
            final DistributionSetTag existingDistTag = tagManagement.findDistributionSetTag(tagName.getValue());
            if (optiongroup.getValue().equals(createTagStr)) {
                if (!checkIsDuplicate(existingDistTag)) {
                    createNewTag();
                }
            } else {

                updateExistingTag(existingDistTag);
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
            DistributionSetTag newDistTag = entityFactory.generateDistributionSetTag(tagNameValue, tagDescValue,
                    ColorPickerConstants.START_COLOR.getCSS());

            if (isNotEmpty(getColorPicked())) {
                newDistTag.setColour(getColorPicked());
            }
            newDistTag = tagManagement.createDistributionSetTag(newDistTag);
            displaySuccess(newDistTag.getName());
            resetDistTagValues();
        } else {
            displayValidationError(i18n.get(SPUILabelDefinitions.MISSING_TAG_NAME));
        }
    }

    /**
     * RESET.
     * 
     * @param event
     */
    @Override
    public void discard(final ClickEvent event) {
        super.discard(event);
        resetDistTagValues();
    }

    /**
     * RESET.
     */
    private void resetDistTagValues() {
        tagName.removeStyleName(TARGET_TAG_NAME_DYNAMIC_STYLE);
        tagName.addStyleName(SPUIDefinitions.NEW_TARGET_TAG_NAME);
        tagName.setValue("");
        tagName.setInputPrompt(i18n.get(MSG_TEXTFIELD_NAME));
        setColor(ColorPickerConstants.START_COLOR);
        getWindow().setVisible(false);
        tagPreviewBtnClicked = false;
        UI.getCurrent().removeWindow(getWindow());
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
        setOriginalTagName(distTagSelected);
        final DistributionSetTag selectedDistTag = tagManagement.findDistributionSetTag(distTagSelected);
        if (null != selectedDistTag) {
            tagDesc.setValue(selectedDistTag.getDescription());
            setOriginalTagDesc(selectedDistTag.getDescription());
            if (null == selectedDistTag.getColour()) {
                setTagColor(getColorPickerLayout().getDefaultColor(), ColorPickerConstants.DEFAULT_COLOR);
                setSelectedColorOriginal(getColorPickerLayout().getDefaultColor());
            } else {
                setTagColor(ColorPickerHelper.rgbToColorConverter(selectedDistTag.getColour()),
                        selectedDistTag.getColour());
                setSelectedColorOriginal(ColorPickerHelper.rgbToColorConverter(selectedDistTag.getColour()));
            }
        }
    }

    @Override
    protected void createRequiredComponents() {
        super.createRequiredComponents();
        createOptionGroup(permChecker.hasCreateDistributionPermission(), permChecker.hasUpdateDistributionPermission());
    }

    @Override
    protected void reset() {

        super.reset();
        setOptionGroupDefaultValue(permChecker.hasCreateDistributionPermission(),
                permChecker.hasUpdateDistributionPermission());
    }

}
