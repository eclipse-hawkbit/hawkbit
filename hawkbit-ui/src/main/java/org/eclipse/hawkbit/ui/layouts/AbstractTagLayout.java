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
import org.eclipse.hawkbit.ui.common.builder.LabelBuilder;
import org.eclipse.hawkbit.ui.common.builder.TextAreaBuilder;
import org.eclipse.hawkbit.ui.common.builder.TextFieldBuilder;
import org.eclipse.hawkbit.ui.common.builder.WindowBuilder;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
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
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.components.colorpicker.ColorChangeEvent;
import com.vaadin.ui.components.colorpicker.ColorChangeListener;
import com.vaadin.ui.components.colorpicker.ColorSelector;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Abstract class for tag layout.
 *
 * @param <E>
 *            entity class
 */
public abstract class AbstractTagLayout<E extends NamedEntity> extends CustomComponent
        implements ColorChangeListener, ColorSelector {

    private static final long serialVersionUID = 1L;

    private static final String TAG_NAME_DYNAMIC_STYLE = "new-tag-name";

    private static final String TAG_DESC_DYNAMIC_STYLE = "new-tag-desc";

    private static final String TAG_DYNAMIC_STYLE = "tag-color-preview";

    private static final String MESSAGE_ERROR_MISSING_TAGNAME = "message.error.missing.tagname";

    private static final int MAX_TAGS = 500;

    private final VaadinMessageSource i18n;

    private transient EntityFactory entityFactory;

    private transient EventBus.UIEventBus eventBus;

    private final SpPermissionChecker permChecker;

    private final UINotification uiNotification;

    private final FormLayout formLayout = new FormLayout();

    private CommonDialogWindow window;

    private Label colorLabel;

    private TextField tagName;

    private TextArea tagDesc;

    private Button tagColorPreviewBtn;

    private ColorPickerLayout colorPickerLayout;

    private GridLayout mainLayout;

    private VerticalLayout contentLayout;

    private boolean tagPreviewBtnClicked;

    private String colorPicked;

    private HorizontalLayout colorLabelLayout;

    /**
     * Constructor for AbstractCreateUpdateTagLayout
     * 
     * @param i18n
     *            I18N
     * @param entityFactory
     *            EntityFactory
     * @param eventBus
     *            UIEventBus
     * @param permChecker
     *            SpPermissionChecker
     * @param uiNotification
     *            UINotification
     */
    public AbstractTagLayout(final VaadinMessageSource i18n, final EntityFactory entityFactory,
            final UIEventBus eventBus, final SpPermissionChecker permChecker, final UINotification uiNotification) {
        this.i18n = i18n;
        this.entityFactory = entityFactory;
        this.eventBus = eventBus;
        this.permChecker = permChecker;
        this.uiNotification = uiNotification;
        init();
    }

    /**
     * 
     * Save or update the entity.
     */
    private final class SaveOnDialogCloseListener implements SaveDialogCloseListener {

        @Override
        public void saveOrUpdate() {
            saveEntity();
        }

        @Override
        public boolean canWindowSaveOrUpdate() {
            return isDeleteAction() || (isUpdateAction() || !isDuplicate());
        }

    }

    protected boolean isDeleteAction() {
        return false;
    }

    /**
     * Discard the changes and close the popup.
     */
    protected void discard() {
        UI.getCurrent().removeWindow(window);
    }

    /**
     * Init the layout.
     */
    protected void init() {
        setSizeUndefined();
        createRequiredComponents();
        buildLayout();
        addListeners();
        eventBus.subscribe(this);
        openConfigureWindow();
    }

    protected abstract Optional<E> findEntityByName();

    protected abstract String getWindowCaption();

    protected abstract void saveEntity();

    protected void createRequiredComponents() {
        colorLabel = new LabelBuilder().name(i18n.getMessage("label.choose.tag.color")).buildLabel();
        colorLabel.addStyleName(SPUIStyleDefinitions.COLOR_LABEL_STYLE);

        tagName = new TextFieldBuilder(getTagNameSize()).caption(i18n.getMessage("textfield.name"))
                .styleName(ValoTheme.TEXTFIELD_TINY + " " + SPUIDefinitions.TAG_NAME).required(true, i18n)
                .prompt(i18n.getMessage("textfield.name")).immediate(true).id(getTagNameId()).buildTextComponent();

        tagDesc = new TextAreaBuilder(getTagDescSize()).caption(i18n.getMessage("textfield.description"))
                .styleName(ValoTheme.TEXTFIELD_TINY + " " + SPUIDefinitions.TAG_DESC)
                .prompt(i18n.getMessage("textfield.description")).id(getTagDescId()).buildTextComponent();

        tagDesc.setNullRepresentation("");

        tagColorPreviewBtn = new Button();
        tagColorPreviewBtn.setId(UIComponentIdProvider.TAG_COLOR_PREVIEW_ID);
        getPreviewButtonColor(ColorPickerConstants.DEFAULT_COLOR);
        tagColorPreviewBtn.setStyleName(TAG_DYNAMIC_STYLE);
    }

    protected abstract String getTagDescId();

    protected abstract String getTagNameId();

    protected abstract int getTagDescSize();

    protected abstract int getTagNameSize();

    protected void buildLayout() {
        mainLayout = new GridLayout(3, 2);
        mainLayout.setSpacing(true);
        colorPickerLayout = new ColorPickerLayout();
        ColorPickerHelper.setRgbSliderValues(colorPickerLayout);
        contentLayout = new VerticalLayout();

        colorLabelLayout = new HorizontalLayout();
        colorLabelLayout.setMargin(false);
        colorLabelLayout.addComponents(colorLabel, tagColorPreviewBtn);

        formLayout.addComponent(tagName);
        formLayout.addComponent(tagDesc);
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
        slidersValueChangeListeners();
    }

    protected Color getColorForColorPicker() {
        return ColorPickerConstants.START_COLOR;
    }

    protected void resetFields() {
        tagName.setEnabled(true);
        tagName.clear();
        tagDesc.clear();
        restoreComponentStyles();
        colorPickerLayout.setSelectedColor(colorPickerLayout.getDefaultColor());
        colorPickerLayout.getSelPreview().setColor(colorPickerLayout.getSelectedColor());
        tagPreviewBtnClicked = false;
        disableFields();
    }

    protected void disableFields() {
        // can be overwritten
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

    protected void setColorToComponents(final Color newColor) {
        setColor(newColor);
        colorPickerLayout.getColorSelect().setColor(newColor);
        getPreviewButtonColor(newColor.getCSS());
        createDynamicStyleForComponents(tagName, tagDesc, newColor.getCSS());
    }

    protected void displaySuccess(final String tagName) {
        uiNotification.displaySuccess(i18n.getMessage("message.save.success", tagName));
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

    protected abstract boolean isUpdateAction();

    protected boolean isDuplicate() {
        return isDuplicateByName();
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
        if (colorPickerLayout.getColorSelect() != null) {
            createDynamicStyleForComponents(tagName, tagDesc, colorPickedPreview);
            colorPickerLayout.getColorSelect().setColor(colorPickerLayout.getSelPreview().getColor());
        }
    }

    public ColorPickerLayout getColorPickerLayout() {
        return colorPickerLayout;
    }

    /**
     * Creates the window to create or update a tag or type
     * 
     * @return the created window
     */
    public CommonDialogWindow createWindow() {
        window = new WindowBuilder(SPUIDefinitions.CREATE_UPDATE_WINDOW).caption(getWindowCaption()).content(this)
                .cancelButtonClickListener(event -> discard()).layout(mainLayout).i18n(i18n)
                .saveDialogCloseListener(new SaveOnDialogCloseListener()).buildCommonDialogWindow();
        return window;
    }

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

    protected String getColorPicked() {
        return colorPicked;
    }

    protected void setColorPicked(final String colorPicked) {
        this.colorPicked = colorPicked;
    }

    protected FormLayout getFormLayout() {
        return formLayout;
    }

    protected GridLayout getMainLayout() {
        return mainLayout;
    }

    @Override
    public void addColorChangeListener(final ColorChangeListener listener) {
    }

    @Override
    public void removeColorChangeListener(final ColorChangeListener listener) {
    }

    protected static String getTagNameDynamicStyle() {
        return TAG_NAME_DYNAMIC_STYLE;
    }

    protected static String getTagDescDynamicStyle() {
        return TAG_DESC_DYNAMIC_STYLE;
    }

    protected static String getTagDynamicStyle() {
        return TAG_DYNAMIC_STYLE;
    }

    protected static String getMessageErrorMissingTagname() {
        return MESSAGE_ERROR_MISSING_TAGNAME;
    }

    protected static int getMaxTags() {
        return MAX_TAGS;
    }

    protected VaadinMessageSource getI18n() {
        return i18n;
    }

    protected EntityFactory getEntityFactory() {
        return entityFactory;
    }

    protected EventBus.UIEventBus getEventBus() {
        return eventBus;
    }

    protected SpPermissionChecker getPermChecker() {
        return permChecker;
    }

    protected UINotification getUiNotification() {
        return uiNotification;
    }

    protected Label getColorLabel() {
        return colorLabel;
    }

    protected TextField getTagName() {
        return tagName;
    }

    protected TextArea getTagDesc() {
        return tagDesc;
    }

    protected Button getTagColorPreviewBtn() {
        return tagColorPreviewBtn;
    }

    protected VerticalLayout getContentLayout() {
        return contentLayout;
    }

    protected boolean isTagPreviewBtnClicked() {
        return tagPreviewBtnClicked;
    }

    protected HorizontalLayout getColorLabelLayout() {
        return colorLabelLayout;
    }

    protected void setTagName(final TextField tagName) {
        this.tagName = tagName;
    }

    protected void setTagDesc(final TextArea tagDesc) {
        this.tagDesc = tagDesc;
    }

    protected void setTagColorPreviewBtn(final Button tagColorPreviewBtn) {
        this.tagColorPreviewBtn = tagColorPreviewBtn;
    }

    /**
     * Open color picker on click of preview button. Auto select the color based
     * on target tag if already selected.
     */
    private void previewButtonClicked() {
        if (!tagPreviewBtnClicked) {
            colorPickerLayout
                    .setSelectedColor(ColorPickerHelper.rgbToColorConverter(ColorPickerConstants.DEFAULT_COLOR));
        }

        tagPreviewBtnClicked = !tagPreviewBtnClicked;
        colorPickerLayout.setVisible(tagPreviewBtnClicked);
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

    /**
     * Value change listeners implementations of sliders.
     */
    private void slidersValueChangeListeners() {
        colorPickerLayout.getRedSlider().addValueChangeListener(new ValueChangeListener() {
            private static final long serialVersionUID = 1L;

            @Override
            public void valueChange(final ValueChangeEvent event) {
                final double red = (Double) event.getProperty().getValue();
                final Color newColor = new Color((int) red, colorPickerLayout.getSelectedColor().getGreen(),
                        colorPickerLayout.getSelectedColor().getBlue());
                setColorToComponents(newColor);
            }
        });
        colorPickerLayout.getGreenSlider().addValueChangeListener(new ValueChangeListener() {
            private static final long serialVersionUID = 1L;

            @Override
            public void valueChange(final ValueChangeEvent event) {
                final double green = (Double) event.getProperty().getValue();
                final Color newColor = new Color(colorPickerLayout.getSelectedColor().getRed(), (int) green,
                        colorPickerLayout.getSelectedColor().getBlue());
                setColorToComponents(newColor);
            }
        });
        colorPickerLayout.getBlueSlider().addValueChangeListener(new ValueChangeListener() {
            private static final long serialVersionUID = 1L;

            @Override
            public void valueChange(final ValueChangeEvent event) {
                final double blue = (Double) event.getProperty().getValue();
                final Color newColor = new Color(colorPickerLayout.getSelectedColor().getRed(),
                        colorPickerLayout.getSelectedColor().getGreen(), (int) blue);
                setColorToComponents(newColor);
            }
        });
    }

    private boolean isDuplicateByName() {
        final Optional<E> existingType = findEntityByName();
        existingType.ifPresent(type -> uiNotification
                .displayValidationError(i18n.getMessage("message.tag.duplicate.check", type.getName())));
        return existingType.isPresent();
    }

    private void openConfigureWindow() {
        createWindow();
        UI.getCurrent().addWindow(window);
        window.setModal(true);
        window.setVisible(Boolean.TRUE);
    }

    protected CommonDialogWindow getWindow() {
        return window;
    }

}
