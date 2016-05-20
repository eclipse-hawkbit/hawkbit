package org.eclipse.hawkbit.ui.layouts;

import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.ui.colorPicker.ColorPickerConstants;
import org.eclipse.hawkbit.ui.colorPicker.ColorPickerHelper;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.server.Page;
import com.vaadin.shared.ui.colorpicker.Color;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.components.colorpicker.ColorChangeEvent;
import com.vaadin.ui.components.colorpicker.ColorSelector;
import com.vaadin.ui.themes.ValoTheme;

public class CreateUpdateTypeLayout extends CreateUpdateTagLayout {

    private static final long serialVersionUID = 5732904956185988397L;

    protected String createTypeStr;
    protected String updateTypeStr;
    protected TextField typeKey;

    public static final String TYPE_NAME_DYNAMIC_STYLE = "new-tag-name";
    private static final String TYPE_DESC_DYNAMIC_STYLE = "new-tag-desc";

    /**
     * Value change listeners implementations of sliders.
     */
    protected void slidersValueChangeListeners() {
        getColorPickerLayout().getRedSlider().addValueChangeListener(new ValueChangeListener() {
            private static final long serialVersionUID = -8336732883300920839L;

            @Override
            public void valueChange(final ValueChangeEvent event) {
                final double red = (Double) event.getProperty().getValue();
                final Color newColor = new Color((int) red, getColorPickerLayout().getSelectedColor().getGreen(),
                        getColorPickerLayout().getSelectedColor().getBlue());
                setColorToComponents(newColor);
            }
        });
        getColorPickerLayout().getGreenSlider().addValueChangeListener(new ValueChangeListener() {
            private static final long serialVersionUID = 1236358037711775663L;

            @Override
            public void valueChange(final ValueChangeEvent event) {
                final double green = (Double) event.getProperty().getValue();
                final Color newColor = new Color(getColorPickerLayout().getSelectedColor().getRed(), (int) green,
                        getColorPickerLayout().getSelectedColor().getBlue());
                setColorToComponents(newColor);
            }
        });
        getColorPickerLayout().getBlueSlider().addValueChangeListener(new ValueChangeListener() {
            private static final long serialVersionUID = 8466370744686043947L;

            @Override
            public void valueChange(final ValueChangeEvent event) {
                final double blue = (Double) event.getProperty().getValue();
                final Color newColor = new Color(getColorPickerLayout().getSelectedColor().getRed(),
                        getColorPickerLayout().getSelectedColor().getGreen(), (int) blue);
                setColorToComponents(newColor);
            }
        });
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
    protected void createDynamicStyleForComponents(final TextField tagName, final TextField typeKey,
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
    }

    /**
     * Listener for option group - Create tag/Update.
     *
     * @param event
     *            ValueChangeEvent
     */
    protected void createOptionValueChanged(final ValueChangeEvent event) {

        if ("Update Type".equals(event.getProperty().getValue())) {
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
            window.setSaveButtonEnabled(true);
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
    }

    /**
     * create option group with Create tag/Update tag based on permissions.
     */
    @Override
    protected void createOptionGroup(final boolean hasCreatePermission, final boolean hasUpdatePermission) {

        optiongroup = new OptionGroup("Select Action");
        optiongroup.addStyleName(ValoTheme.OPTIONGROUP_SMALL);
        optiongroup.addStyleName("custom-option-group");
        optiongroup.setNullSelectionAllowed(false);

        if (hasCreatePermission) {
            optiongroup.addItem(createTypeStr);
            optiongroup.select(createTypeStr);
        }
        if (hasUpdatePermission) {
            optiongroup.addItem(updateTypeStr);
            if (!hasCreatePermission) {
                optiongroup.select(updateTypeStr);
            }
        }
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

    protected Boolean checkIsDuplicate(final NamedEntity existingType) {

        if (existingType != null) {
            uiNotification.displayValidationError(
                    i18n.get("message.tag.duplicate.check", new Object[] { existingType.getName() }));
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    protected Boolean checkIsDuplicateByKey(final NamedEntity existingType) {

        if (existingType != null) {
            if (existingType instanceof DistributionSetType) {
                uiNotification.displayValidationError(i18n.get("message.type.key.duplicate.check",
                        new Object[] { ((DistributionSetType) existingType).getKey() }));
                return Boolean.TRUE;
            } else if (existingType instanceof SoftwareModuleType) {
                uiNotification.displayValidationError(i18n.get("message.type.key.swmodule.duplicate.check",
                        new Object[] { ((SoftwareModuleType) existingType).getKey() }));
                return Boolean.TRUE;
            }
        }
        return Boolean.FALSE;
    }

    @Override
    protected void save(final ClickEvent event) {
        // TODO Auto-generated method stub

    }

    @Override
    protected void populateTagNameCombo() {
        // TODO Auto-generated method stub

    }

    @Override
    protected void setTagDetails(final String tagSelected) {
        // TODO Auto-generated method stub

    }

}
