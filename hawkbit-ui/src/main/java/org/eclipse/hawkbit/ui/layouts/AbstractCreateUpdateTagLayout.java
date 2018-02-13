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
import org.eclipse.hawkbit.ui.colorpicker.ColorPickerLayout;
import org.eclipse.hawkbit.ui.common.CommonDialogWindow;
import org.eclipse.hawkbit.ui.common.CommonDialogWindow.SaveDialogCloseListener;
import org.eclipse.hawkbit.ui.common.EmptyStringValidator;
import org.eclipse.hawkbit.ui.common.builder.LabelBuilder;
import org.eclipse.hawkbit.ui.common.builder.TextAreaBuilder;
import org.eclipse.hawkbit.ui.common.builder.TextFieldBuilder;
import org.eclipse.hawkbit.ui.common.builder.WindowBuilder;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.EventBus.UIEventBus;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.server.Page;
import com.vaadin.shared.ui.colorpicker.Color;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.components.colorpicker.ColorChangeEvent;
import com.vaadin.ui.components.colorpicker.ColorChangeListener;
import com.vaadin.ui.components.colorpicker.ColorSelector;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Abstract class for create/update target tag layout.
 *
 * @param <E>
 */
public abstract class AbstractCreateUpdateTagLayout<E extends NamedEntity> extends CustomComponent
        implements ColorChangeListener, ColorSelector {

    private static final long serialVersionUID = 4229177824620576456L;
    private static final String TAG_NAME_DYNAMIC_STYLE = "new-tag-name";
    private static final String TAG_DESC_DYNAMIC_STYLE = "new-tag-desc";
    protected static final String TAG_DYNAMIC_STYLE = "tag-color-preview";
    protected static final String MESSAGE_ERROR_MISSING_TAGNAME = "message.error.missing.tagname";

    protected static final int MAX_TAGS = 500;

    protected VaadinMessageSource i18n;

    protected transient EntityFactory entityFactory;

    protected transient EventBus.UIEventBus eventBus;

    protected SpPermissionChecker permChecker;

    protected UINotification uiNotification;

    private final FormLayout formLayout = new FormLayout();

    protected String createTagStr;
    protected String updateTagStr;
    protected Label comboLabel;
    protected CommonDialogWindow window;

    protected Label colorLabel;
    protected TextField tagName;
    protected TextArea tagDesc;
    protected Button tagColorPreviewBtn;
    protected OptionGroup optiongroup;
    protected ComboBox tagNameComboBox;

    protected VerticalLayout comboLayout;
    protected ColorPickerLayout colorPickerLayout;
    protected GridLayout mainLayout;
    protected VerticalLayout contentLayout;

    protected boolean tagPreviewBtnClicked;

    private String colorPicked;
    protected String tagNameValue;
    protected String tagDescValue;

    /**
     * Constructor for AbstractCreateUpdateTagLayout
     * 
     * @param i18n
     *            I18N
     * @param tagManagement
     *            TagManagement
     * @param entityFactory
     *            EntityFactory
     * @param eventBus
     *            UIEventBus
     * @param permChecker
     *            SpPermissionChecker
     * @param uiNotification
     *            UINotification
     */
    public AbstractCreateUpdateTagLayout(final VaadinMessageSource i18n, final EntityFactory entityFactory,
            final UIEventBus eventBus, final SpPermissionChecker permChecker, final UINotification uiNotification) {
        this.i18n = i18n;
        this.entityFactory = entityFactory;
        this.eventBus = eventBus;
        this.permChecker = permChecker;
        this.uiNotification = uiNotification;
    }

    /**
     * 
     * Save or update the entity.
     */
    private final class SaveOnDialogCloseListener implements SaveDialogCloseListener {

        @Override
        public void saveOrUpdate() {
            if (isUpdateAction()) {
                updateEntity(findEntityByName().orElse(null));
                return;
            }

            createEntity();
        }

        @Override
        public boolean canWindowSaveOrUpdate() {
            return isUpdateAction() || !isDuplicate();

        }

        private boolean isUpdateAction() {
            return !optiongroup.getValue().equals(createTagStr);
        }
    }

    /**
     * Discard the changes and close the popup.
     */
    protected void discard() {
        UI.getCurrent().removeWindow(window);
    }

    /**
     * Populate target name combo.
     */
    protected abstract void populateTagNameCombo();

    protected abstract void setTagDetails(final String tagSelected);

    /**
     * Init the layout.
     */
    public void init() {

        setSizeUndefined();
        createRequiredComponents();
        buildLayout();
        addListeners();
        eventBus.subscribe(this);
    }

    protected void createRequiredComponents() {

        createTagStr = i18n.getMessage("label.create.tag");
        updateTagStr = i18n.getMessage("label.update.tag");
        comboLabel = new LabelBuilder().name(i18n.getMessage("label.choose.tag")).buildLabel();
        colorLabel = new LabelBuilder().name(i18n.getMessage("label.choose.tag.color")).buildLabel();
        colorLabel.addStyleName(SPUIDefinitions.COLOR_LABEL_STYLE);

        tagName = new TextFieldBuilder().caption(i18n.getMessage("textfield.name"))
                .styleName(ValoTheme.TEXTFIELD_TINY + " " + SPUIDefinitions.TAG_NAME).required(true)
                .prompt(i18n.getMessage("textfield.name")).immediate(true).id(SPUIDefinitions.NEW_TARGET_TAG_NAME)
                .validator(new EmptyStringValidator(i18n)).buildTextComponent();

        tagDesc = new TextAreaBuilder().caption(i18n.getMessage("textfield.description"))
                .styleName(ValoTheme.TEXTFIELD_TINY + " " + SPUIDefinitions.TAG_DESC)
                .prompt(i18n.getMessage("textfield.description")).immediate(true)
                .id(SPUIDefinitions.NEW_TARGET_TAG_DESC).buildTextComponent();

        tagDesc.setNullRepresentation("");

        tagNameComboBox = SPUIComponentProvider.getComboBox(null, "", null, null, false, "",
                i18n.getMessage("label.combobox.tag"));
        tagNameComboBox.addStyleName(SPUIDefinitions.FILTER_TYPE_COMBO_STYLE);
        tagNameComboBox.setImmediate(true);
        tagNameComboBox.setId(UIComponentIdProvider.DIST_TAG_COMBO);

        tagColorPreviewBtn = new Button();
        tagColorPreviewBtn.setId(UIComponentIdProvider.TAG_COLOR_PREVIEW_ID);
        getPreviewButtonColor(ColorPickerConstants.DEFAULT_COLOR);
        tagColorPreviewBtn.setStyleName(TAG_DYNAMIC_STYLE);
    }

    protected void buildLayout() {

        mainLayout = new GridLayout(3, 2);
        mainLayout.setSpacing(true);
        comboLayout = new VerticalLayout();
        colorPickerLayout = new ColorPickerLayout();
        ColorPickerHelper.setRgbSliderValues(colorPickerLayout);
        contentLayout = new VerticalLayout();

        final HorizontalLayout colorLabelLayout = new HorizontalLayout();
        colorLabelLayout.setMargin(false);
        colorLabelLayout.addComponents(colorLabel, tagColorPreviewBtn);

        formLayout.addComponent(optiongroup);
        formLayout.addComponent(comboLayout);
        formLayout.addComponent(tagName);
        formLayout.addComponent(tagDesc);
        formLayout.addStyleName("form-lastrow");
        formLayout.setSizeFull();

        contentLayout.addComponent(formLayout);
        contentLayout.addComponent(colorLabelLayout);
        contentLayout.setComponentAlignment(formLayout, Alignment.MIDDLE_CENTER);
        contentLayout.setComponentAlignment(colorLabelLayout, Alignment.MIDDLE_LEFT);
        contentLayout.setSizeUndefined();

        mainLayout.setSizeFull();
        mainLayout.addComponent(contentLayout, 0, 0);

        colorPickerLayout.setVisible(false);
        mainLayout.addComponent(colorPickerLayout, 1, 0);
        mainLayout.setComponentAlignment(colorPickerLayout, Alignment.MIDDLE_CENTER);

        setCompositionRoot(mainLayout);
        tagName.focus();
    }

    protected void addListeners() {
        colorPickerLayout.getColorSelect().addColorChangeListener(this);
        colorPickerLayout.getSelPreview().addColorChangeListener(this);
        tagColorPreviewBtn.addClickListener(event -> previewButtonClicked());
        tagNameComboBox.addValueChangeListener(this::tagNameChosen);
        slidersValueChangeListeners();
    }

    /**
     * Open color picker on click of preview button. Auto select the color based
     * on target tag if already selected.
     */
    private void previewButtonClicked() {
        if (!tagPreviewBtnClicked) {
            final String selectedOption = (String) optiongroup.getValue();
            if (selectedOption == null) {
                return;
            }

            if (tagNameComboBox.getValue() == null) {
                colorPickerLayout
                        .setSelectedColor(ColorPickerHelper.rgbToColorConverter(ColorPickerConstants.DEFAULT_COLOR));
            } else {
                colorPickerLayout.setSelectedColor(getColorForColorPicker());
            }
        }

        tagPreviewBtnClicked = !tagPreviewBtnClicked;
        colorPickerLayout.setVisible(tagPreviewBtnClicked);
    }

    protected abstract Color getColorForColorPicker();

    private void tagNameChosen(final ValueChangeEvent event) {
        final String tagSelected = (String) event.getProperty().getValue();
        if (null != tagSelected) {
            setTagDetails(tagSelected);
        } else {
            resetTagNameField();
        }
        window.setOrginaleValues();
    }

    protected void resetTagNameField() {
        tagName.setEnabled(false);
        tagName.clear();
        tagDesc.clear();
        restoreComponentStyles();
        colorPickerLayout.setSelectedColor(colorPickerLayout.getDefaultColor());
        colorPickerLayout.getSelPreview().setColor(colorPickerLayout.getSelectedColor());
        tagPreviewBtnClicked = false;
    }

    /**
     * Listener for option group - Create tag/Update.
     *
     * @param event
     *            ValueChangeEvent
     */
    protected void optionValueChanged(final ValueChangeEvent event) {

        if (updateTagStr.equals(event.getProperty().getValue())) {
            tagName.clear();
            tagDesc.clear();
            tagName.setEnabled(false);
            populateTagNameCombo();
            // show target name combo
            comboLayout.addComponent(comboLabel);
            comboLayout.addComponent(tagNameComboBox);
        } else {
            tagName.setEnabled(true);
            tagName.clear();
            tagDesc.clear();
            // hide target name combo
            comboLayout.removeComponent(comboLabel);
            comboLayout.removeComponent(tagNameComboBox);
        }
        // close the color picker layout
        tagPreviewBtnClicked = false;
        // reset the selected color - Set default color
        restoreComponentStyles();
        getPreviewButtonColor(ColorPickerConstants.DEFAULT_COLOR);
        colorPickerLayout.getSelPreview()
                .setColor(ColorPickerHelper.rgbToColorConverter(ColorPickerConstants.DEFAULT_COLOR));
        window.setOrginaleValues();
    }

    /**
     * reset the components.
     */
    protected void reset() {
        tagName.setEnabled(true);
        tagName.clear();
        tagDesc.clear();
        restoreComponentStyles();

        // hide target name combo
        comboLayout.removeComponent(comboLabel);
        comboLayout.removeComponent(tagNameComboBox);

        // Default green color
        colorPickerLayout.setVisible(false);
        colorPickerLayout.setSelectedColor(colorPickerLayout.getDefaultColor());
        colorPickerLayout.getSelPreview().setColor(colorPickerLayout.getSelectedColor());
        tagPreviewBtnClicked = false;
    }

    /**
     * On change of color in color picker ,change RGB sliders, components border
     * color and color of preview button.
     */
    @Override
    public void colorChanged(final ColorChangeEvent event) {
        setColor(event.getColor());
        for (final ColorSelector select : colorPickerLayout.getSelectors()) {
            if (!event.getSource().equals(select) && select.equals(this)
                    && !select.getColor().equals(colorPickerLayout.getSelectedColor())) {
                select.setColor(colorPickerLayout.getSelectedColor());
            }
        }
        ColorPickerHelper.setRgbSliderValues(colorPickerLayout);
        getPreviewButtonColor(event.getColor().getCSS());
        createDynamicStyleForComponents(tagName, tagDesc, event.getColor().getCSS());
    }

    /**
     * Dynamic styles for window.
     *
     * @param top
     *            int value
     * @param marginLeft
     *            int value
     */
    protected void getPreviewButtonColor(final String color) {
        Page.getCurrent().getJavaScript().execute(HawkbitCommonUtil.getPreviewButtonColorScript(color));
    }

    /**
     * Set tag name and desc field border color based on chosen color.
     *
     * @param tagName
     * @param tagDesc
     * @param taregtTagColor
     */
    protected void createDynamicStyleForComponents(final TextField tagName, final TextArea tagDesc,
            final String taregtTagColor) {

        tagName.removeStyleName(SPUIDefinitions.TAG_NAME);
        tagDesc.removeStyleName(SPUIDefinitions.TAG_DESC);
        getTargetDynamicStyles(taregtTagColor);
        tagName.addStyleName(TAG_NAME_DYNAMIC_STYLE);
        tagDesc.addStyleName(TAG_DESC_DYNAMIC_STYLE);
    }

    /**
     * reset the tag name and tag description component border color.
     */
    protected void restoreComponentStyles() {
        tagName.removeStyleName(TAG_NAME_DYNAMIC_STYLE);
        tagDesc.removeStyleName(TAG_DESC_DYNAMIC_STYLE);
        tagName.addStyleName(SPUIDefinitions.TAG_NAME);
        tagDesc.addStyleName(SPUIDefinitions.TAG_DESC);
        getPreviewButtonColor(ColorPickerConstants.DEFAULT_COLOR);
    }

    /**
     * Get target style - Dynamically as per the color picked, cannot be done
     * from the static css.
     *
     * @param colorPickedPreview
     */
    private static void getTargetDynamicStyles(final String colorPickedPreview) {
        Page.getCurrent().getJavaScript()
                .execute(HawkbitCommonUtil.changeToNewSelectedPreviewColor(colorPickedPreview));
    }

    @Override
    public Color getColor() {
        return null;
    }

    @Override
    public void setColor(final Color color) {
        if (color == null) {
            return;
        }
        colorPickerLayout.setSelectedColor(color);
        colorPickerLayout.getSelPreview().setColor(colorPickerLayout.getSelectedColor());
        final String colorPickedPreview = colorPickerLayout.getSelPreview().getColor().getCSS();
        if (tagName.isEnabled() && null != colorPickerLayout.getColorSelect()) {
            createDynamicStyleForComponents(tagName, tagDesc, colorPickedPreview);
            colorPickerLayout.getColorSelect().setColor(colorPickerLayout.getSelPreview().getColor());
        }

    }

    /**
     * create option group with Create tag/Update tag based on permissions.
     */
    protected void createOptionGroup(final boolean hasCreatePermission, final boolean hasUpdatePermission) {

        optiongroup = new OptionGroup("Select Action");
        optiongroup.setId(UIComponentIdProvider.OPTION_GROUP);
        optiongroup.addStyleName(ValoTheme.OPTIONGROUP_SMALL);
        optiongroup.addStyleName("custom-option-group");
        optiongroup.setNullSelectionAllowed(false);

        if (hasCreatePermission) {
            optiongroup.addItem(createTagStr);
        }
        if (hasUpdatePermission) {
            optiongroup.addItem(updateTagStr);
        }

        setOptionGroupDefaultValue(hasCreatePermission, hasUpdatePermission);
    }

    protected void setOptionGroupDefaultValue(final boolean hasCreatePermission, final boolean hasUpdatePermission) {

        if (hasCreatePermission) {
            optiongroup.select(createTagStr);
        }
        if (hasUpdatePermission && !hasCreatePermission) {
            optiongroup.select(updateTagStr);
        }
    }

    public ColorPickerLayout getColorPickerLayout() {
        return colorPickerLayout;
    }

    public CommonDialogWindow getWindow() {
        reset();
        window = new WindowBuilder(SPUIDefinitions.CREATE_UPDATE_WINDOW).caption(getWindowCaption()).content(this)
                .cancelButtonClickListener(event -> discard()).layout(mainLayout).i18n(i18n)
                .saveDialogCloseListener(new SaveOnDialogCloseListener()).buildCommonDialogWindow();
        return window;
    }

    /**
     * Value change listeners implementations of sliders.
     */
    private void slidersValueChangeListeners() {
        colorPickerLayout.getRedSlider().addValueChangeListener(new ValueChangeListener() {
            private static final long serialVersionUID = -8336732888800920839L;

            @Override
            public void valueChange(final ValueChangeEvent event) {
                final double red = (Double) event.getProperty().getValue();
                final Color newColor = new Color((int) red, colorPickerLayout.getSelectedColor().getGreen(),
                        colorPickerLayout.getSelectedColor().getBlue());
                setColorToComponents(newColor);
            }
        });
        colorPickerLayout.getGreenSlider().addValueChangeListener(new ValueChangeListener() {
            private static final long serialVersionUID = 1236358037766775663L;

            @Override
            public void valueChange(final ValueChangeEvent event) {
                final double green = (Double) event.getProperty().getValue();
                final Color newColor = new Color(colorPickerLayout.getSelectedColor().getRed(), (int) green,
                        colorPickerLayout.getSelectedColor().getBlue());
                setColorToComponents(newColor);
            }
        });
        colorPickerLayout.getBlueSlider().addValueChangeListener(new ValueChangeListener() {
            private static final long serialVersionUID = 8466370763686043947L;

            @Override
            public void valueChange(final ValueChangeEvent event) {
                final double blue = (Double) event.getProperty().getValue();
                final Color newColor = new Color(colorPickerLayout.getSelectedColor().getRed(),
                        colorPickerLayout.getSelectedColor().getGreen(), (int) blue);
                setColorToComponents(newColor);
            }
        });
    }

    protected void setColorToComponents(final Color newColor) {
        setColor(newColor);
        colorPickerLayout.getColorSelect().setColor(newColor);
        getPreviewButtonColor(newColor.getCSS());
        createDynamicStyleForComponents(tagName, tagDesc, newColor.getCSS());
    }

    /**
     * Create new tag.
     */
    protected void createNewTag() {
        colorPicked = ColorPickerHelper.getColorPickedString(colorPickerLayout.getSelPreview());
        tagNameValue = tagName.getValue();
        tagDescValue = tagDesc.getValue();
    }

    protected void displaySuccess(final String tagName) {
        uiNotification.displaySuccess(i18n.getMessage("message.save.success", new Object[] { tagName }));
    }

    protected void displayValidationError(final String errorMessage) {
        uiNotification.displayValidationError(errorMessage);
    }

    protected void setTagColor(final Color selectedColor, final String previewColor) {
        getColorPickerLayout().setSelectedColor(selectedColor);
        getColorPickerLayout().getSelPreview().setColor(getColorPickerLayout().getSelectedColor());
        getColorPickerLayout().getColorSelect().setColor(getColorPickerLayout().getSelectedColor());
        createDynamicStyleForComponents(tagName, tagDesc, previewColor);
        getPreviewButtonColor(previewColor);
    }

    private boolean isDuplicateByName() {
        final Optional<E> existingType = findEntityByName();
        existingType.ifPresent(type -> uiNotification.displayValidationError(
                i18n.getMessage("message.tag.duplicate.check", new Object[] { type.getName() })));

        return existingType.isPresent();
    }

    protected boolean isDuplicate() {
        return isDuplicateByName();
    }

    public String getColorPicked() {
        return colorPicked;
    }

    public void setColorPicked(final String colorPicked) {
        this.colorPicked = colorPicked;
    }

    public String getTagNameValue() {
        return tagNameValue;
    }

    public void setTagNameValue(final String tagNameValue) {
        this.tagNameValue = tagNameValue;
    }

    public String getTagDescValue() {
        return tagDescValue;
    }

    public void setTagDescValue(final String tagDescValue) {
        this.tagDescValue = tagDescValue;
    }

    public FormLayout getFormLayout() {
        return formLayout;
    }

    public GridLayout getMainLayout() {
        return mainLayout;
    }

    @Override
    public void addColorChangeListener(final ColorChangeListener listener) {
    }

    @Override
    public void removeColorChangeListener(final ColorChangeListener listener) {
    }

    protected abstract Optional<E> findEntityByName();

    protected abstract String getWindowCaption();

    protected abstract void updateEntity(E entity);

    protected abstract void createEntity();

}
