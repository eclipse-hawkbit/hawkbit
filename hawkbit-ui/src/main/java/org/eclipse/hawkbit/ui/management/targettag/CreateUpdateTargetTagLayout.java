/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.targettag;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.hawkbit.eventbus.event.TargetTagCreatedBulkEvent;
import org.eclipse.hawkbit.eventbus.event.TargetTagDeletedEvent;
import org.eclipse.hawkbit.eventbus.event.TargetTagUpdateEvent;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.management.tag.CreateUpdateTagLayout;
import org.eclipse.hawkbit.ui.management.tag.SpColorPickerPreview;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.google.common.base.Strings;
import com.vaadin.shared.ui.colorpicker.Color;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;

/**
 *
 *
 */
@SpringComponent
@ViewScope
public class CreateUpdateTargetTagLayout extends CreateUpdateTagLayout {

    private static final long serialVersionUID = 2446682350481560235L;

    @Autowired
    private transient UINotification uiNotification;

    private Window targetTagWindow;

    @Override
    protected void createOptionGroup() {
        final List<String> optionValues = new ArrayList<>();
        if (permChecker.hasCreateTargetPermission()) {
            optionValues.add(createTag.getValue());
        }
        if (permChecker.hasUpdateTargetPermission()) {
            optionValues.add(updateTag.getValue());
        }

        createOptionGroup(optionValues);
    }

    private void createOptionGroup(final List<String> tagOptions) {
        optiongroup = new OptionGroup("", tagOptions);
        optiongroup.setCaption(null);
        optiongroup.addStyleName(ValoTheme.OPTIONGROUP_SMALL);
        optiongroup.addStyleName("custom-option-group");

        optiongroup.setNullSelectionAllowed(false);
        if (!tagOptions.isEmpty()) {
            optiongroup.select(tagOptions.get(0));
        }
    }

    public Window getWindow() {
        reset();
        targetTagWindow = SPUIComponentProvider.getWindow(i18n.get("caption.add.tag"), null,
                SPUIDefinitions.CREATE_UPDATE_WINDOW);
        targetTagWindow.setContent(this);
        return targetTagWindow;

    }

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
                selectedColor = new Color(44, 151, 32);
                selPreview.setColor(selectedColor);
                colorSelect.setColor(selectedColor);
                createDynamicStyleForComponents(tagName, tagDesc, DEFAULT_COLOR);
                getPreviewButtonColor(DEFAULT_COLOR);
            } else {
                selectedColor = rgbToColorConverter(selectedTargetTag.getColour());
                selPreview.setColor(selectedColor);
                colorSelect.setColor(selectedColor);
                createDynamicStyleForComponents(tagName, tagDesc, selectedTargetTag.getColour());
                getPreviewButtonColor(selectedTargetTag.getColour());
            }
        }
    }

    @Override
    public void save(final ClickEvent event) {
        if (mandatoryValuesPresent()) {
            final TargetTag existingTag = tagManagement.findTargetTag(tagName.getValue());
            if (optiongroup.getValue().equals(createTagNw)) {
                if (!checkIsDuplicate(existingTag)) {
                    createNewTag();
                }
            } else {

                updateTag(existingTag);
            }
        }
    }

    private Boolean checkIsDuplicate(final TargetTag existingTag) {
        if (existingTag != null) {
            uiNotification.displayValidationError(
                    i18n.get("message.tag.duplicate.check", new Object[] { existingTag.getName() }));
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    private Boolean mandatoryValuesPresent() {
        if (Strings.isNullOrEmpty(tagName.getValue())) {
            if (optiongroup.getValue().equals(createTagNw)) {
                uiNotification.displayValidationError(SPUILabelDefinitions.MISSING_TAG_NAME);
            }
            if (optiongroup.getValue().equals(updateTagNw)) {
                if (null == tagNameComboBox.getValue()) {
                    uiNotification.displayValidationError(i18n.get("message.error.missing.tagname"));
                } else {
                    uiNotification.displayValidationError(SPUILabelDefinitions.MISSING_TAG_NAME);
                }
            }
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }

    /**
     * Create new tag.
     */
    private void createNewTag() {
        final String colorPicked = getColorPickedString();
        final String tagNameValue = HawkbitCommonUtil.trimAndNullIfEmpty(tagName.getValue());
        final String tagDescValue = HawkbitCommonUtil.trimAndNullIfEmpty(tagDesc.getValue());
        if (null != tagNameValue) {
            TargetTag newTargetTag = new TargetTag(tagNameValue);
            if (null != tagDescValue) {
                newTargetTag.setDescription(tagDescValue);
            }
            newTargetTag.setColour(new Color(0, 146, 58).getCSS());
            if (colorPicked != null) {
                newTargetTag.setColour(colorPicked);
            }
            newTargetTag = tagManagement.createTargetTag(newTargetTag);
            uiNotification.displaySuccess(i18n.get("message.save.success", new Object[] { newTargetTag.getName() }));
            closeWindow();
        } else {
            uiNotification.displayValidationError(i18n.get("message.error.missing.tagname"));

        }
    }

    /**
     * update tag.
     */
    private void updateTag(final TargetTag targetObj) {
        final String nameUpdateValue = HawkbitCommonUtil.trimAndNullIfEmpty(tagName.getValue());
        final String descUpdateValue = HawkbitCommonUtil.trimAndNullIfEmpty(tagDesc.getValue());

        if (null != nameUpdateValue) {
            targetObj.setName(nameUpdateValue);
            targetObj.setDescription(null != descUpdateValue ? descUpdateValue : null);
            targetObj.setColour(getColorPickedString());
            tagManagement.updateTargetTag(targetObj);
            uiNotification.displaySuccess(i18n.get("message.update.success", new Object[] { targetObj.getName() }));
            closeWindow();
        } else {
            uiNotification.displayValidationError(i18n.get("message.tag.update.mandatory"));
        }

    }

    private void closeWindow() {
        targetTagWindow.close();
        UI.getCurrent().removeWindow(targetTagWindow);
    }

    /**
     * remove target tab window.
     * 
     * @param event
     */
    @Override
    public void discard(final ClickEvent event) {
        UI.getCurrent().removeWindow(targetTagWindow);
    }

    /**
     * Get color picked value in string.
     *
     * @return String of color picked value.
     */
    private String getColorPickedString() {
        return "rgb(" + getSelPreview().getColor().getRed() + "," + getSelPreview().getColor().getGreen() + ","
                + getSelPreview().getColor().getBlue() + ")";
    }

    /**
     * Color view.
     * 
     * @return ColorPickerPreview as UI
     */
    public SpColorPickerPreview getSelPreview() {
        return selPreview;
    }

}
