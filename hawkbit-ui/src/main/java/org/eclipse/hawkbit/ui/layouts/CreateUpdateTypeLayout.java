/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.layouts;

import java.util.Optional;

import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.colorpicker.ColorPickerConstants;
import org.eclipse.hawkbit.ui.colorpicker.ColorPickerHelper;
import org.eclipse.hawkbit.ui.common.builder.LabelBuilder;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus.UIEventBus;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.server.Page;
import com.vaadin.shared.ui.colorpicker.Color;
import com.vaadin.ui.Button;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.components.colorpicker.ColorChangeEvent;
import com.vaadin.ui.components.colorpicker.ColorSelector;

/**
 * Superclass defining common properties and methods for creating/updating
 * types.
 */
public abstract class CreateUpdateTypeLayout<E extends NamedEntity> extends AbstractCreateUpdateTagLayout<E> {

    private static final long serialVersionUID = 5732904956185988397L;

    protected TextField typeKey;

    private static final String TYPE_NAME_DYNAMIC_STYLE = "new-tag-name";
    private static final String TYPE_DESC_DYNAMIC_STYLE = "new-tag-desc";

    public CreateUpdateTypeLayout(final VaadinMessageSource i18n, final EntityFactory entityFactory,
            final UIEventBus eventBus, final SpPermissionChecker permChecker, final UINotification uiNotification) {
        super(i18n, entityFactory, eventBus, permChecker, uiNotification);
    }

    @Override
    protected void addListeners() {
        super.addListeners();
        optiongroup.addValueChangeListener(this::optionValueChanged);
    }

    @Override
    protected void createRequiredComponents() {

        createTagStr = i18n.getMessage("label.create.type");
        updateTagStr = i18n.getMessage("label.update.type");
        comboLabel = new LabelBuilder().name(i18n.getMessage("label.choose.type")).buildLabel();
        colorLabel = new LabelBuilder().name(i18n.getMessage("label.choose.type.color")).buildLabel();
        colorLabel.addStyleName(SPUIDefinitions.COLOR_LABEL_STYLE);

        tagNameComboBox = SPUIComponentProvider.getComboBox(i18n.getMessage("label.combobox.type"), "", null, null,
                false, "", i18n.getMessage("label.combobox.type"));
        tagNameComboBox.setId(SPUIDefinitions.NEW_DISTRIBUTION_SET_TYPE_NAME_COMBO);
        tagNameComboBox.addStyleName(SPUIDefinitions.FILTER_TYPE_COMBO_STYLE);
        tagNameComboBox.setImmediate(true);
        tagNameComboBox.setPageLength(SPUIDefinitions.DIST_TYPE_SIZE);

        tagColorPreviewBtn = new Button();
        tagColorPreviewBtn.setId(UIComponentIdProvider.TAG_COLOR_PREVIEW_ID);
        getPreviewButtonColor(ColorPickerConstants.DEFAULT_COLOR);
        tagColorPreviewBtn.setStyleName(TAG_DYNAMIC_STYLE);

        createOptionGroup(permChecker.hasCreateRepositoryPermission(), permChecker.hasUpdateRepositoryPermission());
    }

    @Override
    protected void setColorToComponents(final Color newColor) {

        super.setColorToComponents(newColor);
        createDynamicStyleForComponents(tagName, typeKey, tagDesc, newColor.getCSS());
    }

    /**
     * Set tag name and desc field border color based on chosen color.
     *
     * @param tagName
     * @param tagDesc
     * @param taregtTagColor
     */
    private void createDynamicStyleForComponents(final TextField tagName, final TextField typeKey,
            final TextArea typeDesc, final String typeTagColor) {

        tagName.removeStyleName(SPUIDefinitions.TYPE_NAME);
        typeKey.removeStyleName(SPUIDefinitions.TYPE_KEY);
        typeDesc.removeStyleName(SPUIDefinitions.TYPE_DESC);
        getDynamicStyles(typeTagColor);
        tagName.addStyleName(TYPE_NAME_DYNAMIC_STYLE);
        typeKey.addStyleName(TYPE_NAME_DYNAMIC_STYLE);
        typeDesc.addStyleName(TYPE_DESC_DYNAMIC_STYLE);
    }

    /**
     * Get target style - Dynamically as per the color picked, cannot be done
     * from the static css.
     * 
     * @param colorPickedPreview
     */
    private void getDynamicStyles(final String colorPickedPreview) {

        Page.getCurrent().getJavaScript()
                .execute(HawkbitCommonUtil.changeToNewSelectedPreviewColor(colorPickedPreview));
    }

    /**
     * reset the components.
     */
    @Override
    protected void reset() {

        super.reset();
        typeKey.clear();
        restoreComponentStyles();
        setOptionGroupDefaultValue(permChecker.hasCreateRepositoryPermission(),
                permChecker.hasUpdateRepositoryPermission());
    }

