/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.dstag;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.eventbus.event.DistributionSetTagCreatedBulkEvent;
import org.eclipse.hawkbit.repository.eventbus.event.DistributionSetTagDeletedEvent;
import org.eclipse.hawkbit.repository.eventbus.event.DistributionSetTagUpdateEvent;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
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
import com.vaadin.spring.annotation.VaadinSessionScope;
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
@VaadinSessionScope
public class CreateUpdateDistributionTagLayoutWindow extends CreateUpdateTagLayout {
    private static final long serialVersionUID = 444276149954167545L;

    @Autowired
    private transient UINotification uiNotification;

    @Autowired
    private transient EntityFactory entityFactory;

    private static final String MISSING_TAG_NAME = "message.error.missing.tagname";
    private static final String TARGET_TAG_NAME_DYNAMIC_STYLE = "new-target-tag-name";
    private static final String MSG_TEXTFIELD_NAME = "textfield.name";

    private Window distTagWindow;

    public Window getWindow() {
        reset();
        distTagWindow = SPUIComponentProvider.getWindow(i18n.get("caption.add.tag"), null,
                SPUIDefinitions.CREATE_UPDATE_WINDOW);
        distTagWindow.setContent(this);
        return distTagWindow;

    }

    @Override
    protected void populateTagNameCombo() {
        tagNameComboBox.removeAllItems();
        final List<DistributionSetTag> distTagNameList = tagManagement.findAllDistributionSetTags();
        distTagNameList.forEach(value -> tagNameComboBox.addItem(value.getName()));
    }

    /**
     * Update DistributionTag.
     */
    @Override
    public void save(final ClickEvent event) {
        if (mandatoryValuesPresent()) {
            final DistributionSetTag existingDistTag = tagManagement.findDistributionSetTag(tagName.getValue());
            if (optiongroup.getValue().equals(createTagNw)) {
                if (!checkIsDuplicate(existingDistTag)) {
                    crateNewTag();
                }
            } else {

                updateTag(existingDistTag);
            }
        }
    }

    /**
     * Create new tag.
     */
    private void crateNewTag() {

        final String colorPicked = getColorPickedSting();
        final String tagNameValue = HawkbitCommonUtil.trimAndNullIfEmpty(tagName.getValue());
        final String tagDescValue = HawkbitCommonUtil.trimAndNullIfEmpty(tagDesc.getValue());

        if (null != tagNameValue) {
            DistributionSetTag newDistTag = entityFactory.generateDistributionSetTag(tagNameValue, tagDescValue,
                    new Color(0, 146, 58).getCSS());

            if (colorPicked != null) {
                newDistTag.setColour(colorPicked);
            }

            newDistTag = tagManagement.createDistributionSetTag(newDistTag);
            uiNotification.displaySuccess(i18n.get("message.save.success", new Object[] { newDistTag.getName() }));
            resetDistTagValues();

        } else {
            uiNotification.displayValidationError(i18n.get(MISSING_TAG_NAME));

        }

    }

    /**
     * update tag.
     */
    private void updateTag(final DistributionSetTag distObj) {
        final String nameUpdateValue = HawkbitCommonUtil.trimAndNullIfEmpty(tagName.getValue());
        final String descUpdateValue = HawkbitCommonUtil.trimAndNullIfEmpty(tagDesc.getValue());

        if (null != nameUpdateValue) {
            distObj.setName(nameUpdateValue);
            distObj.setDescription(null != descUpdateValue ? descUpdateValue : null);
            // update target tag with color selected
            distObj.setColour(getColorPickedSting());
            tagManagement.updateDistributionSetTag(distObj);
            uiNotification.displaySuccess(i18n.get("message.update.success", new Object[] { distObj.getName() }));
            closeWindow();
        } else {
            uiNotification.displayValidationError(i18n.get("message.tag.update.mandatory"));
        }

    }

    private void closeWindow() {
        distTagWindow.close();
        UI.getCurrent().removeWindow(distTagWindow);
    }

    /**
     * RESET.
     * 
     * @param event
     */
    @Override
    public void discard(final ClickEvent event) {
        distTagWindow.setVisible(false);
        UI.getCurrent().removeWindow(distTagWindow);
        resetDistTagValues();
    }

    /**
     * Get color picked value in string.
     *
     * @return String of color picked value.
     */
    private String getColorPickedSting() {
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

    /**
     * RESET.
     */
    private void resetDistTagValues() {
        tagName.removeStyleName(TARGET_TAG_NAME_DYNAMIC_STYLE);
        tagName.addStyleName(SPUIDefinitions.NEW_TARGET_TAG_NAME);
        tagName.setValue("");
        tagName.setInputPrompt(i18n.get(MSG_TEXTFIELD_NAME));
        setColor(new Color(0, 146, 58));
        distTagWindow.setVisible(false);
        tagPreviewBtnClicked = false;
        UI.getCurrent().removeWindow(distTagWindow);
    }

    /*
     * (non-Javadoc)
     * 
     * @see hawkbit.server.ui.management.CreateUpdateTagLayout#createOptionGroup
     * ()
     */
    @Override
    protected void createOptionGroup() {
        final List<String> optionValues = new ArrayList<>();
        if (permChecker.hasCreateDistributionPermission()) {
            optionValues.add(createTag.getValue());
        }
        if (permChecker.hasUpdateDistributionPermission()) {
            optionValues.add(updateTag.getValue());
        }

        createOptionGroup(optionValues);
    }

    private void createOptionGroup(final List<String> tagOptions) {
        optiongroup = new OptionGroup("", tagOptions);
        optiongroup.addStyleName(ValoTheme.OPTIONGROUP_SMALL);
        optiongroup.addStyleName("custom-option-group");
        optiongroup.setNullSelectionAllowed(false);
        if (!tagOptions.isEmpty()) {
            optiongroup.select(tagOptions.get(0));
        }
    }

    /**
     * 
     * @return
     */
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
     * Select tag & set tag name & tag desc values corresponding to selected
     * tag.
     * 
     * @param targetTagSelected
     *            as the selected tag from combo
     */
    @Override
    public void setTagDetails(final String distTagSelected) {
        tagName.setValue(distTagSelected);
        final DistributionSetTag selectedDistTag = tagManagement.findDistributionSetTag(distTagSelected);
        if (null != selectedDistTag) {
            tagDesc.setValue(selectedDistTag.getDescription());
            if (null == selectedDistTag.getColour()) {
                selectedColor = new Color(44, 151, 32);
                selPreview.setColor(selectedColor);
                colorSelect.setColor(selectedColor);
                createDynamicStyleForComponents(tagName, tagDesc, DEFAULT_COLOR);
                getPreviewButtonColor(DEFAULT_COLOR);
            } else {
                selectedColor = rgbToColorConverter(selectedDistTag.getColour());
                selPreview.setColor(selectedColor);
                colorSelect.setColor(selectedColor);
                createDynamicStyleForComponents(tagName, tagDesc, selectedDistTag.getColour());
                getPreviewButtonColor(selectedDistTag.getColour());
            }
        }
    }

    /**
     * Checking Tag already existed or not.
     * 
     * @param existingDistTag
     * @return
     */

    private Boolean checkIsDuplicate(final DistributionSetTag existingDistTag) {
        if (existingDistTag != null) {
            uiNotification.displayValidationError(
                    i18n.get("message.tag.duplicate.check", new Object[] { existingDistTag.getName() }));
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

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

}