    /**
     * Listener for option group - Create tag/Update.
     *
     * @param event
     *            ValueChangeEvent
     */
    @Override
    protected void optionValueChanged(final ValueChangeEvent event) {

        if (updateTagStr.equals(event.getProperty().getValue())) {
            tagName.clear();
            tagDesc.clear();
            typeKey.clear();
            typeKey.setEnabled(false);
            tagName.setEnabled(false);
            populateTagNameCombo();
            comboLayout.addComponent(comboLabel);
            comboLayout.addComponent(tagNameComboBox);
        } else {
            typeKey.setEnabled(true);
            tagName.setEnabled(true);
            tagName.clear();
            tagDesc.clear();
            typeKey.clear();
            comboLayout.removeComponent(comboLabel);
            comboLayout.removeComponent(tagNameComboBox);
        }
        restoreComponentStyles();
        getPreviewButtonColor(ColorPickerConstants.DEFAULT_COLOR);
        getColorPickerLayout().getSelPreview()
                .setColor(ColorPickerHelper.rgbToColorConverter(ColorPickerConstants.DEFAULT_COLOR));
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.vaadin.ui.components.colorpicker.ColorSelector#setColor(com.vaadin.
     * shared.ui.colorpicker .Color)
     */
    @Override
    public void setColor(final Color color) {

        if (color == null) {
            return;
        }
        getColorPickerLayout().setSelectedColor(color);
        getColorPickerLayout().getSelPreview().setColor(getColorPickerLayout().getSelectedColor());
        final String colorPickedPreview = getColorPickerLayout().getSelPreview().getColor().getCSS();
        if (tagName.isEnabled() && null != getColorPickerLayout().getColorSelect()) {
            createDynamicStyleForComponents(tagName, typeKey, tagDesc, colorPickedPreview);
            getColorPickerLayout().getColorSelect().setColor(getColorPickerLayout().getSelPreview().getColor());
        }
    }

    /**
     * reset the tag name and tag description component border color.
     */
    @Override
    protected void restoreComponentStyles() {
        super.restoreComponentStyles();
        typeKey.removeStyleName(TYPE_NAME_DYNAMIC_STYLE);
        typeKey.addStyleName(SPUIDefinitions.TYPE_KEY);
    }

    protected void setColorPickerComponentsColor(final String color) {

        if (null == color) {
            getColorPickerLayout()
                    .setSelectedColor(ColorPickerHelper.rgbToColorConverter(ColorPickerConstants.DEFAULT_COLOR));
            getColorPickerLayout().getSelPreview().setColor(getColorPickerLayout().getSelectedColor());
            getColorPickerLayout().getColorSelect().setColor(getColorPickerLayout().getSelectedColor());
            createDynamicStyleForComponents(tagName, typeKey, tagDesc, ColorPickerConstants.DEFAULT_COLOR);
            getPreviewButtonColor(ColorPickerConstants.DEFAULT_COLOR);
        } else {
            getColorPickerLayout().setSelectedColor(ColorPickerHelper.rgbToColorConverter(color));
            getColorPickerLayout().getSelPreview().setColor(getColorPickerLayout().getSelectedColor());
            getColorPickerLayout().getColorSelect().setColor(getColorPickerLayout().getSelectedColor());
            createDynamicStyleForComponents(tagName, typeKey, tagDesc, color);
            getPreviewButtonColor(color);
        }
    }

    @Override
    public void colorChanged(final ColorChangeEvent event) {
        setColor(event.getColor());
        for (final ColorSelector select : getColorPickerLayout().getSelectors()) {
            if (!event.getSource().equals(select) && select.equals(this)
                    && !select.getColor().equals(getColorPickerLayout().getSelectedColor())) {
                select.setColor(getColorPickerLayout().getSelectedColor());
            }
        }
        ColorPickerHelper.setRgbSliderValues(getColorPickerLayout());
        getPreviewButtonColor(event.getColor().getCSS());
        createDynamicStyleForComponents(tagName, typeKey, tagDesc, event.getColor().getCSS());
    }

    private boolean isDuplicateByKey() {
        final Optional<E> existingType = findEntityByKey();

        existingType.ifPresent(type -> uiNotification.displayValidationError(getDuplicateKeyErrorMessage(type)));

        return existingType.isPresent();
    }

    @Override
    protected boolean isDuplicate() {
        return isDuplicateByKey() || super.isDuplicate();
    }

    protected abstract Optional<E> findEntityByKey();

    protected abstract String getDuplicateKeyErrorMessage(E existingType);

    @Override
    protected void populateTagNameCombo() {
        // is implemented in the inherited class
    }

    @Override
    protected void setTagDetails(final String tagSelected) {
        // is implemented in the inherited class
    }

}
